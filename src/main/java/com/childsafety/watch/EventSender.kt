package com.childsafety.watch

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID
import kotlin.concurrent.thread

class EmergencyProvider(private val context: Context) {
    fun activate(mode: AppMode, snapshot: HardwareSnapshot, result: (String) -> Unit) {
        val event = JSONObject().apply {
            put("event_id", UUID.randomUUID().toString()); put("event_type", "SOS"); put("occurred_at", Instant.now().toString()); put("battery_percent", snapshot.battery.percent); put("network_transport", snapshot.connectivity.transport); put("mode", mode.name)
            snapshot.location.latitude?.let { latitude -> put("location", JSONObject().put("latitude", latitude).put("longitude", snapshot.location.longitude).put("accuracy_meters", snapshot.location.accuracyMeters)) }
        }
        context.getSharedPreferences("outbox", Context.MODE_PRIVATE).edit().putString("pending_sos", event.toString()).apply()
        if (!snapshot.connectivity.internet) { result("SOS RECORDED — OFFLINE; queued for synchronization") ; return }
        thread {
            try {
                val endpoint = if (mode == AppMode.DEMO) "/api/v1/demo/events" else "/api/v1/events"
                if (mode == AppMode.DEMO) event.put("device_id", "WATCH-001")
                val connection = (URL(BuildConfig.API_BASE_URL + endpoint).openConnection() as HttpURLConnection).apply { requestMethod = "POST"; doOutput = true; setRequestProperty("Content-Type", "application/json"); if (mode == AppMode.DEMO) setRequestProperty("X-Demo-Key", BuildConfig.DEMO_API_KEY) else setRequestProperty("X-Device-Token", context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("device_token", "") ?: "") }
                connection.outputStream.use { it.write(event.toString().toByteArray()) }
                if (connection.responseCode in 200..299) { context.getSharedPreferences("outbox", Context.MODE_PRIVATE).edit().remove("pending_sos").apply(); result("SOS SENT — CRITICAL case created") } else result("SOS QUEUED — backend returned ${connection.responseCode}")
            } catch (_: Exception) { result("SOS QUEUED — server unreachable") }
        }
    }
}
