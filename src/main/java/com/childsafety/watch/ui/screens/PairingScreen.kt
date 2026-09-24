package com.childsafety.watch.ui.screens

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.childsafety.watch.ChildSafetyWatchApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun PairingScreen(
    onPairingComplete: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ChildSafetyWatchApp
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE) }

    var pairingCode by remember { mutableStateOf(prefs.getString("pairing_code", null) ?: "") }
    var deviceId by remember { mutableStateOf(prefs.getString("device_id", null) ?: "") }
    var deviceToken by remember { mutableStateOf(prefs.getString("device_token", null) ?: "") }
    var statusText by remember { mutableStateOf("Registering watch...") }
    var isChecking by remember { mutableStateOf(false) }

    // Initialize registration on first load
    LaunchedEffect(Unit) {
        var devId = prefs.getString("device_id", null)
        var token = prefs.getString("device_token", null)
        var code = prefs.getString("pairing_code", null)

        var identifier = prefs.getString("device_identifier", null)
        if (identifier == null) {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            identifier = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                "WATCH-$androidId"
            } else {
                "WATCH-" + UUID.randomUUID().toString().take(8).uppercase()
            }
            prefs.edit().putString("device_identifier", identifier).apply()
        }

        if (devId.isNullOrEmpty() || token.isNullOrEmpty() || code.isNullOrEmpty()) {
            val modelName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            val regRes = app.apiService.registerDevice(identifier, modelName)
            if (regRes.isSuccess) {
                val data = regRes.getOrThrow()
                devId = data.id
                token = data.device_token ?: ""
                code = data.pairing_code ?: ""

                prefs.edit()
                    .putString("device_id", devId)
                    .putString("device_token", token)
                    .putString("pairing_code", code)
                    .putBoolean("is_paired", data.is_paired)
                    .apply()

                deviceId = devId
                deviceToken = token
                pairingCode = code

                if (data.is_paired && !data.child_id.isNullOrEmpty()) {
                    prefs.edit().putString("child_id", data.child_id).apply()
                    onPairingComplete()
                    return@LaunchedEffect
                }
            } else {
                statusText = "Registration error. Tap retry."
            }
        }

        // Periodic check loop
        while (true) {
            val currentDevId = prefs.getString("device_id", null)
            val currentToken = prefs.getString("device_token", null)
            if (!currentDevId.isNullOrEmpty() && !currentToken.isNullOrEmpty()) {
                val statusRes = app.apiService.checkDeviceStatus(currentDevId, currentToken)
                if (statusRes.isSuccess) {
                    val status = statusRes.getOrThrow()
                    if (status.is_paired && !status.child_id.isNullOrEmpty()) {
                        prefs.edit()
                            .putBoolean("is_paired", true)
                            .putString("child_id", status.child_id)
                            .apply()
                        onPairingComplete()
                        break
                    } else {
                        statusText = "Waiting for parent app..."
                    }
                } else {
                    statusText = "Connecting to backend..."
                }
            }
            delay(4000L)
        }
    }

    var serverUrl by remember { mutableStateOf(prefs.getString("server_url", "http://10.0.2.2:8000/api/v1") ?: "http://10.0.2.2:8000/api/v1") }
    var showServerConfig by remember { mutableStateOf(false) }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 12.dp, end = 12.dp)
    ) {
        item {
            Text(
                text = "PAIR WATCH",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter code in Parent App",
                color = Color.LightGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (pairingCode.isNotEmpty()) pairingCode else "...",
                    color = Color(0xFF38BDF8),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusText,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactButton(
                    onClick = {
                        scope.launch {
                            val currentDevId = prefs.getString("device_id", null)
                            val currentToken = prefs.getString("device_token", null)
                            if (!currentDevId.isNullOrEmpty() && !currentToken.isNullOrEmpty()) {
                                isChecking = true
                                val statusRes = app.apiService.checkDeviceStatus(currentDevId, currentToken)
                                if (statusRes.isSuccess) {
                                    val status = statusRes.getOrThrow()
                                    if (status.is_paired && !status.child_id.isNullOrEmpty()) {
                                        prefs.edit()
                                            .putBoolean("is_paired", true)
                                            .putString("child_id", status.child_id)
                                            .apply()
                                        onPairingComplete()
                                    } else {
                                        statusText = "Not paired yet. Check parent app."
                                    }
                                } else {
                                    statusText = "Connection error. Check Wi-Fi / IP."
                                }
                                isChecking = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0284C7))
                ) {
                    Text("Check", fontSize = 10.sp)
                }

                CompactButton(
                    onClick = {
                        scope.launch {
                            // Re-attempt device registration with current backend
                            val identifier = prefs.getString("device_identifier", "WATCH-" + UUID.randomUUID().toString().take(8)) ?: "WATCH-DEFAULT"
                            val modelName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                            statusText = "Reconnecting..."
                            val regRes = app.apiService.registerDevice(identifier, modelName)
                            if (regRes.isSuccess) {
                                val data = regRes.getOrThrow()
                                prefs.edit()
                                    .putString("device_id", data.id)
                                    .putString("device_token", data.device_token ?: "")
                                    .putString("pairing_code", data.pairing_code ?: "")
                                    .putBoolean("is_paired", data.is_paired)
                                    .apply()
                                deviceId = data.id
                                deviceToken = data.device_token ?: ""
                                pairingCode = data.pairing_code ?: ""
                                statusText = "Ready. Enter code."
                            } else {
                                statusText = "Failed: ${regRes.exceptionOrNull()?.message?.take(30)}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF334155))
                ) {
                    Text("Retry", fontSize = 10.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            val currentServer = prefs.getString("server_url", "http://10.0.2.2:8000/api/v1") ?: "http://10.0.2.2:8000/api/v1"
            CompactButton(
                onClick = {
                    // Quick cycle common local IPs or custom prompt
                    val presets = listOf(
                        "http://10.0.2.2:8000/api/v1",
                        "http://192.168.1.100:8000/api/v1",
                        "http://192.168.1.50:8000/api/v1",
                        "http://192.168.0.100:8000/api/v1"
                    )
                    val nextIdx = (presets.indexOf(currentServer) + 1).let { if (it >= presets.size || it < 0) 0 else it }
                    val newUrl = presets[nextIdx]
                    prefs.edit().putString("server_url", newUrl).apply()
                    app.apiService.updateBaseUrl(newUrl)
                    statusText = "Server switched. Tap Retry."
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E293B))
            ) {
                Text(
                    text = "Host: ${currentServer.replace("http://", "").replace("/api/v1", "")}",
                    fontSize = 8.sp,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}
