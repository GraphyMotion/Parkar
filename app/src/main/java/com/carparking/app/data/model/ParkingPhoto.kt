package com.carparking.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parking_photos",
    foreignKeys = [ForeignKey(
        entity = ParkingRecord::class,
        parentColumns = ["id"],
        childColumns = ["parkingId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("parkingId")]
)
data class ParkingPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parkingId: Int,
    val photoPath: String,
    val addedAt: Long = System.currentTimeMillis()
)
