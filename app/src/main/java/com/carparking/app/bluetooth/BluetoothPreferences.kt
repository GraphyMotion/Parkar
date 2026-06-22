package com.carparking.app.bluetooth

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestion des préférences Bluetooth pour l'auto-parking.
 * Chaque voiture peut être liée à un appareil Bluetooth différent.
 */
object BluetoothPreferences {

    data class CarLink(val carId: Int, val deviceAddress: String, val deviceName: String)

    private const val PREFS_NAME = "parkar_bluetooth"
    private const val KEY_BT_ENABLED = "car_bt_enabled"
    private const val KEY_LINKS = "bt_car_links"
    private const val KEY_LAST_EVENT = "last_bt_event"
    private const val KEY_LAST_DISCONNECT_TS = "last_disconnect_ts"

    /**
     * Marque l'instant d'une déconnexion Bluetooth d'une voiture liée, pour que
     * [AppLifecycleObserver] évite de proposer une sauvegarde manuelle en double
     * pendant que la sauvegarde automatique est en cours (GPS + géocodage).
     */
    fun recordDisconnectAttempt(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_DISCONNECT_TS, System.currentTimeMillis()).apply()
    }

    fun hasRecentDisconnectAttempt(context: Context, windowMs: Long): Boolean {
        val ts = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_DISCONNECT_TS, 0)
        return System.currentTimeMillis() - ts < windowMs
    }

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

    fun getLinks(context: Context): List<CarLink> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LINKS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CarLink(o.getInt("carId"), o.getString("address"), o.getString("name"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLinkForCar(context: Context, carId: Int): CarLink? =
        getLinks(context).firstOrNull { it.carId == carId }

    fun getLinkForDevice(context: Context, address: String): CarLink? =
        getLinks(context).firstOrNull { it.deviceAddress == address }

    /** Associe un appareil Bluetooth à une voiture (remplace tout lien existant pour cette voiture ou cet appareil). */
    fun saveLink(context: Context, carId: Int, address: String, name: String) {
        val updated = getLinks(context)
            .filterNot { it.carId == carId || it.deviceAddress == address } +
            CarLink(carId, address, name)
        persist(context, updated)
        setBluetoothAutoParkEnabled(context, true)
    }

    fun removeLink(context: Context, carId: Int) {
        persist(context, getLinks(context).filterNot { it.carId == carId })
    }

    private fun persist(context: Context, links: List<CarLink>) {
        val arr = JSONArray()
        links.forEach { link ->
            arr.put(JSONObject().apply {
                put("carId", link.carId)
                put("address", link.deviceAddress)
                put("name", link.deviceName)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LINKS, arr.toString()).apply()
    }
}
