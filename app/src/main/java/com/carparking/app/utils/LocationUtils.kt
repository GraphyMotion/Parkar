package com.carparking.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LatLng(val latitude: Double, val longitude: Double)

object LocationUtils {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LatLng? =
        suspendCancellableCoroutine { cont ->
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(LatLng(location.latitude, location.longitude))
                    } else {
                        // fallback: last known location
                        fusedClient.lastLocation.addOnSuccessListener { last ->
                            cont.resume(if (last != null) LatLng(last.latitude, last.longitude) else null)
                        }.addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener { cont.resume(null) }
            cont.invokeOnCancellation { cts.cancel() }
        }

    fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var result: String? = null
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    result = addresses.firstOrNull()?.let { buildAddress(it) }
                }
                // Synchronous fallback — GeocodeListener is async on API 33+, use deprecated for sync
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.let { buildAddress(it) }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.let { buildAddress(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Géocodage direct : convertit un texte d'adresse en coordonnées GPS.
     * Retourne null si l'adresse est introuvable ou le réseau indisponible.
     */
    fun getCoordinatesFromAddress(context: Context, addressText: String): LatLng? {
        if (addressText.isBlank()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(addressText.trim(), 1)
            results?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildAddress(address: android.location.Address): String? {
        return buildString {
            if (!address.thoroughfare.isNullOrEmpty()) {
                append(address.thoroughfare)
                if (!address.subThoroughfare.isNullOrEmpty()) append(" ${address.subThoroughfare}")
            }
            if (!address.locality.isNullOrEmpty()) {
                if (isNotEmpty()) append(", ")
                append(address.locality)
            } else if (!address.subAdminArea.isNullOrEmpty()) {
                if (isNotEmpty()) append(", ")
                append(address.subAdminArea)
            }
        }.ifEmpty { null }
    }
}
