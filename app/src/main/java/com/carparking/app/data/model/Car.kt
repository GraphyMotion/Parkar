package com.carparking.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
