package com.carparking.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parking_records",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class ParkingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val carId: Int,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val note: String? = null,
    val photoPath: String? = null,
    val parkedAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null,
    val reminderWorkerId: String? = null,
    val isActive: Boolean = true
)
