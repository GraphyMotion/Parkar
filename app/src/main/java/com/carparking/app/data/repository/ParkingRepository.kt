package com.carparking.app.data.repository

import com.carparking.app.data.database.ParkingRecordDao
import com.carparking.app.data.model.ParkingRecord
import kotlinx.coroutines.flow.Flow

class ParkingRepository(private val parkingDao: ParkingRecordDao) {

    fun getParkingsByCar(carId: Int): Flow<List<ParkingRecord>> =
        parkingDao.getParkingRecordsByCar(carId)

    fun getActiveParkingByCar(carId: Int): Flow<ParkingRecord?> =
        parkingDao.getActiveParkingByCar(carId)

    fun getAllActiveParkings(): Flow<List<ParkingRecord>> =
        parkingDao.getAllActiveParkings()

    suspend fun getParkingById(id: Int): ParkingRecord? = parkingDao.getParkingById(id)

    suspend fun saveParking(parking: ParkingRecord): Long {
        parkingDao.deactivateParkingForCar(parking.carId)
        return parkingDao.insertParking(parking)
    }

    suspend fun updateParking(parking: ParkingRecord) = parkingDao.updateParking(parking)
    suspend fun deleteParking(parking: ParkingRecord) = parkingDao.deleteParking(parking)
    suspend fun deactivateForCar(carId: Int) = parkingDao.deactivateParkingForCar(carId)
}
