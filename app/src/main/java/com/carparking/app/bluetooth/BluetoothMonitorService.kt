package com.carparking.app.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.carparking.app.MainActivity

/**
 * Service en premier plan — **mode fiable optionnel**, désactivé par défaut.
 *
 * La détection normale passe par [CarCompanionService] (Companion Device Manager),
 * qui ne demande aucune notification permanente. Ce service n'est là que comme
 * filet de sécurité pour les téléphones où le CDM ne réveille pas l'appli de
 * manière fiable : il maintient le processus vivant, au prix d'une notification
 * permanente discrète qu'Android impose à tout service en premier plan.
 */
class BluetoothMonitorService : Service() {

    private var receiver: BluetoothCarMonitor? = null

    companion object {
        private const val TAG = "BluetoothMonitorService"
        private const val CHANNEL_ID = "bt_monitor_channel"
        private const val NOTIF_ID = 70_000

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, BluetoothMonitorService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BluetoothMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        receiver = BluetoothCarMonitor()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this, receiver, filter, ContextCompat.RECEIVER_EXPORTED
        )
        Log.d(TAG, "Mode fiable démarré")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
        Log.d(TAG, "Mode fiable arrêté")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): android.app.Notification {
        val openPending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Auto-Park — mode fiable")
            .setContentText("Détection Bluetooth renforcée active")
            .setContentIntent(openPending)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Surveillance Bluetooth",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Mode fiable : détection renforcée du parking automatique"
                setShowBadge(false)
            }
        )
    }
}
