package com.carparking.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.carparking.app.data.database.AppDatabase
import com.carparking.app.widget.CarParkingWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reçoit les actions des boutons de la notification persistante.
 */
class ParkingActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            OngoingParkingNotification.ACTION_ARRIVED -> {
                val carId = intent.getIntExtra(OngoingParkingNotification.EXTRA_CAR_ID, -1)
                if (carId == -1) return

                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AppDatabase.getDatabase(context)
                            .parkingRecordDao()
                            .deactivateParkingForCar(carId)

                        OngoingParkingNotification.dismiss(context, carId)

                        // Met à jour le widget
                        CarParkingWidget().updateAll(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
