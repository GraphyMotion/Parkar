package com.carparking.app.bluetooth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Après un redémarrage du téléphone : réarme l'observation de présence CDM et
 * relance le mode fiable s'il est activé.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!BluetoothPreferences.isBluetoothAutoParkEnabled(context)) return

        CompanionDeviceHelper.rearmAll(context)
        if (BluetoothPreferences.isReliableModeEnabled(context)) {
            BluetoothMonitorService.start(context)
        }
    }
}
