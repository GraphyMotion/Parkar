package com.carparking.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver Bluetooth classique.
 *
 * Il complète [CarCompanionService] : il couvre Android 11 (où l'observation de
 * présence du Companion Device Manager n'existe pas) et les cas où l'appli tourne
 * déjà. Le garde-fou anti-doublon d'[AutoParkHandler] empêche une double
 * sauvegarde quand les deux sources reçoivent le même événement.
 */
class BluetoothCarMonitor : BroadcastReceiver() {

    private companion object {
        const val SOURCE = "receiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != BluetoothDevice.ACTION_ACL_CONNECTED &&
            action != BluetoothDevice.ACTION_ACL_DISCONNECTED
        ) return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        val address = device.address
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    AutoParkHandler.handleDisconnected(appContext, address, SOURCE)
                } else {
                    AutoParkHandler.handleConnected(appContext, address, SOURCE)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
