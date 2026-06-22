package com.carparking.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.carparking.app.bluetooth.BluetoothPreferences
import com.carparking.app.notification.AutoParkNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Détecte quand l'app passe en arrière-plan et propose de sauvegarder le parking
 * si l'utilisateur a des voitures mais aucun parking actif.
 */
class AppLifecycleObserver(
    private val appContext: Context
) : DefaultLifecycleObserver {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("parkar_auto_park", Context.MODE_PRIVATE)

    private val handler = Handler(Looper.getMainLooper())
    private var graceRunnable: Runnable? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val GRACE_DELAY_MS = 5_000L
        private const val KEY_LAST_PROMPT = "last_prompt_time"
        private const val PROMPT_COOLDOWN_MS = 30 * 60 * 1000L // 30 min cooldown
        // Fenêtre pendant laquelle une sauvegarde Bluetooth peut être en cours
        // (GPS + géocodage) : évite de proposer une sauvegarde manuelle en double
        private const val BLUETOOTH_SAVE_WINDOW_MS = 30_000L
    }

    override fun onStop(owner: LifecycleOwner) {
        graceRunnable?.let { handler.removeCallbacks(it) }

        graceRunnable = Runnable {
            scope.launch {
                if (shouldShowAutoParkPrompt()) {
                    AutoParkNotification.show(appContext)
                    prefs.edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply()
                }
            }
        }
        handler.postDelayed(graceRunnable!!, GRACE_DELAY_MS)
    }

    override fun onStart(owner: LifecycleOwner) {
        graceRunnable?.let { handler.removeCallbacks(it) }
        graceRunnable = null
    }

    private suspend fun shouldShowAutoParkPrompt(): Boolean {
        // Cooldown check
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0)
        if (System.currentTimeMillis() - lastPrompt < PROMPT_COOLDOWN_MS) return false

        // Une sauvegarde automatique Bluetooth est peut-être en cours (GPS + géocodage) :
        // attendre plutôt que de proposer une sauvegarde manuelle en double
        if (BluetoothPreferences.hasRecentDisconnectAttempt(appContext, BLUETOOTH_SAVE_WINDOW_MS)) return false

        val db = com.carparking.app.data.database.AppDatabase.getDatabase(appContext)
        val cars = db.carDao().getAllCarsSync()
        if (cars.isEmpty()) return false

        val activeParkings = db.parkingRecordDao().getAllActiveParkingsSync()
        return activeParkings.isEmpty()
    }
}
