package com.carparking.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carparking.app.data.database.AppDatabase
import com.carparking.app.data.model.Car
import com.carparking.app.data.repository.CarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CarRepository

    val cars: StateFlow<List<Car>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = CarRepository(db.carDao())
        cars = repository.allCars.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    }

    fun addCar(car: Car) = viewModelScope.launch { repository.insertCar(car) }
    fun updateCar(car: Car) = viewModelScope.launch { repository.updateCar(car) }
    fun deleteCar(car: Car) = viewModelScope.launch { repository.deleteCar(car) }
    suspend fun getCarById(id: Int): Car? = repository.getCarById(id)
}
