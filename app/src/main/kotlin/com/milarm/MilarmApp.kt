package com.milarm

import android.app.Application
import android.content.Context
import com.milarm.alarm.AlarmScheduler
import com.milarm.alarm.Notifications
import com.milarm.data.AlarmRepository

class MilarmApp : Application() {
    lateinit var repository: AlarmRepository
        private set
    lateinit var scheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository(this)
        scheduler = AlarmScheduler(this)
        Notifications.createChannels(this)
    }
}

val Context.milarm: MilarmApp get() = applicationContext as MilarmApp
