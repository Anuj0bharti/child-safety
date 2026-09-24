package com.childsafety.watch.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.childsafety.watch.location.LocationTracker

@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val locationTracker = LocationTracker(context)

    // Check actual hardware
    val hasAccel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    val hasGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    val hasHeartRate = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null
    val hasLocationPermission = locationTracker.hasLocationPermission()

    val activeNetwork = connectivityManager?.activeNetwork
    val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
    val hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp, start = 10.dp, end = 10.dp)
    ) {
        item {
            Text(
                text = "HARDWARE DIAGNOSTICS",
                color = Color.Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DiagItem("DEVICE MODEL", Build.MODEL)
                DiagItem("WEAR OS VERSION", "Android ${Build.VERSION.RELEASE}")
                DiagItem("API LEVEL", Build.VERSION.SDK_INT.toString())
                DiagItem("GPS", if (hasLocationPermission) "AVAILABLE" else "PERMISSION REQUIRED")
                DiagItem("ACCELEROMETER", if (hasAccel) "AVAILABLE" else "UNAVAILABLE")
                DiagItem("GYROSCOPE", if (hasGyro) "AVAILABLE" else "UNAVAILABLE")
                DiagItem("HEART RATE", if (hasHeartRate) "AVAILABLE" else "PHYSICAL TEST REQ")
                DiagItem("CELLULAR / LTE", if (hasCellular) "CONNECTED" else "PHYSICAL TEST REQ")
                DiagItem("WI-FI", if (hasWifi) "CONNECTED" else "AVAILABLE")
                DiagItem("BLUETOOTH", "AVAILABLE")
                DiagItem("BATTERY SENSOR", "AVAILABLE")
                DiagItem("INTERNET", if (hasInternet) "AVAILABLE" else "OFFLINE")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            CompactButton(
                onClick = onBack,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text("Back", fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DiagItem(label: String, state: String) {
    val stateColor = when (state) {
        "AVAILABLE", "CONNECTED" -> Color(0xFF10B981)
        "UNAVAILABLE" -> Color(0xFFEF4444)
        "PERMISSION REQUIRED" -> Color(0xFFF59E0B)
        "PHYSICAL TEST REQ" -> Color(0xFF60A5FA)
        else -> Color.White
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        Text(text = state, color = stateColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

