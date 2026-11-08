package com.oki.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Mantiene a Oki en primer plano con una notificación fija. Así Android lo trata como una app
 * importante y no lo duerme ni lo cierra. Además vigila que el lector de pagos siga conectado
 * y le pide a Android reconectarlo si se cayó.
 */
class KeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private val watchdog = object : Runnable {
        override fun run() {
            if (!PaymentListenerService.connected) {
                try {
                    NotificationListenerService.requestRebind(
                        ComponentName(this@KeepAliveService, PaymentListenerService::class.java)
                    )
                } catch (e: Exception) {
                    // Sin acceso a notificaciones no hay nada que reconectar.
                }
            }
            handler.postDelayed(this, WATCHDOG_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        handler.postDelayed(watchdog, WATCHDOG_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.keepalive_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_oki)
            .setContentTitle(getString(R.string.keepalive_title))
            .setContentText(getString(R.string.keepalive_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "oki_activo"
        private const val NOTIFICATION_ID = 1
        private const val WATCHDOG_MS = 60_000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
            } catch (e: Exception) {
                // Android puede negarlo si la app está en segundo plano; se reintenta al abrir Oki.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}
