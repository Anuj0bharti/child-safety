package com.childsafety.watch.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.childsafety.watch.battery.BatteryMonitor
import com.childsafety.watch.location.LocationTracker
import com.childsafety.watch.network.NetworkMonitor
import com.childsafety.watch.service.SafetyMonitoringService
import com.childsafety.watch.ui.screens.DiagnosticsScreen
import com.childsafety.watch.ui.screens.HomeScreen
import com.childsafety.watch.ui.screens.PairingScreen

class MainActivity : ComponentActivity() {

    private lateinit var locationTracker: LocationTracker
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var batteryMonitor: BatteryMonitor

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Start tracking once permissions evaluated
        locationTracker.startTracking()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationTracker = LocationTracker(this)
        networkMonitor = NetworkMonitor(this)
        batteryMonitor = BatteryMonitor(this)

        // Request permissions
        val requiredPerms = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPerms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        requiredPerms.add(android.Manifest.permission.BODY_SENSORS)
        permissionLauncher.launch(requiredPerms.toTypedArray())

        // Start Foreground Service
        val serviceIntent = Intent(this, SafetyMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val prefs = getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
        val isPaired = prefs.getBoolean("is_paired", false)

        setContent {
            val navController = rememberSwipeDismissableNavController()
            val locationData by locationTracker.locationFlow.collectAsState()
            val networkStatus by networkMonitor.statusFlow.collectAsState()
            val batteryInfo by batteryMonitor.batteryFlow.collectAsState()

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = if (isPaired) "home" else "pairing"
            ) {
                composable("pairing") {
                    PairingScreen(
                        onPairingComplete = {
                            navController.navigate("home") {
                                popUpTo("pairing") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        locationData = locationData,
                        networkStatus = networkStatus,
                        batteryInfo = batteryInfo,
                        onNavigateToDiagnostics = {
                            navController.navigate("diagnostics")
                        }
                    )
                }
                composable("diagnostics") {
                    DiagnosticsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        locationTracker.startTracking()
        networkMonitor.startMonitoring()
        batteryMonitor.start()
    }

    override fun onPause() {
        super.onPause()
        // Service keeps running in background
    }
}

