package com.carparking.app.ui.components

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carparking.app.ui.theme.GoldAccent
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.roundToInt

/**
 * Boussole pointant vers la voiture garée avec distance mise à jour en temps réel.
 * Gère elle-même les mises à jour GPS continues — pas besoin de passer la position.
 */
@SuppressLint("MissingPermission")
@Composable
fun CompassToParking(
    carLat: Double,
    carLng: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ── Position utilisateur, mise à jour en continu ──────────────────────
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }

    DisposableEffect(Unit) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2_000L          // mise à jour toutes les 2 secondes
        )
            .setMinUpdateIntervalMillis(1_000L)  // au minimum toutes les 1 seconde
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    userLat = loc.latitude
                    userLng = loc.longitude
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        onDispose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    // ── Capteurs boussole (accéléromètre + magnétomètre) ─────────────────
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val gravity    = remember { FloatArray(3) }
    val geomagnetic = remember { FloatArray(3) }
    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER  ->
                        event.values.forEachIndexed { i, v -> gravity[i]     = v }
                    Sensor.TYPE_MAGNETIC_FIELD ->
                        event.values.forEachIndexed { i, v -> geomagnetic[i] = v }
                }
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sensorManager?.unregisterListener(listener) }
    }

    // ── Calcul bearing + distance ─────────────────────────────────────────
    val bearingToCar: Float
    val distanceMeters: Float?

    if (userLat != null && userLng != null) {
        val results = FloatArray(2)
        Location.distanceBetween(userLat!!, userLng!!, carLat, carLng, results)
        distanceMeters = results[0]
        bearingToCar   = results[1]
    } else {
        distanceMeters = null
        bearingToCar   = 0f
    }

    // ── Rotation animée de la flèche ─────────────────────────────────────
    val arrowRotation by animateFloatAsState(
        targetValue    = bearingToCar - azimuth,
        animationSpec  = tween(durationMillis = 250),
        label          = "compass_rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface    = MaterialTheme.colorScheme.onSurfaceVariant

    // ── UI ────────────────────────────────────────────────────────────────
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Distance
        Text(
            text = if (distanceMeters != null) formatDistance(distanceMeters)
                   else "Localisation…",
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color      = if (distanceMeters != null) primaryColor else onSurface
        )
        Text(
            text  = if (distanceMeters != null) "de votre voiture" else "Patientez…",
            style = MaterialTheme.typography.bodyMedium,
            color = onSurface
        )

        Spacer(Modifier.height(24.dp))

        // Boussole dessinée avec Canvas
        Canvas(modifier = Modifier.size(200.dp)) {
            val cx     = size.width  / 2f
            val cy     = size.height / 2f
            val radius = size.minDimension / 2f * 0.88f

            // Cercle de fond
            drawCircle(color = surfaceColor, radius = radius)

            // Graduations tous les 45°
            for (angle in 0 until 360 step 45) {
                val rad       = Math.toRadians(angle.toDouble()).toFloat()
                val tickStart = radius * 0.82f
                val tickEnd   = radius * 0.95f
                drawLine(
                    color       = onSurface.copy(alpha = 0.4f),
                    start       = Offset(cx + tickStart * kotlin.math.sin(rad), cy - tickStart * kotlin.math.cos(rad)),
                    end         = Offset(cx + tickEnd   * kotlin.math.sin(rad), cy - tickEnd   * kotlin.math.cos(rad)),
                    strokeWidth = if (angle % 90 == 0) 3f else 1.5f
                )
            }

            // Flèche vers la voiture (rotation animée)
            rotate(degrees = arrowRotation, pivot = Offset(cx, cy)) {
                val bodyLen = radius * 0.55f
                val tipLen  = radius * 0.22f
                val halfW   = radius * 0.10f

                // Pointe bleue (vers la voiture)
                drawPath(
                    path  = Path().apply {
                        moveTo(cx,               cy - bodyLen - tipLen)
                        lineTo(cx - halfW * 0.6f, cy - bodyLen)
                        lineTo(cx + halfW * 0.6f, cy - bodyLen)
                        close()
                    },
                    color = primaryColor
                )
                // Corps
                drawLine(
                    color       = primaryColor,
                    start       = Offset(cx, cy - bodyLen),
                    end         = Offset(cx, cy + bodyLen * 0.45f),
                    strokeWidth = halfW * 0.9f
                )
                // Queue rouge (côté opposé)
                drawPath(
                    path  = Path().apply {
                        moveTo(cx,               cy + bodyLen * 0.45f)
                        lineTo(cx - halfW * 0.5f, cy + bodyLen * 0.45f - tipLen * 0.7f)
                        lineTo(cx + halfW * 0.5f, cy + bodyLen * 0.45f - tipLen * 0.7f)
                        close()
                    },
                    color = Color.Red.copy(alpha = 0.85f)
                )
            }

            // Centre doré
            drawCircle(color = GoldAccent, radius = radius * 0.07f, center = Offset(cx, cy))
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text      = "Orientez votre téléphone\nvers la voiture",
            style     = MaterialTheme.typography.bodySmall,
            color     = onSurface,
            textAlign = TextAlign.Center
        )

        // Indicateur de rafraîchissement
        Spacer(Modifier.height(8.dp))
        Text(
            text  = if (userLat != null) "📡 GPS actif — mise à jour en temps réel"
                    else "🔄 Acquisition GPS…",
            style = MaterialTheme.typography.labelSmall,
            color = if (userLat != null)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDistance(meters: Float): String = when {
    meters < 10   -> "${meters.roundToInt()} m"
    meters < 1000 -> "${(meters / 5).roundToInt() * 5} m"   // arrondi à 5m
    else          -> "${"%.2f".format(meters / 1000)} km"
}
