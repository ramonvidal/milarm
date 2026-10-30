package com.milarm

import android.app.Application
import android.content.Context
import com.milarm.alarm.AlarmScheduler
import com.milarm.alarm.Notifications
import com.milarm.data.AlarmRepository
import com.milarm.update.UpdateInstaller
import com.milarm.update.UpdateRepository

class MilarmApp : Application() {
    lateinit var repository: AlarmRepository
        private set
    lateinit var scheduler: AlarmScheduler
        private set
    lateinit var updates: UpdateRepository
        private set
    lateinit var installer: UpdateInstaller
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository(this)
        scheduler = AlarmScheduler(this)
        updates = UpdateRepository(this)
        installer = UpdateInstaller(this)
        Notifications.createChannels(this)
    }
}

val Context.milarm: MilarmApp get() = applicationContext as MilarmApp
