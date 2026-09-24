package com.childsafety.watch.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HeartRateState(
    val bpm: Int? = null,
    val isAvailable: Boolean = false,
    val permissionGranted: Boolean = false,
    val statusText: String = "INITIALIZING",
    val lastReadingTimestamp: Long = 0L
)

class HeartRateMonitor(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val heartRateSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private val _stateFlow = MutableStateFlow(HeartRateState())
    val stateFlow: StateFlow<HeartRateState> = _stateFlow.asStateFlow()

    private var isListening = false

    fun checkStatus() {
        val hasSensor = heartRateSensor != null
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED

        val status = when {
            !hasSensor -> "NOT AVAILABLE"
            !hasPermission -> "PERMISSION REQUIRED"
            else -> "AVAILABLE"
        }

        _stateFlow.value = _stateFlow.value.copy(
            isAvailable = hasSensor,
            permissionGranted = hasPermission,
            statusText = status
        )
    }

    fun startListening() {
        checkStatus()
        if (isListening || heartRateSensor == null || !_stateFlow.value.permissionGranted) {
            return
        }

        sensorManager?.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        isListening = true
    }

    fun stopListening() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_HEART_RATE) return

        val rate = event.values.firstOrNull()?.toInt()
        if (rate != null && rate > 0) {
            _stateFlow.value = _stateFlow.value.copy(
                bpm = rate,
                statusText = "$rate BPM",
                lastReadingTimestamp = System.currentTimeMillis()
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

