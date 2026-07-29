package com.carparking.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.carparking.app.bluetooth.BluetoothMonitorService
import com.carparking.app.bluetooth.BluetoothPreferences
import com.carparking.app.bluetooth.CompanionDeviceHelper
import com.carparking.app.crash.CrashHandler
import com.carparking.app.data.database.AppDatabase
import com.carparking.app.notification.AutoParkNotification
import com.carparking.app.notification.NotificationHelper
import com.carparking.app.notification.OngoingParkingNotification
import com.carparking.app.utils.AppLifecycleObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class CarParkingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Capture les plantages dans un journal local consultable
        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(this, Thread.getDefaultUncaughtExceptionHandler())
        )
        // OSMDroid
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = getExternalFilesDir(null)
            osmdroidTileCache = getExternalFilesDir("tiles")
        }
        // Canaux de notifications
        NotificationHelper.createNotificationChannel(this)
        OngoingParkingNotification.createChannel(this)
        // Sans ce canal, la notification d'échec GPS était silencieusement ignorée
        AutoParkNotification.createChannel(this)
        // Proposer de sauvegarder le parking quand l'app passe en arrière-plan
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(this))

        // Réarme l'observation de présence des voitures associées : le système peut
        // l'avoir relâchée (mise à jour de l'appli, redémarrage du téléphone).
        CompanionDeviceHelper.rearmAll(this)

        // Mode fiable optionnel (service en premier plan), si l'utilisateur l'a activé
        if (BluetoothPreferences.isBluetoothAutoParkEnabled(this) &&
            BluetoothPreferences.isReliableModeEnabled(this)
        ) {
            BluetoothMonitorService.start(this)
        }

        migrateLegacyReminder()
    }

    /**
     * Reporte l'ancienne durée de rappel globale sur chaque voiture qui n'en a pas
     * encore, pour ne pas perdre silencieusement le réglage de l'utilisateur.
     * Ne s'exécute qu'une fois : la valeur globale est effacée au passage.
     */
    private fun migrateLegacyReminder() {
        val legacy = BluetoothPreferences.consumeLegacyReminderMinutes(this)
        if (legacy <= 0L) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dao = AppDatabase.getDatabase(this@CarParkingApplication).carDao()
                dao.getAllCarsSync()
                    .filter { it.autoReminderMinutes == 0L }
                    .forEach { dao.updateCar(it.copy(autoReminderMinutes = legacy)) }
            }
        }
    }
}
