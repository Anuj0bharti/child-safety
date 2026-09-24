package com.childsafety.watch.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryInfo(
    val percent: Int? = null,
    val isCharging: Boolean = false,
    val isLow: Boolean = false
)

class BatteryMonitor(private val context: Context) {

    private val _batteryFlow = MutableStateFlow(BatteryInfo())
    val batteryFlow: StateFlow<BatteryInfo> = _batteryFlow.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateFromIntent(it) }
        }
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(receiver, filter)
        intent?.let { updateFromIntent(it) }
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    private fun updateFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val percent = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            null
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        _batteryFlow.value = BatteryInfo(
            percent = percent,
            isCharging = isCharging,
            isLow = percent != null && percent <= 20
        )
    }
}

