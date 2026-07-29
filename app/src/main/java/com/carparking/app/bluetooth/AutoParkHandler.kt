package com.carparking.app.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.carparking.app.MainActivity
import com.carparking.app.data.database.AppDatabase
import com.carparking.app.data.model.ParkingRecord
import com.carparking.app.notification.AutoParkNotification
import com.carparking.app.notification.OngoingParkingNotification
import com.carparking.app.notification.ReminderScheduler
import com.carparking.app.utils.LocationUtils

/**
 * Logique métier de l'auto-parking, indépendante de la source de l'événement.
 *
 * Deux sources l'appellent :
 *  - [CarCompanionService] : réveillé par le système via le Companion Device Manager,
 *    y compris quand l'appli est fermée (source principale sur Android 12+).
 *  - [BluetoothCarMonitor] : receiver classique, qui couvre le cas où l'appli tourne
 *    déjà et les appareils Android 11.
 *
 * Un garde-fou anti-doublon ([BluetoothPreferences.hasRecentDisconnectAttempt]) évite
 * une double sauvegarde si les deux sources reçoivent le même événement.
 */
object AutoParkHandler {

    private const val TAG = "AutoParkHandler"
    private const val NOTIF_CHANNEL_ID = "bt_autopark_channel"
    private const val NOTIF_ID_CONNECTED = 60_000

    /** Fenêtre pendant laquelle un second événement identique est ignoré. */
    private const val DEDUP_WINDOW_MS = 15_000L

    /**
     * La voiture s'arrête (déconnexion Bluetooth) : on capture et sauvegarde la position.
     */
    suspend fun handleDisconnected(context: Context, deviceAddress: String, source: String) {
        if (!BluetoothPreferences.isBluetoothAutoParkEnabled(context)) return
        val link = BluetoothPreferences.getLinkForDevice(context, deviceAddress)
        if (link == null) {
            BluetoothPreferences.recordEvent(context, "[$source] Déconnexion $deviceAddress (non lié)")
            return
        }

        // Anti-doublon : si l'autre source vient de traiter la même déconnexion, on sort.
        if (BluetoothPreferences.hasRecentDisconnectAttempt(context, DEDUP_WINDOW_MS)) {
            Log.d(TAG, "Déconnexion déjà traitée par une autre source — ignorée")
            return
        }
        BluetoothPreferences.recordDisconnectAttempt(context)
        BluetoothPreferences.recordEvent(context, "[$source] Déconnexion ${link.deviceName} — sauvegarde…")

        try {
            val db = AppDatabase.getDatabase(context)
            val car = db.carDao().getCarById(link.carId) ?: return

            // Position RAPIDE : dernière position connue (instantanée, la voiture roulait
            // à l'instant). On ne retombe sur un fix frais qu'en dernier recours.
            val location = LocationUtils.getLastKnownLocation(context)
                ?: LocationUtils.getCurrentLocation(context)
            if (location == null) {
                Log.w(TAG, "Impossible d'obtenir la position GPS")
                BluetoothPreferences.recordEvent(context,
                    "Échec GPS — vérifiez la localisation \"Tout le temps\"")
                AutoParkNotification.show(context)
                return
            }

            val reminderMins = BluetoothPreferences.getDefaultReminderMinutes(context)
            val workerId = if (reminderMins > 0) {
                ReminderScheduler.schedule(context, car.name, reminderMins, car.id)
            } else null

            // On ENREGISTRE IMMÉDIATEMENT les coordonnées, sans attendre le géocodage
            // (réseau, plus lent). Ainsi rien n'est perdu même si l'adresse tarde.
            val now = System.currentTimeMillis()
            val record = ParkingRecord(
                carId = car.id,
                latitude = location.latitude,
                longitude = location.longitude,
                address = null,
                note = "Sauvegarde automatique (Bluetooth)",
                reminderAt = if (reminderMins > 0) now + reminderMins * 60_000L else null,
                reminderWorkerId = workerId
            )
            db.parkingRecordDao().deactivateParkingForCar(car.id)
            val newId = db.parkingRecordDao().insertParking(record)

            OngoingParkingNotification.show(
                context = context,
                carId = car.id,
                carName = car.name,
                address = null,
                latitude = location.latitude,
                longitude = location.longitude,
                parkedSince = now
            )

            BluetoothPreferences.recordEvent(context, "[$source] Parking sauvegardé ✓")
            Log.d(TAG, "Parking auto-sauvegarde (coordonnées enregistrées)")

            // Géocodage best-effort : on complète l'adresse une fois la position
            // déjà en sécurité dans la base.
            val address = LocationUtils.getAddressFromCoordinates(
                context, location.latitude, location.longitude
            )
            if (address != null) {
                db.parkingRecordDao().updateParking(record.copy(id = newId.toInt(), address = address))
                OngoingParkingNotification.show(
                    context = context,
                    carId = car.id,
                    carName = car.name,
                    address = address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    parkedSince = now
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde auto", e)
            BluetoothPreferences.recordEvent(context, "Erreur sauvegarde : ${e.message}")
        }
    }

    /**
     * La voiture redémarre (reconnexion Bluetooth) : on archive le parking en cours.
     */
    suspend fun handleConnected(context: Context, deviceAddress: String, source: String) {
        if (!BluetoothPreferences.isBluetoothAutoParkEnabled(context)) return
        val link = BluetoothPreferences.getLinkForDevice(context, deviceAddress) ?: return

        if (BluetoothPreferences.hasRecentConnectAttempt(context, DEDUP_WINDOW_MS)) {
            Log.d(TAG, "Connexion déjà traitée par une autre source — ignorée")
            return
        }
        BluetoothPreferences.recordConnectAttempt(context)
        BluetoothPreferences.recordEvent(context, "[$source] Connexion ${link.deviceName} — archivage")

        try {
            val db = AppDatabase.getDatabase(context)
            val car = db.carDao().getCarById(link.carId) ?: return
            db.parkingRecordDao().deactivateParkingForCar(car.id)
            OngoingParkingNotification.dismiss(context, car.id)
            Log.d(TAG, "Parking archive pour ${car.name}")
            notifyArrival(context, car.name)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'archivage", e)
        }
    }

    /**
     * Notification de confirmation au démarrage de la voiture.
     *
     * On ne lance plus l'activité directement : Android bloque le démarrage d'activité
     * depuis l'arrière-plan, et cela faisait surgir l'appli par-dessus le GPS/la musique.
     * Une notification cliquable est plus fiable et moins intrusive.
     */
    private fun notifyArrival(context: Context, carName: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, NOTIF_ID_CONNECTED, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🚗 Bon trajet !")
            .setContentText("Parking de $carName archivé.")
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_CONNECTED, notif)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(NOTIF_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Bluetooth Auto-Park",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications de démarrage/arrêt automatique du parking" }
        )
    }
}
