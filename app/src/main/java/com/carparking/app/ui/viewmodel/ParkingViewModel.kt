package com.carparking.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.carparking.app.data.database.AppDatabase
import com.carparking.app.data.model.ParkingPhoto
import com.carparking.app.data.model.ParkingRecord
import com.carparking.app.data.repository.ParkingRepository
import com.carparking.app.notification.OngoingParkingNotification
import com.carparking.app.notification.ReminderWorker
import com.carparking.app.widget.CarParkingWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class ParkingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ParkingRepository
    private val db = AppDatabase.getDatabase(application)

    val allActiveParkings: StateFlow<List<ParkingRecord>>

    init {
        repository = ParkingRepository(db.parkingRecordDao())
        allActiveParkings = repository.getAllActiveParkings().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    }

    fun getActiveParkingByCar(carId: Int): Flow<ParkingRecord?> =
        repository.getActiveParkingByCar(carId)

    fun getParkingsByCar(carId: Int): Flow<List<ParkingRecord>> =
        repository.getParkingsByCar(carId)

    fun getPhotosForParking(parkingId: Int): Flow<List<ParkingPhoto>> =
        db.parkingPhotoDao().getPhotosForParking(parkingId)

    /**
     * Sauvegarde le stationnement, les photos supplémentaires, active la notif persistante et le widget.
     * @param extraPhotoPaths Photos au-delà de la première (déjà dans parking.photoPath)
     */
    fun saveParking(
        parking: ParkingRecord,
        carName: String,
        reminderMinutes: Long?,
        context: Context,
        extraPhotoPaths: List<String> = emptyList()
    ) = viewModelScope.launch {
        var workerId: String? = null
        if (reminderMinutes != null && reminderMinutes > 0) {
            workerId = scheduleReminder(context, carName, reminderMinutes, parking.carId)
        }
        val newId = repository.saveParking(parking.copy(reminderWorkerId = workerId))

        // Sauvegarde les photos supplémentaires
        extraPhotoPaths.forEach { path ->
            db.parkingPhotoDao().insertPhoto(
                ParkingPhoto(parkingId = newId.toInt(), photoPath = path)
            )
        }

        // Notification persistante
        OngoingParkingNotification.show(
            context     = context,
            carId       = parking.carId,
            carName     = carName,
            address     = parking.address,
            latitude    = parking.latitude,
            longitude   = parking.longitude,
            parkedSince = parking.parkedAt
        )

        CarParkingWidget().updateAll(context)
    }

    fun deactivateParking(carId: Int, context: Context) = viewModelScope.launch {
        repository.deactivateForCar(carId)
        OngoingParkingNotification.dismiss(context, carId)
        CarParkingWidget().updateAll(context)
    }

    fun deleteParking(parking: ParkingRecord, context: Context) = viewModelScope.launch {
        parking.reminderWorkerId?.let { id ->
            WorkManager.getInstance(context).cancelWorkById(UUID.fromString(id))
        }
        db.parkingPhotoDao().deleteAllForParking(parking.id)
        repository.deleteParking(parking)
        OngoingParkingNotification.dismiss(context, parking.carId)
        CarParkingWidget().updateAll(context)
    }

    fun deletePhoto(photo: ParkingPhoto) = viewModelScope.launch {
        db.parkingPhotoDao().deletePhoto(photo)
    }

    /**
     * Corrige la position/adresse/note d'un stationnement existant
     * (ex: GPS imprécis au moment de se garer) et met à jour la notification
     * persistante et le widget si ce stationnement est actif.
     */
    fun correctParking(
        parking: ParkingRecord,
        newLatitude: Double,
        newLongitude: Double,
        newAddress: String?,
        newNote: String?,
        context: Context,
        carName: String
    ) = viewModelScope.launch {
        val updated = parking.copy(
            latitude = newLatitude,
            longitude = newLongitude,
            address = newAddress,
            note = newNote
        )
        repository.updateParking(updated)
        if (updated.isActive) {
            OngoingParkingNotification.show(
                context     = context,
                carId       = updated.carId,
                carName     = carName,
                address     = updated.address,
                latitude    = updated.latitude,
                longitude   = updated.longitude,
                parkedSince = updated.parkedAt
            )
            CarParkingWidget().updateAll(context)
        }
    }

    private fun scheduleReminder(
        context: Context,
        carName: String,
        delayMinutes: Long,
        notificationId: Int
    ): String {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_CAR_NAME to carName,
                    ReminderWorker.KEY_NOTIFICATION_ID to notificationId
                )
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
        return request.id.toString()
    }
}
