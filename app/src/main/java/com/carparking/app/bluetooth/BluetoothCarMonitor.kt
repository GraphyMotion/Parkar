package com.carparking.app.bluetooth

import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.carparking.app.MainActivity
import com.carparking.app.data.database.AppDatabase
import com.carparking.app.data.model.ParkingRecord
import com.carparking.app.notification.AutoParkNotification
import com.carparking.app.notification.OngoingParkingNotification
import com.carparking.app.utils.LocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BluetoothCarMonitor : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothCarMonitor"
        private const val NOTIF_CHANNEL_ID = "bt_autopark_channel"
        private const val NOTIF_ID_CONNECTED = 60_000
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let { onCarDisconnected(context, it) }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let { onCarConnected(context, it) }
            }
        }
    }

    private fun onCarDisconnected(context: Context, device: BluetoothDevice) {
        val prefs = context.getSharedPreferences("parkar_bluetooth", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("car_bt_enabled", false)) return
        val savedAddress = prefs.getString("car_bt_address", null) ?: return
        BluetoothPreferences.recordEvent(context, "Déconnexion ${device.address}" +
            if (device.address == savedAddress) " ✓ voiture" else " (autre appareil)")
        if (device.address != savedAddress) return

        Log.d(TAG, "Voiture deconnectee - sauvegarde auto du parking")

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val cars = db.carDao().getAllCarsSync()
                if (cars.isEmpty()) { pending.finish(); return@launch }

                val location = LocationUtils.getCurrentLocation(context)
                if (location == null) {
                    Log.w(TAG, "Impossible d'obtenir la position GPS")
                    BluetoothPreferences.recordEvent(context,
                        "Échec GPS — vérifiez la localisation \"Tout le temps\"")
                    AutoParkNotification.show(context)
                    pending.finish()
                    return@launch
                }

                val address = LocationUtils.getAddressFromCoordinates(
                    context, location.latitude, location.longitude
                )

                val car = cars.first()
                db.parkingRecordDao().deactivateParkingForCar(car.id)
                db.parkingRecordDao().insertParking(
                    ParkingRecord(
                        carId = car.id,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address,
                        note = "Sauvegarde automatique (Bluetooth)"
                    )
                )

                OngoingParkingNotification.show(
                    context = context,
                    carId = car.id,
                    carName = car.name,
                    address = address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    parkedSince = System.currentTimeMillis()
                )

                BluetoothPreferences.recordEvent(context,
                    "Parking sauvegardé : ${address ?: "GPS seul"}")
                Log.d(TAG, "Parking auto-sauvegarde: ${address ?: "GPS seulement"}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la sauvegarde auto", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun onCarConnected(context: Context, device: BluetoothDevice) {
        val prefs = context.getSharedPreferences("parkar_bluetooth", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("car_bt_enabled", false)) return
        val savedAddress = prefs.getString("car_bt_address", null) ?: return
        BluetoothPreferences.recordEvent(context, "Connexion ${device.address}" +
            if (device.address == savedAddress) " ✓ voiture" else " (autre appareil)")
        if (device.address != savedAddress) return

        Log.d(TAG, "Voiture reconnectee - archivage du parking et lancement appli")

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val cars = db.carDao().getAllCarsSync()
                val car = cars.firstOrNull() ?: return@launch
                db.parkingRecordDao().deactivateParkingForCar(car.id)
                OngoingParkingNotification.dismiss(context, car.id)
                Log.d(TAG, "Parking archive pour ${car.name}")
                launchApp(context, car.name)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'archivage", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun launchApp(context: Context, carName: String) {
        // Lance directement MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)

        // Notification de confirmation visible dans le shade (au cas où le lancement est bloqué par Android)
        ensureChannel(context)
        val openPending = PendingIntent.getActivity(
            context, NOTIF_ID_CONNECTED, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🚗 Bon trajet !")
            .setContentText("Parking de $carName archivé — l'appli est ouverte.")
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_CONNECTED, notif)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(NOTIF_CHANNEL_ID) != null) return
        val ch = android.app.NotificationChannel(
            NOTIF_CHANNEL_ID,
            "Bluetooth Auto-Park",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notifications de démarrage/arrêt automatique du parking" }
        nm.createNotificationChannel(ch)
    }
}