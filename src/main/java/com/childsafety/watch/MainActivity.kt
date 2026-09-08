package com.childsafety.watch

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private var mode = AppMode.DEMO
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { render() }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)); buildUi(); render() }
    private fun buildUi() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 18, 22, 18) }
        val title = TextView(this).apply { text = "CHILD SAFETY"; textSize = 20f }
        status = TextView(this).apply { textSize = 13f; setPadding(0, 10, 0, 10) }
        val modeButton = Button(this).apply { text = "Switch to REAL MODE"; setOnClickListener { mode = if (mode == AppMode.DEMO) AppMode.REAL else AppMode.DEMO; text = if (mode == AppMode.DEMO) "Switch to REAL MODE" else "Switch to DEMO MODE"; render() } }
        val sos = Button(this).apply { text = "🚨  SOS"; textSize = 24f; setOnClickListener { status.text = "🚨 EMERGENCY\nSending emergency alert…"; EmergencyProvider(this@MainActivity).activate(mode, snapshot()) { message -> runOnUiThread { status.text = "${mode.name} MODE\n$message\n\n${format(snapshot())}" } } } }
        panel.addView(title); panel.addView(status); panel.addView(modeButton); panel.addView(sos); setContentView(panel)
    }
    private fun snapshot(): HardwareSnapshot {
        val location = if (mode == AppMode.DEMO) MockLocationProvider() else RealLocationProvider(this)
        val sensors = if (mode == AppMode.DEMO) MockSensorProvider() else RealSensorProvider(this)
        val network = if (mode == AppMode.DEMO) MockConnectivityProvider() else RealConnectivityProvider(this)
        val battery = if (mode == AppMode.DEMO) MockBatteryProvider() else RealBatteryProvider(this)
        return HardwareSnapshot(location.current(), sensors.current(), network.current(), battery.current())
    }
    private fun render() { status.text = "${mode.name} MODE\n${format(snapshot())}" }
    private fun format(s: HardwareSnapshot) = "GPS: ${s.location.source}\nNetwork: ${s.connectivity.transport} (${if (s.connectivity.internet) "INTERNET" else "OFFLINE"}) — ${s.connectivity.source}\nBattery: ${s.battery.percent ?: "UNKNOWN"}% — ${s.battery.source}\nAccelerometer: ${s.sensors.accelerometer}\nGyroscope: ${s.sensors.gyroscope}\nHeart rate: ${s.sensors.heartRate}"
}
