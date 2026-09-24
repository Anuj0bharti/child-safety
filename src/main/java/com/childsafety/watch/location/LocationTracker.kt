package com.childsafety.watch.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WatchLocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Float = 0.0f,
    val speed: Float = 0.0f,
    val timestamp: Long = 0L,
    val isAvailable: Boolean = false,
    val statusText: String = "INITIALIZING"
)

class LocationTracker(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationFlow = MutableStateFlow(WatchLocationData())
    val locationFlow: StateFlow<WatchLocationData> = _locationFlow.asStateFlow()

    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            _locationFlow.value = WatchLocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                speed = location.speed,
                timestamp = location.time,
                isAvailable = true,
                statusText = "CONNECTED"
            )
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startTracking(intervalMillis: Long = 5000L) {
        if (isTracking || !hasLocationPermission()) {
            if (!hasLocationPermission()) {
                _locationFlow.value = _locationFlow.value.copy(
                    isAvailable = false,
                    statusText = "PERMISSION_REQUIRED"
                )
            }
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setWaitForAccurateLocation(true)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        isTracking = true
    }

    fun stopTracking() {
        if (isTracking) {
            fusedClient.removeLocationUpdates(locationCallback)
            isTracking = false
        }
    }
}

