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
 * Service en premier plan qui écoute les connexions/déconnexions Bluetooth de la voiture.
 *
 * Indispensable : sur les téléphones récents (Xiaomi, Samsung, etc.), les receivers
 * déclarés dans le manifest ne sont pas réveillés de manière fiable quand l'appli
 * est en arrière-plan. Un receiver enregistré dynamiquement dans un service en
 * premier plan reçoit toujours les événements.
 */
class BluetoothMonitorService : Service() {

    private var receiver: BluetoothCarMonitor? = null

    companion object {
        private const val TAG = "BluetoothMonitorService"
        private const val CHANNEL_ID = "bt_monitor_channel"
        private const val NOTIF_ID = 70_000

        fun start(context: Context) {
            val intent = Intent(context, BluetoothMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
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
        registerReceiver(receiver, filter)
        BluetoothPreferences.recordEvent(this, "Surveillance Bluetooth démarrée")
        Log.d(TAG, "Service de surveillance Bluetooth démarré")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY : Android relance le service s'il est tué
        return START_STICKY
    }

    override fun onDestroy() {
        receiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        receiver = null
        Log.d(TAG, "Service de surveillance Bluetooth arrêté")
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
            .setContentTitle("Auto-Park actif")
            .setContentText("En attente de la connexion Bluetooth de votre voiture")
            .setContentIntent(openPending)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Surveillance Bluetooth",
            NotificationManager.IMPORTANCE_MIN   // discrète, repliée en bas du volet
        ).apply {
            description = "Indique que la détection automatique du parking est active"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
