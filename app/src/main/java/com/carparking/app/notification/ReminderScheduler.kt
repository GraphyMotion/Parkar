package com.carparking.app.notification

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun schedule(
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
