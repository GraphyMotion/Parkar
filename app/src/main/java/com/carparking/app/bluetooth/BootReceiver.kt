package com.carparking.app.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Relance la surveillance Bluetooth après un redémarrage du téléphone,
 * si l'Auto-Park était activé.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            BluetoothPreferences.isBluetoothAutoParkEnabled(context)
        ) {
            BluetoothMonitorService.start(context)
        }
    }
}
