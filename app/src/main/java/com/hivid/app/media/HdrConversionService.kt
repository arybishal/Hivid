package com.hivid.app.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hivid.app.HividApplication
import com.hivid.app.R
import com.hivid.app.domain.model.ConversionReport
import com.hivid.app.domain.model.ConversionState
import com.hivid.app.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HdrConversionService : LifecycleService() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Hivid:TranscodeLock").apply {
            setReferenceCounted(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                if (status >= PowerManager.THERMAL_STATUS_SEVERE) {
                    _thermalThrottlingWarning.value = true
                }
            }
            powerManager.addThermalStatusListener(thermalListener!!)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_CANCEL) {
            com.hivid.app.util.AppLogger.i("HdrConversionService", "Cancel intent received from notification action")
            _cancelRequested.value = true
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotif = buildNotification(0, getString(R.string.notif_converting))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, initialNotif, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, initialNotif)
        }

        wakeLock?.acquire(60 * 60 * 1000L) // 1 hour safety timeout

        lifecycleScope.launch {
            _conversionProgress.collect { progress ->
                val notif = buildNotification(progress, getString(R.string.notif_converting))
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(NOTIFICATION_ID, notif)
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(progress: Int, title: String): Notification {
        val cancelIntent = Intent(this, HdrConversionService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, HividApplication.CHANNEL_ID_PROCESSING)
            .setContentTitle(title)
            .setContentText("$progress%")
            .setSmallIcon(R.drawable.ic_hivid_logo)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .setProgress(100, progress, progress == 0)
            .addAction(R.drawable.ic_hivid_logo, getString(R.string.btn_cancel), cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener != null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.removeThermalStatusListener(thermalListener!!)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        const val NOTIFICATION_ID = 2026
        const val ACTION_START = "com.hivid.app.action.START"
        const val ACTION_CANCEL = "com.hivid.app.action.CANCEL"

        private val _conversionProgress = MutableStateFlow(0)
        val conversionProgress = _conversionProgress.asStateFlow()

        private val _cancelRequested = MutableStateFlow(false)
        val cancelRequested = _cancelRequested.asStateFlow()

        private val _thermalThrottlingWarning = MutableStateFlow(false)
        val thermalThrottlingWarning = _thermalThrottlingWarning.asStateFlow()

        fun updateProgress(progress: Int) {
            _conversionProgress.value = progress
        }

        fun resetCancel() {
            _cancelRequested.value = false
        }

        fun start(context: Context) {
            resetCancel()
            val intent = Intent(context, HdrConversionService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, HdrConversionService::class.java)
            context.stopService(intent)
        }
    }
}
