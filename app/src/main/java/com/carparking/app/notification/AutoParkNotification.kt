package com.carparking.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.carparking.app.MainActivity

/**
 * Notification proposant de sauvegarder le parking automatiquement
 * quand l'app passe en arrière-plan sans parking actif.
 */
object AutoParkNotification {

    const val CHANNEL_ID = "auto_park_channel"
    private const val NOTIF_ID = 90_001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Parking automatique",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Propose de sauvegarder votre position quand vous quittez l'app"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(context: Context) {
        // Open the app on HomeScreen (user picks their car)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_park_prompt", true)
        }
        val openPending = PendingIntent.getActivity(
            context, NOTIF_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick "Save now" action — opens SaveParking for first car
        // (We pass auto_park_prompt and let MainActivity/HomeScreen handle it)
        val saveIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_park_prompt", true)
            putExtra("auto_park_save", true)
        }
        val savePending = PendingIntent.getActivity(
            context, NOTIF_ID + 1, saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🅿️ Enregistrer votre parking ?")
            .setContentText("Il semble que vous vous éloignez. Sauvegarder votre position ?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Aucune position de parking enregistrée. Appuyez pour sauvegarder votre emplacement actuel.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_save, "Enregistrer", savePending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, notification)
    }

    fun dismiss(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(NOTIF_ID)
    }
}
