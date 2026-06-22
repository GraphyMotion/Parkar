package com.carparking.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val carName = inputData.getString(KEY_CAR_NAME) ?: "votre voiture"
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 1)
        NotificationHelper.showParkingReminder(applicationContext, carName, notificationId)
        return Result.success()
    }

    companion object {
        const val KEY_CAR_NAME = "car_name"
        const val KEY_NOTIFICATION_ID = "notification_id"
    }
}
