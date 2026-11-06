package com.zensa.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.zensa.app.di.ServiceLocator
import com.zensa.app.sync.SyncScheduler

/**
 * Application entry point.
 * Initializes DI, schedules offline WorkManager sync, and notification channel for FCM.
 */
class ZensaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        SyncScheduler.schedulePeriodic(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "zensa_default",
                "Zensa",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Study reminders and sync alerts" }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }
}
