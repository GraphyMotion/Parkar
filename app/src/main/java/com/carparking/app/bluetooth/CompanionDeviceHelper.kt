package com.carparking.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.util.Log

/**
 * Encapsule le Companion Device Manager (CDM).
 *
 * Le CDM permet de déclarer la voiture comme « appareil compagnon ». Le système se
 * charge alors de réveiller [CarCompanionService] à chaque connexion/déconnexion,
 * même appli fermée, et sans notification permanente.
 *
 * Disponible à partir d'Android 12 (API 31) pour l'observation de présence.
 */
object CompanionDeviceHelper {

    private const val TAG = "CompanionDeviceHelper"

    /** L'observation de présence CDM demande Android 12+. */
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private fun manager(context: Context): CompanionDeviceManager? =
        context.getSystemService(CompanionDeviceManager::class.java)

    /**
     * Ouvre le sélecteur système d'appareils Bluetooth appairés.
     * [onIntentSender] reçoit l'IntentSender à lancer via un ActivityResultLauncher.
     */
    fun requestAssociation(
        context: Context,
        onIntentSender: (IntentSender) -> Unit,
        onError: (String) -> Unit
    ) {
        val cdm = manager(context)
        if (cdm == null) {
            onError("Companion Device Manager indisponible sur cet appareil")
            return
        }
        val request = AssociationRequest.Builder()
            .addDeviceFilter(BluetoothDeviceFilter.Builder().build())
            .setSingleDevice(false)
            .build()

        // associate() peut lever une SecurityException (permission BLUETOOTH_SCAN
        // manquante, le sélecteur système scanne les appareils) ou une
        // IllegalStateException. Sans ce try/catch, l'appli se fermait instantanément.
        try {
            cdm.associate(request, object : CompanionDeviceManager.Callback() {
                // Déprécié sur Android 13+, mais l'implémentation par défaut de
                // onAssociationPending() l'appelle : une seule surcharge suffit.
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onDeviceFound(intentSender: IntentSender) {
                    onIntentSender(intentSender)
                }

                override fun onFailure(error: CharSequence?) {
                    onError(error?.toString() ?: "Association annulée")
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de l'association compagnon", e)
            onError("Impossible d'ouvrir le sélecteur : ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Extrait l'appareil choisi par l'utilisateur depuis le résultat du sélecteur.
     * @return (adresse MAC, nom) ou null.
     */
    fun parseAssociationResult(data: Intent?): Pair<String, String>? = try {
        parseAssociationResultInternal(data)
    } catch (e: Exception) {
        // Inclut la SecurityException si BLUETOOTH_CONNECT vient d'être révoquée.
        Log.w(TAG, "Résultat d'association illisible : ${e.message}")
        null
    }

    @SuppressLint("MissingPermission")
    private fun parseAssociationResultInternal(data: Intent?): Pair<String, String>? {
        if (data == null) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val info = data.getParcelableExtra(
                CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java
            )
            val address = info?.deviceMacAddress?.toString()
            if (address != null) {
                val name = info.displayName?.toString()?.takeIf { it.isNotBlank() } ?: address
                return address to name
            }
        }

        @Suppress("DEPRECATION")
        val device: BluetoothDevice? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
            }
        return device?.let { it.address to (it.name ?: it.address) }
    }

    /**
     * Demande au système de surveiller la présence de cet appareil et de réveiller
     * [CarCompanionService] à chaque apparition/disparition.
     */
    fun startObserving(context: Context, address: String) {
        if (!isSupported) return
        val cdm = manager(context) ?: return
        try {
            @Suppress("DEPRECATION")
            cdm.startObservingDevicePresence(address)
            Log.d(TAG, "Observation de présence activée pour $address")
        } catch (e: Exception) {
            // Typiquement si l'appareil n'est pas (ou plus) associé au niveau système.
            Log.w(TAG, "Impossible d'observer $address : ${e.message}")
        }
    }

    fun stopObserving(context: Context, address: String) {
        if (!isSupported) return
        val cdm = manager(context) ?: return
        try {
            @Suppress("DEPRECATION")
            cdm.stopObservingDevicePresence(address)
        } catch (e: Exception) {
            Log.w(TAG, "Arrêt d'observation impossible pour $address : ${e.message}")
        }
    }

    /** Supprime l'association système (appelé quand on délie une voiture). */
    fun disassociate(context: Context, address: String) {
        if (!isSupported) return
        val cdm = manager(context) ?: return
        try {
            stopObserving(context, address)
            @Suppress("DEPRECATION")
            cdm.disassociate(address)
        } catch (e: Exception) {
            Log.w(TAG, "Dissociation impossible pour $address : ${e.message}")
        }
    }

    /** Adresses actuellement associées au niveau système. */
    fun associatedAddresses(context: Context): List<String> {
        if (!isSupported) return emptyList()
        val cdm = manager(context) ?: return emptyList()
        return try {
            @Suppress("DEPRECATION")
            cdm.associations
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Réarme l'observation pour toutes les voitures liées. Appelé au démarrage de
     * l'appli : le système peut avoir relâché l'observation (mise à jour, redémarrage).
     */
    fun rearmAll(context: Context) {
        if (!isSupported) return
        if (!BluetoothPreferences.isBluetoothAutoParkEnabled(context)) return
        val associated = associatedAddresses(context)
        BluetoothPreferences.getLinks(context).forEach { link ->
            if (associated.any { it.equals(link.deviceAddress, ignoreCase = true) }) {
                startObserving(context, link.deviceAddress)
            }
        }
    }

    /** true si la voiture est bien associée au niveau système (et non juste en base). */
    fun isAssociated(context: Context, address: String): Boolean =
        associatedAddresses(context).any { it.equals(address, ignoreCase = true) }
}
