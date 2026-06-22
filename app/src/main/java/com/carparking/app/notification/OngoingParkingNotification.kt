package com.carparking.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.carparking.app.MainActivity

/**
 * Notification persistante affichée pendant tout le stationnement.
 * Contient deux actions rapides : Naviguer et J'ai retrouvé.
 */
object OngoingParkingNotification {

    const val CHANNEL_ID   = "ongoing_parking_channel"
    const val ACTION_ARRIVED = "com.carparking.app.ACTION_ARRIVED"
    const val EXTRA_CAR_ID   = "extra_car_id"
    const val EXTRA_CAR_NAME = "extra_car_name"

    // Décalage pour éviter collision avec les ID de rappel
    private const val NOTIF_ID_OFFSET = 50_000

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stationnement en cours",
            NotificationManager.IMPORTANCE_LOW          // discrète, sans son
        ).apply {
            description = "Indique qu'une voiture est actuellement garée"
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        carId: Int,
        carName: String,
        address: String?,
        latitude: Double,
        longitude: Double,
        parkedSince: Long
    ) {
        // Ouvre l'app au tap
        val openIntent = PendingIntent.getActivity(
            context, carId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1 : Naviguer
        val mapsUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Ma+voiture)")
        val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        val navPending = PendingIntent.getActivity(
            context, carId + 1000,
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=$latitude,$longitude")),
                "Naviguer"
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Essaie Maps en priorité, sinon browser
        val navFinal = if (mapsIntent.resolveActivity(context.packageManager) != null) {
            PendingIntent.getActivity(
                context, carId + 1000, mapsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else navPending

        // Action 2 : J'ai retrouvé
        val arrivedIntent = Intent(ACTION_ARRIVED).apply {
            setClass(context, ParkingActionReceiver::class.java)
            putExtra(EXTRA_CAR_ID, carId)
            putExtra(EXTRA_CAR_NAME, carName)
        }
        val arrivedPending = PendingIntent.getBroadcast(
            context, carId + 2000, arrivedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsed  = System.currentTimeMillis() - parkedSince
        val minutes  = elapsed / 60_000
        val hours    = minutes / 60
        val timeText = when {
            hours > 0   -> "depuis ${hours}h${if (minutes % 60 > 0) "${minutes % 60}min" else ""}"
            minutes > 0 -> "depuis ${minutes}min"
            else        -> "à l'instant"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🚗 $carName garée $timeText")
            .setContentText(if (!address.isNullOrEmpty()) "📍 $address" else "Position enregistrée")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(buildString {
                        if (!address.isNullOrEmpty()) appendLine("📍 $address")
                        append("Appuyez pour voir sur la carte")
                    })
            )
            .setContentIntent(openIntent)
            .setOngoing(true)           // persistante
            .setAutoCancel(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_directions, "Naviguer", navFinal)
            .addAction(android.R.drawable.checkbox_on_background, "J'ai retrouvé", arrivedPending)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_OFFSET + carId, notification)
    }

    fun dismiss(context: Context, carId: Int) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(NOTIF_ID_OFFSET + carId)
    }
}
