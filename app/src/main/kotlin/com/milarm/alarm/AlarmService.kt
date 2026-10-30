package com.milarm.alarm

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.milarm.core.Alarm
import com.milarm.core.AlarmFlow
import com.milarm.core.AlarmPhase
import com.milarm.core.SoundMode
import com.milarm.core.WakeCheckEvaluator
import com.milarm.milarm
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Runs one alarm from ringing until the user is up: plays sound and vibration, keeps the band
 * buzzing through forwarded notifications, then watches phone sensors during the wake-up check.
 */
class AlarmService : Service(), SensorEventListener {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var sensorManager: SensorManager

    private var alarm: Alarm? = null
    private var phase: AlarmPhase = AlarmPhase.Done
    private var player: MediaPlayer? = null
    private var evaluator: WakeCheckEvaluator? = null
    private var sensorsRegistered = false
    private var bandNotificationId = 0

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            evaluator?.onUserPresent()
            checkAwake()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "milarm:alarm")
        sensorManager = getSystemService(SensorManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // Every startForegroundService() call must be followed by startForeground().
                val next = milarm.repository.get(intent.getIntExtra(EXTRA_ALARM_ID, -1))
                startInForeground(Notifications.ringing(this, next ?: placeholderAlarm), mediaType())
                if (next == null) {
                    finish()
                } else {
                    stopRinging()
                    stopSensors()
                    alarm = next
                    enterPhase(AlarmFlow.start(intent.getIntExtra(EXTRA_ROUND, 0)))
                }
            }
            ACTION_DISMISS -> transition { AlarmFlow.onDismiss(phase, it.wakeCheck) }
            ACTION_SNOOZE -> {
                val current = phase
                val alarm = alarm
                if (current is AlarmPhase.Ringing && alarm != null) {
                    milarm.scheduler.scheduleSnooze(alarm.id, current.round, alarm.snoozeMinutes)
                }
                transition { AlarmFlow.onSnooze(phase) }
            }
            ACTION_IM_UP -> transition { AlarmFlow.onAwake(phase) }
        }
        if (alarm == null) finish()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        stopSensors()
        if (wakeLock.isHeld) wakeLock.release()
        AlarmSession.update(null)
        super.onDestroy()
    }

    private fun transition(next: (Alarm) -> AlarmPhase) {
        val alarm = alarm ?: return
        val nextPhase = next(alarm)
        if (nextPhase != phase) enterPhase(nextPhase)
    }

    private fun enterPhase(next: AlarmPhase) {
        val alarm = alarm ?: return finish()
        handler.removeCallbacksAndMessages(null)
        phase = next
        AlarmSession.update(if (next == AlarmPhase.Done) null else AlarmSession.State(alarm, next))
        when (next) {
            is AlarmPhase.Ringing -> {
                stopSensors()
                startRinging(alarm)
            }
            is AlarmPhase.WakeCheck -> {
                stopRinging()
                startWakeCheck(alarm)
            }
            is AlarmPhase.Asking -> ask(alarm)
            AlarmPhase.Done -> finish()
        }
    }

    // Ringing

    private fun startRinging(alarm: Alarm) {
        holdWakeLock(RING_TIMEOUT_MS + 60_000)
        startInForeground(Notifications.ringing(this, alarm), mediaType())
        if (alarm.soundMode == SoundMode.RINGTONE_AND_VIBRATE) playSound()
        if (alarm.soundMode != SoundMode.BAND_ONLY) vibrate()

        val bandAlert = object : Runnable {
            override fun run() {
                postBandAlert("⏰ ${Notifications.title(alarm)}", Notifications.timeText(alarm))
                handler.postDelayed(this, BAND_REPEAT_MS)
            }
        }
        bandAlert.run()
        handler.postDelayed({ transition { AlarmFlow.onRingTimeout(phase) } }, RING_TIMEOUT_MS)
    }

    private fun playSound() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { Log.w(TAG, "Could not play alarm sound", it) }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
    }

    private fun stopRinging() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        getSystemService(Vibrator::class.java)?.cancel()
        Notifications.cancelBandAlerts(this)
    }

    // Wake-up check

    private fun startWakeCheck(alarm: Alarm) {
        val minutes = alarm.wakeCheck.checkAfterMinutes
        holdWakeLock(minutes * 60_000L + alarm.wakeCheck.responseSeconds * 1_000L + 60_000)
        startInForeground(Notifications.wakeCheck(this, alarm), wakeCheckType())
        startSensors()
        handler.postDelayed({
            transition { AlarmFlow.onCheckDeadline(phase, evaluator?.isAwake == true) }
        }, minutes * 60_000L)
    }

    private fun ask(alarm: Alarm) {
        startInForeground(Notifications.asking(this, alarm), wakeCheckType())
        postBandAlert("Are you up?", "Tap I'm up on your phone")
        handler.postDelayed({ transition { AlarmFlow.onAskTimeout(phase) } }, alarm.wakeCheck.responseSeconds * 1_000L)
    }

    private fun checkAwake() {
        if (evaluator?.isAwake == true) transition { AlarmFlow.onAwake(phase) }
    }

    private fun startSensors() {
        if (sensorsRegistered) return
        evaluator = WakeCheckEvaluator()
        if (hasActivityPermission()) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        val motion = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        motion?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        ContextCompat.registerReceiver(
            this, userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sensorsRegistered = true
    }

    private fun stopSensors() {
        if (!sensorsRegistered) return
        sensorManager.unregisterListener(this)
        unregisterReceiver(userPresentReceiver)
        sensorsRegistered = false
        evaluator = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val evaluator = evaluator ?: return
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> evaluator.onStepCounter(event.values[0].toLong())
            Sensor.TYPE_LINEAR_ACCELERATION -> evaluator.onMotion(magnitude(event.values), event.timestamp / 1_000_000)
            Sensor.TYPE_ACCELEROMETER ->
                evaluator.onMotion(abs(magnitude(event.values) - SensorManager.GRAVITY_EARTH), event.timestamp / 1_000_000)
        }
        checkAwake()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private fun magnitude(v: FloatArray) = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])

    // Helpers

    private fun postBandAlert(title: String, text: String) {
        bandNotificationId = Notifications.postBandAlert(this, title, text, bandNotificationId)
    }

    private fun startInForeground(notification: Notification, type: Int) {
        ServiceCompat.startForeground(this, Notifications.FOREGROUND_ID, notification, type)
    }

    private fun mediaType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0

    /** The health type needs the activity permission on Android 14+; fall back if it was denied. */
    private fun wakeCheckType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasActivityPermission()) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            mediaType()
        }

    private fun hasActivityPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun holdWakeLock(timeoutMillis: Long) {
        if (wakeLock.isHeld) wakeLock.release()
        wakeLock.acquire(timeoutMillis)
    }

    private fun finish() {
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        stopSensors()
        alarm = null
        phase = AlarmPhase.Done
        AlarmSession.update(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (wakeLock.isHeld) wakeLock.release()
        stopSelf()
    }

    companion object {
        private const val TAG = "AlarmService"
        const val ACTION_START = "com.milarm.action.START"
        const val ACTION_DISMISS = "com.milarm.action.DISMISS"
        const val ACTION_SNOOZE = "com.milarm.action.SNOOZE"
        const val ACTION_IM_UP = "com.milarm.action.IM_UP"
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_ROUND = "round"

        private const val BAND_REPEAT_MS = 30_000L
        private const val RING_TIMEOUT_MS = 10 * 60_000L

        private val placeholderAlarm = Alarm(id = -1, hour = 0, minute = 0)

        fun start(context: Context, alarmId: Int, round: Int) {
            val intent = Intent(context, AlarmService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_ROUND, round)
            ContextCompat.startForegroundService(context, intent)
        }

        fun send(context: Context, action: String) {
            context.startService(Intent(context, AlarmService::class.java).setAction(action))
        }
    }
}
