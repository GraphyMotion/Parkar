package com.carparking.app.notification

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    /** @param message texte personnalisé (ex. fin de stationnement payant), sinon texte par défaut. */
    fun schedule(
        context: Context,
        carName: String,
        delayMinutes: Long,
        carId: Int,
        message: String? = null
    ): String {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_CAR_NAME to carName,
                    ReminderWorker.KEY_NOTIFICATION_ID to carId,
                    ReminderWorker.KEY_CAR_ID to carId,
                    ReminderWorker.KEY_MESSAGE to message
                )
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
        return request.id.toString()
    }
}
