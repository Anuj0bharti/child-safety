package com.childsafety.watch.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TransportType {
    CELLULAR,
    WIFI,
    BLUETOOTH,
    NO_INTERNET,
    UNKNOWN
}

data class NetworkStatus(
    val transport: TransportType = TransportType.UNKNOWN,
    val isConnected: Boolean = false,
    val statusText: String = "CHECKING..."
)

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _statusFlow = MutableStateFlow(NetworkStatus())
    val statusFlow: StateFlow<NetworkStatus> = _statusFlow.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkCapabilities(connectivityManager.getNetworkCapabilities(network))
        }

        override fun onLost(network: Network) {
            updateNetworkCapabilities(connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork))
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            updateNetworkCapabilities(capabilities)
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        updateNetworkCapabilities(connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork))
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
    }

    private fun updateNetworkCapabilities(capabilities: NetworkCapabilities?) {
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            _statusFlow.value = NetworkStatus(
                transport = TransportType.NO_INTERNET,
                isConnected = false,
                statusText = "NO INTERNET"
            )
            return
        }

        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TransportType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> TransportType.BLUETOOTH
            else -> TransportType.UNKNOWN
        }

        val text = when (transport) {
            TransportType.CELLULAR -> "LTE / CELLULAR"
            TransportType.WIFI -> "WI-FI"
            TransportType.BLUETOOTH -> "PHONE ASSISTED (BT)"
            else -> "CONNECTED"
        }

        _statusFlow.value = NetworkStatus(
            transport = transport,
            isConnected = true,
            statusText = text
        )
    }
}

