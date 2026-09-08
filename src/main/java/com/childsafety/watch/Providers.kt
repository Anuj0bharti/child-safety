package com.childsafety.watch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import java.util.concurrent.Executor

enum class AppMode { REAL, DEMO }
data class LocationState(val available: Boolean, val source: String, val latitude: Double? = null, val longitude: Double? = null, val accuracyMeters: Float? = null)
data class SensorState(val accelerometer: String, val gyroscope: String, val heartRate: String)
data class ConnectivityState(val transport: String, val internet: Boolean, val source: String)
data class BatteryState(val percent: Int?, val source: String)
data class HardwareSnapshot(val location: LocationState, val sensors: SensorState, val connectivity: ConnectivityState, val battery: BatteryState)

interface LocationProvider { fun current(): LocationState }
interface SensorProvider { fun current(): SensorState }
interface HeartRateProvider { fun capability(): String }
interface ConnectivityProvider { fun current(): ConnectivityState }
interface BatteryProvider { fun current(): BatteryState }

class RealLocationProvider(private val context: Context) : LocationProvider {
    override fun current(): LocationState {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return LocationState(false, "PERMISSION REQUIRED")
        val manager = context.getSystemService(LocationManager::class.java)
        val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstNotNullOfOrNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        return if (location == null) LocationState(false, "NO FIX") else LocationState(true, "REAL ${location.provider}", location.latitude, location.longitude, location.accuracy)
    }
}

class RealSensorProvider(private val context: Context) : SensorProvider {
    override fun current(): SensorState {
        val manager = context.getSystemService(SensorManager::class.java)
        fun present(type: Int) = if (manager.getDefaultSensor(type) == null) "UNAVAILABLE" else "AVAILABLE"
        return SensorState(present(Sensor.TYPE_ACCELEROMETER), present(Sensor.TYPE_GYROSCOPE), present(Sensor.TYPE_HEART_RATE))
    }
}

class RealHeartRateProvider(private val context: Context, private val executor: Executor, private val update: (String) -> Unit) : HeartRateProvider {
    private var value = "CHECKING CAPABILITY"
    init {
        runCatching { HealthServices.getClient(context).measureClient.getCapabilitiesAsync().addListener({ value = "CAPABILITY CHECK COMPLETE"; update(value) }, executor) }
            .onFailure { value = "HEALTH SERVICES UNAVAILABLE" }
    }
    override fun capability() = value
}

class RealConnectivityProvider(private val context: Context) : ConnectivityProvider {
    override fun current(): ConnectivityState {
        val network = context.getSystemService(ConnectivityManager::class.java).activeNetwork ?: return ConnectivityState("NO_INTERNET", false, "REAL")
        val c = context.getSystemService(ConnectivityManager::class.java).getNetworkCapabilities(network) ?: return ConnectivityState("UNKNOWN", false, "REAL")
        val transport = when {
            c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            c.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "BLUETOOTH"
            else -> "UNKNOWN"
        }
        return ConnectivityState(transport, c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED), "REAL")
    }
}

class RealBatteryProvider(private val context: Context) : BatteryProvider {
    override fun current(): BatteryState { val value = context.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY); return BatteryState(value.takeIf { it in 0..100 }, "REAL") }
}

class MockLocationProvider : LocationProvider { override fun current() = LocationState(true, "SIMULATED", 28.6139, 77.2090, 8.5f) }
class MockSensorProvider : SensorProvider { override fun current() = SensorState("SIMULATED", "SIMULATED", "SIMULATED") }
class MockConnectivityProvider : ConnectivityProvider { override fun current() = ConnectivityState("WIFI", true, "SIMULATED") }
class MockBatteryProvider : BatteryProvider { override fun current() = BatteryState(82, "SIMULATED") }
