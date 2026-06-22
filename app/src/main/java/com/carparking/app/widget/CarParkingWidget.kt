package com.carparking.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.carparking.app.MainActivity
import com.carparking.app.data.database.AppDatabase
import com.carparking.app.data.model.Car
import com.carparking.app.data.model.ParkingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ColWhite  = ColorProvider(Color(0xFFFFFFFF))
private val ColGreen  = ColorProvider(Color(0xFF4CAF50))
private val ColGrey   = ColorProvider(Color(0xFF9E9E9E))
private val ColLight  = ColorProvider(Color(0xFFB0BEC5))
private val ColBlue   = ColorProvider(Color(0xFF90CAF9))
private val ColNavy   = Color(0xFF0E1330)

class CarParkingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val cars: List<Car>
        val activeParkings: List<ParkingRecord>
        withContext(Dispatchers.IO) {
            cars = db.carDao().getAllCarsSync()
            activeParkings = db.parkingRecordDao().getAllActiveParkingsSync()
        }

        provideContent {
            GlanceTheme {
                WidgetRoot(context, cars, activeParkings)
            }
        }
    }
}

@Composable
private fun WidgetRoot(
    context: Context,
    cars: List<Car>,
    activeParkings: List<ParkingRecord>
) {
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColNavy)
            .cornerRadius(20.dp)
            .clickable(openApp),
        contentAlignment = Alignment.Center
    ) {
        when {
            cars.isEmpty()           -> EmptyState()
            activeParkings.isEmpty() -> NoParkingState(carCount = cars.size)
            else -> {
                val latest = activeParkings.maxByOrNull { it.parkedAt }!!
                ActiveParkingState(
                    car     = cars.firstOrNull { it.id == latest.carId },
                    parking = latest
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = GlanceModifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🚗 Parkar",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColWhite))
        Spacer(GlanceModifier.height(8.dp))
        Text("Ajoutez une voiture\npour commencer",
            style = TextStyle(fontSize = 12.sp, color = ColLight))
    }
}

@Composable
private fun NoParkingState(carCount: Int) {
    Column(
        modifier = GlanceModifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🚗 Parkar",
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColWhite))
        Spacer(GlanceModifier.height(6.dp))
        Text("$carCount voiture${if (carCount > 1) "s" else ""} · Aucune garée",
            style = TextStyle(fontSize = 13.sp, color = ColLight))
        Spacer(GlanceModifier.height(8.dp))
        Text("Appuyez pour ouvrir l'app",
            style = TextStyle(fontSize = 11.sp, color = ColGrey))
    }
}

@Composable
private fun ActiveParkingState(car: Car?, parking: ParkingRecord) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("● ", style = TextStyle(fontSize = 10.sp, color = ColGreen))
            Text(
                "Garée · ${formatDuration(parking.parkedAt)}",
                style = TextStyle(fontSize = 11.sp, color = ColGreen, fontWeight = FontWeight.Medium)
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            car?.name ?: "Ma voiture",
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColWhite),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(2.dp))
        if (!parking.address.isNullOrEmpty()) {
            Text(
                "📍 ${parking.address}",
                style = TextStyle(fontSize = 12.sp, color = ColLight),
                maxLines = 2
            )
        } else {
            Text(
                "📍 %.5f, %.5f".format(parking.latitude, parking.longitude),
                style = TextStyle(fontSize = 11.sp, color = ColGrey),
                maxLines = 1
            )
        }
        if (!parking.note.isNullOrEmpty()) {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                "💬 ${parking.note}",
                style = TextStyle(fontSize = 11.sp, color = ColGrey),
                maxLines = 1
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(
            "Appuyer pour ouvrir →",
            style = TextStyle(fontSize = 11.sp, color = ColBlue, fontStyle = FontStyle.Italic)
        )
    }
}

private fun formatDuration(parkedAtMs: Long): String {
    val diff    = System.currentTimeMillis() - parkedAtMs
    val minutes = diff / 60_000
    val hours   = minutes / 60
    val days    = hours / 24
    return when {
        days > 0    -> "${days}j ${hours % 24}h"
        hours > 0   -> "${hours}h${minutes % 60}min"
        minutes > 0 -> "${minutes}min"
        else        -> "à l'instant"
    }
}
