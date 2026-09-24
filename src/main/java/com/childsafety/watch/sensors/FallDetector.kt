package com.childsafety.watch.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class FallDetector(
    private val context: Context,
    private val onAbnormalMovement: ((magnitude: Float, timestamp: Long) -> Unit)? = null,
    private val onInactivity: ((timestamp: Long) -> Unit)? = null,
    private val onFallDetected: (magnitude: Float, timestamp: Long) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isListening = false

    // Fall state machine thresholds
    private val freeFallThreshold = 3.0f // m/s^2 (below normal 9.8 m/s^2)
    private val impactThreshold = 25.0f   // m/s^2 (violent impact spike)
    private val abnormalMovementThreshold = 32.0f // m/s^2 (violent shake/struggle)
    private val stillnessThreshold = 0.5f // m/s^2 deviation from gravity (near zero motion)
    private val inactivityWindowMillis = 180000L // 3 minutes prolonged stillness
    private val windowMillis = 1500L

    private var lastFreeFallTimestamp = 0L
    private var lastImpactTimestamp = 0L
    private var lastMovementTimestamp = System.currentTimeMillis()
    private var lastAbnormalAlertTimestamp = 0L
    private var lastInactivityAlertTimestamp = 0L

    fun isAvailable(): Boolean {
        return accelerometer != null
    }

    fun start() {
        if (isListening || accelerometer == null) return
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        // 1. Detect Free Fall
        if (magnitude < freeFallThreshold) {
            lastFreeFallTimestamp = now
        }

        // 2. Detect High Impact Spike within window of free fall
        if (magnitude > impactThreshold) {
            if (now - lastFreeFallTimestamp < windowMillis) {
                lastImpactTimestamp = now
                // Potential fall detected - trigger real event
                onFallDetected(magnitude, now)
                // Reset timestamps to avoid repeated rapid triggers
                lastFreeFallTimestamp = 0L
            }
        }

        // 3. Detect Abnormal Violent Movement / Struggle (magnitude > 32 m/s^2 without free-fall)
        if (magnitude > abnormalMovementThreshold) {
            if (now - lastAbnormalAlertTimestamp > 10000L) { // Limit to once every 10s
                lastAbnormalAlertTimestamp = now
                onAbnormalMovement?.invoke(magnitude, now)
            }
        }

        // 4. Inactivity & Stillness Detection
        val gravityDelta = kotlin.math.abs(magnitude - 9.81f)
        if (gravityDelta > stillnessThreshold) {
            lastMovementTimestamp = now
        } else {
            // Check if prolonged stillness exceeded inactivity window
            if (now - lastMovementTimestamp > inactivityWindowMillis) {
                if (now - lastInactivityAlertTimestamp > 60000L) { // Limit to once every 60s
                    lastInactivityAlertTimestamp = now
                    onInactivity?.invoke(now)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    // Pure mathematical function for unit testing
    companion object {
        fun computeMagnitude(x: Float, y: Float, z: Float): Float {
            return sqrt(x * x + y * y + z * z)
        }

        fun isFallPattern(freeFallMag: Float, impactMag: Float, timeDiffMs: Long): Boolean {
            return freeFallMag < 3.0f && impactMag > 25.0f && timeDiffMs in 50..1500
        }
    }
}

