package com.carparking.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.carparking.app.data.database.AppDatabase

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val carId = inputData.getInt(KEY_CAR_ID, -1)
        // Garde unique pour tous les cas (voiture reprise via Bluetooth, « J'ai retrouvé »,
        // nouveau stationnement, rappel remplacé) : on ne notifie que si ce rappel est
        // encore celui du stationnement actif de la voiture. -1 = rappel programmé par
        // une ancienne version, sans carId : comportement d'origine.
        if (carId != -1) {
            val active = AppDatabase.getDatabase(applicationContext).parkingRecordDao()
                .getAllActiveParkingsSync().firstOrNull { it.carId == carId }
            if (active?.reminderWorkerId != id.toString()) return Result.success()
        }
        val carName = inputData.getString(KEY_CAR_NAME) ?: "votre voiture"
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 1)
        NotificationHelper.showParkingReminder(
            applicationContext, carName, notificationId, inputData.getString(KEY_MESSAGE)
        )
        return Result.success()
    }

    companion object {
        const val KEY_CAR_NAME = "car_name"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_CAR_ID = "car_id"
        const val KEY_MESSAGE = "message"
    }
}
