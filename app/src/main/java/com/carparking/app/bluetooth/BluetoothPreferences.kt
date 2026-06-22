package com.carparking.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

/**
 * Gestion des préférences Bluetooth pour l'auto-parking.
 */
object BluetoothPreferences {

    private const val PREFS_NAME = "parkar_bluetooth"
    private const val KEY_BT_ADDRESS = "car_bt_address"
    private const val KEY_BT_NAME = "car_bt_name"
    private const val KEY_BT_ENABLED = "car_bt_enabled"
    private const val KEY_LAST_EVENT = "last_bt_event"

    /** Journal de diagnostic : dernier événement Bluetooth reçu par le receiver */
    fun recordEvent(context: Context, event: String) {
        val ts = java.text.SimpleDateFormat("dd/MM HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_EVENT, "$event ($ts)").apply()
    }

    fun getLastEvent(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_EVENT, null)
    }

    fun isBluetoothAutoParkEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BT_ENABLED, false)
    }

    fun setBluetoothAutoParkEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BT_ENABLED, enabled).apply()
    }

    fun getSavedDeviceAddress(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BT_ADDRESS, null)
    }

    fun getSavedDeviceName(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BT_NAME, null)
    }

    fun saveDevice(context: Context, address: String, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BT_ADDRESS, address)
            .putString(KEY_BT_NAME, name)
            .putBoolean(KEY_BT_ENABLED, true)
            .apply()
    }

    fun clearDevice(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_BT_ADDRESS)
            .remove(KEY_BT_NAME)
            .putBoolean(KEY_BT_ENABLED, false)
            .apply()
    }

    /**
     * Vérifie si l'adresse est actuellement connecté (lié au profil)
     */
    fun isDeviceConnected(context: Context, address: String): Boolean {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return false
        val adapter = btManager.adapter ?: return false
        return try {
            val device = adapter.getRemoteDevice(address)
            // On ne peut pas vérifier la connexion ACL sans receiver dédié
            // Mais on peut vérifier si le device est bonded
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (e: Exception) {
            false
        }
    }
}
