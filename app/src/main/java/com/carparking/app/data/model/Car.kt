package com.carparking.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Durée du rappel de stationnement déclenché lors d'une sauvegarde automatique
     * (Bluetooth), en minutes. 0 = aucun rappel.
     *
     * Propre à chaque voiture : la durée pertinente n'est pas la même pour un
     * véhicule garé en zone bleue et un autre garé devant chez soi.
     */
    val autoReminderMinutes: Long = 0L
)
