package com.carparking.app.bluetooth

import android.companion.CompanionDeviceService
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service compagnon : c'est **le système Android** qui le réveille quand la voiture
 * associée apparaît ou disparaît — y compris lorsque l'appli est totalement fermée
 * ou balayée des applis récentes.
 *
 * C'est ce qui corrige le défaut du receiver classique : une appli balayée des
 * récentes passe en « état stoppé » sur beaucoup de ROMs (Xiaomi, Samsung…), et
 * dans cet état Android ne lui livre plus aucun broadcast. Le Companion Device
 * Manager contourne ce problème, sans imposer de notification permanente.
 *
 * Requiert Android 12 (API 31) pour l'observation de présence.
 */
@RequiresApi(Build.VERSION_CODES.S)
class CarCompanionService : CompanionDeviceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "CarCompanionService"
        private const val SOURCE = "compagnon"
    }

    // On surcharge les variantes String : sur Android 13+, l'implémentation par défaut
    // de onDeviceAppeared(AssociationInfo) les appelle. Une seule surcharge couvre
    // donc Android 12, 13 et suivants.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onDeviceAppeared(address: String) {
        Log.d(TAG, "Voiture détectée : $address")
        scope.launch { AutoParkHandler.handleConnected(applicationContext, address, SOURCE) }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onDeviceDisappeared(address: String) {
        Log.d(TAG, "Voiture éloignée : $address")
        scope.launch { AutoParkHandler.handleDisconnected(applicationContext, address, SOURCE) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
