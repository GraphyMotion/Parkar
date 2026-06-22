package com.carparking.app.data.database

import androidx.room.*
import com.carparking.app.data.model.ParkingPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingPhotoDao {
    @Query("SELECT * FROM parking_photos WHERE parkingId = :parkingId ORDER BY addedAt ASC")
    fun getPhotosForParking(parkingId: Int): Flow<List<ParkingPhoto>>

    @Query("SELECT * FROM parking_photos WHERE parkingId = :parkingId ORDER BY addedAt ASC")
    suspend fun getPhotosForParkingSync(parkingId: Int): List<ParkingPhoto>

    @Insert
    suspend fun insertPhoto(photo: ParkingPhoto): Long

    @Delete
    suspend fun deletePhoto(photo: ParkingPhoto)

    @Query("DELETE FROM parking_photos WHERE parkingId = :parkingId")
    suspend fun deleteAllForParking(parkingId: Int)
}
