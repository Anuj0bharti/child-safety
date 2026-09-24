package com.childsafety.watch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.childsafety.watch.ChildSafetyWatchApp
import com.childsafety.watch.battery.BatteryMonitor
import com.childsafety.watch.location.LocationTracker
import com.childsafety.watch.network.NetworkMonitor
import com.childsafety.watch.sensors.FallDetector
import com.childsafety.watch.sensors.HeartRateMonitor
import com.childsafety.watch.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SafetyMonitoringService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var locationTracker: LocationTracker
    private lateinit var fallDetector: FallDetector
    private lateinit var heartRateMonitor: HeartRateMonitor
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var batteryMonitor: BatteryMonitor

    private val notificationChannelId = "child_safety_monitoring_channel"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        val app = application as ChildSafetyWatchApp

        locationTracker = LocationTracker(this)
        networkMonitor = NetworkMonitor(this)
        batteryMonitor = BatteryMonitor(this)
        heartRateMonitor = HeartRateMonitor(this)

        fallDetector = FallDetector(
            context = this,
            onAbnormalMovement = { magnitude, timestamp ->
                serviceScope.launch {
                    val prefs = getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    val isPaired = prefs.getBoolean("is_paired", false)
                    val childId = prefs.getString("child_id", null) ?: ""
                    val deviceId = prefs.getString("device_id", null) ?: ""
                    val deviceToken = prefs.getString("device_token", null) ?: ""

                    if (isPaired && childId.isNotEmpty() && deviceToken.isNotEmpty()) {
                        val payload = mapOf<String, Any?>(
                            "event_type" to "abnormal_movement",
                            "raw_data" to mapOf("magnitude" to magnitude, "timestamp" to timestamp)
                        )
                        val directResult = app.apiService.sendSensorEvent(
                            deviceToken,
                            childId,
                            deviceId,
                            "abnormal_movement",
                            mapOf("magnitude" to magnitude)
                        )
                        if (directResult.isFailure) {
                            app.syncRepository.enqueueEvent("ABNORMAL_MOVEMENT", payload, isCritical = false)
                        }
                    }
                }
            },
            onInactivity = { timestamp ->
                serviceScope.launch {
                    val prefs = getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    val isPaired = prefs.getBoolean("is_paired", false)
                    val childId = prefs.getString("child_id", null) ?: ""
                    val deviceId = prefs.getString("device_id", null) ?: ""
                    val deviceToken = prefs.getString("device_token", null) ?: ""

                    if (isPaired && childId.isNotEmpty() && deviceToken.isNotEmpty()) {
                        val payload = mapOf<String, Any?>(
                            "event_type" to "prolonged_inactivity",
                            "raw_data" to mapOf("timestamp" to timestamp)
                        )
                        val directResult = app.apiService.sendSensorEvent(
                            deviceToken,
                            childId,
                            deviceId,
                            "prolonged_inactivity",
                            mapOf("timestamp" to timestamp)
                        )
                        if (directResult.isFailure) {
                            app.syncRepository.enqueueEvent("INACTIVITY", payload, isCritical = false)
                        }
                    }
                }
            },
            onFallDetected = { magnitude, timestamp ->
                serviceScope.launch {
                    val prefs = getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    val isPaired = prefs.getBoolean("is_paired", false)
                    val childId = prefs.getString("child_id", null) ?: ""
                    val deviceId = prefs.getString("device_id", null) ?: ""
                    val deviceToken = prefs.getString("device_token", null) ?: ""

                    if (isPaired && childId.isNotEmpty() && deviceToken.isNotEmpty()) {
                        val payload = mapOf<String, Any?>(
                            "event_type" to "possible_fall",
                            "raw_data" to mapOf("magnitude" to magnitude, "timestamp" to timestamp)
                        )

                        val directResult = app.apiService.sendSensorEvent(
                            deviceToken,
                            childId,
                            deviceId,
                            "possible_fall",
                            mapOf("magnitude" to magnitude)
                        )

                        if (directResult.isFailure) {
                            app.syncRepository.enqueueEvent("FALL", payload, isCritical = false)
                        }
                    }
                }
            }
        )

        createNotificationChannel()
        startForeground(notificationId, buildNotification("Active Monitoring"))

        locationTracker.startTracking(intervalMillis = 5000L)
        fallDetector.start()
        networkMonitor.startMonitoring()
        batteryMonitor.start()
        heartRateMonitor.startListening()

        // Background periodic location telemetry & sync loop
        serviceScope.launch {
            while (true) {
                delay(10000L) // 10s telemetry interval

                val prefs = getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                val isPaired = prefs.getBoolean("is_paired", false)
                val childId = prefs.getString("child_id", null) ?: ""
                val deviceId = prefs.getString("device_id", null) ?: ""
                val deviceToken = prefs.getString("device_token", null) ?: ""

                val loc = locationTracker.locationFlow.value
                val net = networkMonitor.statusFlow.value
                val bat = batteryMonitor.batteryFlow.value

                // Send heartbeat if device registered
                if (deviceId.isNotEmpty() && deviceToken.isNotEmpty()) {
                    val hbRes = app.apiService.sendHeartbeat(deviceToken, deviceId, bat.percent, net.transport.name)
                    if (hbRes.isSuccess) {
                        val hb = hbRes.getOrNull()
                        if (hb != null) {
                            com.childsafety.watch.ChildSafetyWatchApp.safetyStatusFlow.value = com.childsafety.watch.WatchSafetyStatus(
                                childStatus = hb.child_status ?: "SAFE",
                                dispatchStatus = hb.dispatch_status ?: "NONE",
                                responderName = hb.responder_name
                            )
                        }
                    }
                }

                // If not paired yet, check status periodically
                if (!isPaired && deviceId.isNotEmpty() && deviceToken.isNotEmpty()) {
                    val statusRes = app.apiService.checkDeviceStatus(deviceId, deviceToken)
                    if (statusRes.isSuccess) {
                        val st = statusRes.getOrThrow()
                        if (st.is_paired && !st.child_id.isNullOrEmpty()) {
                            prefs.edit()
                                .putBoolean("is_paired", true)
                                .putString("child_id", st.child_id)
                                .apply()
                        }
                    }
                }

                // If genuine paired state active and GPS fix available, send location or queue if offline
                if (isPaired && childId.isNotEmpty() && deviceToken.isNotEmpty()) {
                    if (loc.isAvailable) {
                        val result = app.apiService.sendLocation(
                            deviceToken = deviceToken,
                            childId = childId,
                            deviceId = deviceId,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = loc.accuracyMeters,
                            speed = loc.speed
                        )
                        if (result.isFailure) {
                            app.syncRepository.enqueueEvent("LOCATION", mapOf(
                                "latitude" to loc.latitude,
                                "longitude" to loc.longitude,
                                "accuracy_meters" to loc.accuracyMeters,
                                "speed" to loc.speed
                            ))
                        }
                    }

                    // If online, sync pending offline events
                    if (net.isConnected) {
                        app.syncRepository.syncPendingEvents(deviceToken, childId, deviceId)
                    }

                    // If heart rate is available and newly read, transmit heart rate telemetry
                    val hr = heartRateMonitor.stateFlow.value
                    if (hr.bpm != null && hr.bpm > 0) {
                        app.apiService.sendSensorEvent(
                            deviceToken = deviceToken,
                            childId = childId,
                            deviceId = deviceId,
                            eventType = "heart_rate",
                            rawData = mapOf("bpm" to hr.bpm, "timestamp" to hr.lastReadingTimestamp)
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopTracking()
        fallDetector.stop()
        networkMonitor.stopMonitoring()
        batteryMonitor.stop()
        heartRateMonitor.stopListening()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Child Safety Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors safety telemetry in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Child Safety Active")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}

