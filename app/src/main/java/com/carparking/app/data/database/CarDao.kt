package com.carparking.app.data.database

import androidx.room.*
import com.carparking.app.data.model.Car
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY createdAt DESC")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars ORDER BY createdAt DESC")
    suspend fun getAllCarsSync(): List<Car>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getCarById(id: Int): Car?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car): Long

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)
}
