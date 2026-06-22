package com.carparking.app.data.repository

import com.carparking.app.data.database.CarDao
import com.carparking.app.data.model.Car
import kotlinx.coroutines.flow.Flow

class CarRepository(private val carDao: CarDao) {
    val allCars: Flow<List<Car>> = carDao.getAllCars()

    suspend fun getCarById(id: Int): Car? = carDao.getCarById(id)
    suspend fun insertCar(car: Car): Long = carDao.insertCar(car)
    suspend fun updateCar(car: Car) = carDao.updateCar(car)
    suspend fun deleteCar(car: Car) = carDao.deleteCar(car)
}
