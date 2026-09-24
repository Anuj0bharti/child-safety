package com.childsafety.watch.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.childsafety.watch.ChildSafetyWatchApp
import com.childsafety.watch.battery.BatteryInfo
import com.childsafety.watch.location.WatchLocationData
import com.childsafety.watch.network.NetworkStatus
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    locationData: WatchLocationData,
    networkStatus: NetworkStatus,
    batteryInfo: BatteryInfo,
    onNavigateToDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as ChildSafetyWatchApp

    val safetyStatus by com.childsafety.watch.ChildSafetyWatchApp.safetyStatusFlow.collectAsState()
    var currentStatus by remember { mutableStateOf("SAFE") }
    var statusColor by remember { mutableStateOf(Color(0xFF10B981)) } // Emerald
    var buttonText by remember { mutableStateOf("SOS") }
    var buttonColor by remember { mutableStateOf(Color(0xFFDC2626)) }
    var isSosDispatched by remember { mutableStateOf(false) }
    var previousDispatchStatus by remember { mutableStateOf("NONE") }

    // Dynamic state determination & real-time dispatcher feedback
    LaunchedEffect(batteryInfo, locationData, networkStatus, isSosDispatched, safetyStatus) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        if (safetyStatus.dispatchStatus == "RESPONDING") {
            currentStatus = "HELP ON THE WAY"
            statusColor = Color(0xFF06B6D4) // Cyan
            buttonText = "HELP COMING"
            buttonColor = Color(0xFF0284C7)
            if (previousDispatchStatus != "RESPONDING") {
                previousDispatchStatus = "RESPONDING"
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 500), -1))
                } else {
                    vibrator?.vibrate(600)
                }
            }
        } else if (safetyStatus.dispatchStatus == "ACKNOWLEDGED") {
            currentStatus = "DISPATCH NOTIFIED"
            statusColor = Color(0xFFF59E0B) // Amber
            buttonText = "ALERT SEEN"
            buttonColor = Color(0xFFD97706)
            if (previousDispatchStatus != "ACKNOWLEDGED") {
                previousDispatchStatus = "ACKNOWLEDGED"
                vibrator?.vibrate(300)
            }
        } else if (safetyStatus.dispatchStatus == "RESOLVED" || safetyStatus.childStatus == "SAFE") {
            if (isSosDispatched) {
                isSosDispatched = false
                vibrator?.vibrate(200)
            }
            previousDispatchStatus = "RESOLVED"
            buttonText = "SOS"
            buttonColor = Color(0xFFDC2626)
            if (!networkStatus.isConnected) {
                currentStatus = "OFFLINE"
                statusColor = Color(0xFFF59E0B)
            } else if (batteryInfo.isLow) {
                currentStatus = "LOW BATTERY"
                statusColor = Color(0xFFF59E0B)
            } else if (!locationData.isAvailable) {
                currentStatus = "GPS SEARCHING"
                statusColor = Color(0xFF3B82F6)
            } else {
                currentStatus = "SAFE"
                statusColor = Color(0xFF10B981)
            }
        } else if (isSosDispatched || safetyStatus.childStatus == "CRITICAL") {
            currentStatus = "SOS SENT"
            statusColor = Color(0xFFEF4444)
            buttonText = "SOS SENT"
            buttonColor = Color(0xFFDC2626)
        } else if (!networkStatus.isConnected) {
            currentStatus = "OFFLINE"
            statusColor = Color(0xFFF59E0B)
            buttonText = "SOS"
            buttonColor = Color(0xFFDC2626)
        } else if (batteryInfo.isLow) {
            currentStatus = "LOW BATTERY"
            statusColor = Color(0xFFF59E0B)
            buttonText = "SOS"
            buttonColor = Color(0xFFDC2626)
        } else if (!locationData.isAvailable) {
            currentStatus = "GPS SEARCHING"
            statusColor = Color(0xFF3B82F6)
            buttonText = "SOS"
            buttonColor = Color(0xFFDC2626)
        } else {
            currentStatus = "SAFE"
            statusColor = Color(0xFF10B981)
            buttonText = "SOS"
            buttonColor = Color(0xFFDC2626)
        }
    }

    val triggerSOS = {
        isSosDispatched = true
        // Tactile emergency haptic feedback
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
            } else {
                it.vibrate(500)
            }
        }

        coroutineScope.launch {
            val prefs = context.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
            val childId = prefs.getString("child_id", null) ?: ""
            val deviceId = prefs.getString("device_id", null) ?: ""
            val deviceToken = prefs.getString("device_token", null) ?: ""

            if (childId.isNotEmpty() && deviceToken.isNotEmpty()) {
                val result = app.apiService.sendSOS(
                    deviceToken = deviceToken,
                    childId = childId,
                    deviceId = deviceId,
                    latitude = if (locationData.isAvailable) locationData.latitude else null,
                    longitude = if (locationData.isAvailable) locationData.longitude else null,
                    accuracyMeters = if (locationData.isAvailable) locationData.accuracyMeters else null,
                    batteryPercent = batteryInfo.percent,
                    networkTransport = networkStatus.transport.name
                )

                if (result.isFailure) {
                    // Guaranteed offline persistence: queue SOS in local Room DB
                    app.syncRepository.enqueueEvent(
                        eventType = "SOS",
                        payload = mapOf(
                            "latitude" to locationData.latitude,
                            "longitude" to locationData.longitude,
                            "accuracy_meters" to locationData.accuracyMeters,
                            "battery_percent" to batteryInfo.percent,
                            "network_transport" to networkStatus.transport.name
                        ),
                        isCritical = true
                    )
                }
            }
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp, start = 12.dp, end = 12.dp)
    ) {
        item {
            Text(
                text = "CHILD SAFETY",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Status Badge
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentStatus,
                    color = statusColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Telemetry readout
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                TelemetryRow(label = "GPS", value = if (locationData.isAvailable) "CONNECTED" else "SEARCHING")
                TelemetryRow(label = "NETWORK", value = networkStatus.statusText)
                TelemetryRow(label = "BATTERY", value = if (batteryInfo.percent != null) "${batteryInfo.percent}%" else "UNAVAILABLE")
                TelemetryRow(label = "ROUTE", value = "ON ROUTE")
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Large SOS Button
        item {
            Button(
                onClick = { triggerSOS() },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = buttonColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Diagnostics link
        item {
            CompactChip(
                onClick = onNavigateToDiagnostics,
                label = { Text("Device Diagnostics", fontSize = 10.sp) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

