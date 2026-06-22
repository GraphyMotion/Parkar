package com.carparking.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.carparking.app.bluetooth.BluetoothMonitorService
import com.carparking.app.bluetooth.BluetoothPreferences
import com.carparking.app.crash.CrashHandler
import com.carparking.app.notification.NotificationHelper
import com.carparking.app.notification.OngoingParkingNotification
import com.carparking.app.utils.AppLifecycleObserver
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
        // Proposer de sauvegarder le parking quand l'app passe en arrière-plan
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(this))
        // Relance la surveillance Bluetooth si l'Auto-Park est activé
        // (couvre le cas où le système a tué le service)
        if (BluetoothPreferences.isBluetoothAutoParkEnabled(this)) {
            BluetoothMonitorService.start(this)
        }
    }
}
