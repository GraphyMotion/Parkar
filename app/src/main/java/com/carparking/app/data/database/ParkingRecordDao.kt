package com.carparking.app.data.database

import androidx.room.*
import com.carparking.app.data.model.ParkingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingRecordDao {
    @Query("SELECT * FROM parking_records WHERE carId = :carId ORDER BY parkedAt DESC")
    fun getParkingRecordsByCar(carId: Int): Flow<List<ParkingRecord>>

    @Query("SELECT * FROM parking_records WHERE carId = :carId AND isActive = 1 LIMIT 1")
    fun getActiveParkingByCar(carId: Int): Flow<ParkingRecord?>

    @Query("SELECT * FROM parking_records WHERE isActive = 1")
    fun getAllActiveParkings(): Flow<List<ParkingRecord>>

    @Query("SELECT * FROM parking_records WHERE id = :id")
    suspend fun getParkingById(id: Int): ParkingRecord?

    // Versions suspend pour le widget (pas de Flow)
    @Query("SELECT * FROM parking_records WHERE isActive = 1")
    suspend fun getAllActiveParkingsSync(): List<ParkingRecord>

    @Query("SELECT * FROM parking_records WHERE carId = :carId ORDER BY parkedAt DESC")
    suspend fun getAllParkingsByCarSync(carId: Int): List<ParkingRecord>

    @Query("SELECT COUNT(*) FROM parking_records WHERE carId = :carId")
    suspend fun getParkingCountByCar(carId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParking(parking: ParkingRecord): Long

    @Update
    suspend fun updateParking(parking: ParkingRecord)

    @Query("UPDATE parking_records SET isActive = 0 WHERE carId = :carId AND isActive = 1")
    suspend fun deactivateParkingForCar(carId: Int)

    @Delete
    suspend fun deleteParking(parking: ParkingRecord)
}
