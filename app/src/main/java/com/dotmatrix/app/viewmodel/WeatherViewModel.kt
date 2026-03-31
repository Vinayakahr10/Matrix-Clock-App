package com.dotmatrix.app.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotmatrix.app.weather.WeatherForecast
import com.dotmatrix.app.weather.WeatherRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "WeatherViewModel"
    private val repo = WeatherRepository(application.applicationContext)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)

    private val _forecast = MutableStateFlow<WeatherForecast?>(null)
    val forecast: StateFlow<WeatherForecast?> = _forecast.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(hasLocationPermission())
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    init {
        if (_locationPermissionGranted.value) {
            refreshWeather()
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _locationPermissionGranted.value = granted
        if (granted) refreshWeather()
        else _error.value = "Location permission is required to show weather"
    }

    fun refreshWeather() {
        if (!hasLocationPermission()) {
            _locationPermissionGranted.value = false
            _error.value = "Location permission not granted"
            return
        }
        fetchLocationThenForecast()
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationThenForecast() {
        _isLoading.value = true
        _error.value = null

        // Try last known location first for speed
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch {
                        fetchForecast(location)
                    }
                } else {
                    // Request fresh location
                    requestFreshLocation()
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "lastLocation failed, requesting fresh", e)
                requestFreshLocation()
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation() {
        val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(req, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val loc = result.lastLocation ?: run {
                    _isLoading.value = false
                    _error.value = "Could not determine location"
                    return
                }
                viewModelScope.launch { fetchForecast(loc) }
            }
        }, Looper.getMainLooper())
    }

    private suspend fun fetchForecast(location: android.location.Location) {
        val result = repo.fetch7DayForecast(location)
        _isLoading.value = false
        result.fold(
            onSuccess = {
                _forecast.value = it
                _error.value = null
            },
            onFailure = {
                _error.value = it.message ?: "Failed to fetch weather"
            }
        )
    }

    private fun hasLocationPermission(): Boolean {
        val ctx = getApplication<Application>().applicationContext
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
}
