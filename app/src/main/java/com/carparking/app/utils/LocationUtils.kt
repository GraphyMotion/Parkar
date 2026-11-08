package com.carparking.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LatLng(val latitude: Double, val longitude: Double)

object LocationUtils {

    /**
     * Dernière position connue du téléphone — **instantanée** (< 100 ms), car elle est
     * déjà en cache dans le fournisseur fusionné. Idéale pour la sauvegarde automatique
     * au moment où la voiture s'arrête : elle roulait à l'instant, donc la position en
     * cache est fraîche et précise. Retourne null si aucune position récente n'est
     * disponible (plus vieille que [maxAgeMs]), auquel cas on retombe sur un fix frais.
     *
     * C'est ce qui garantit un enregistrement fiable : contrairement à
     * [getCurrentLocation], on n'attend pas un nouveau fix GPS (10-30 s en parking
     * souterrain), qui dépassait souvent la fenêtre du receiver et faisait échouer la
     * sauvegarde de façon aléatoire.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(context: Context, maxAgeMs: Long = 2 * 60_000L): LatLng? =
        suspendCancellableCoroutine { cont ->
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation
                .addOnSuccessListener { last ->
                    if (last != null && System.currentTimeMillis() - last.time <= maxAgeMs) {
                        cont.resume(LatLng(last.latitude, last.longitude))
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { cont.resume(null) }
        }

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
            // Version synchrone (dépréciée sur API 33+ mais fonctionnelle) : les appelants
            // sont déjà hors du thread principal. L'ancienne version lançait en plus une
            // requête asynchrone dont le résultat était jeté → double appel réseau.
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)?.firstOrNull()?.let { buildAddress(it) }
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

    /**
     * Itinéraire à pied vers la voiture. URL universelle Google Maps : ouverte par
     * l'appli Maps si elle est installée, sinon par le navigateur. Remplace l'ancien
     * test resolveActivity(), qui renvoyait toujours null sur Android 11+ (visibilité
     * des paquets) et faisait donc systématiquement passer par le navigateur.
     */
    fun walkingDirectionsIntent(latitude: Double, longitude: Double): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

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
