package com.hivid.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.hivid.app.util.AppLogger

class HividApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize Persistent Logging System
        AppLogger.init(this)
        AppLogger.i(
            "Application",
            "Hivid starting on ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT}, Board: ${Build.BOARD})"
        )

        // 2. Global Uncaught Exception Handler
        setupCrashHandler()

        // 3. Notification Channels
        createNotificationChannels()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.e(
                "CrashHandler",
                "Fatal unhandled exception in thread '${thread.name}': ${throwable.message}",
                throwable
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PROCESSING,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_PROCESSING = "hivid_processing_channel"
    }
}
