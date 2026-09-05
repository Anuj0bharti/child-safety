package com.childsafety.parent

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ParentActivity : Activity() {
    private lateinit var content: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); if (android.os.Build.VERSION.SDK_INT >= 33) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1); buildUi(); refresh() }
    private fun buildUi() { val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 42, 36, 36) }; val title = TextView(this).apply { text = "CHILD SAFETY — PARENT"; textSize = 24f }; content = TextView(this).apply { textSize = 16f; setPadding(0, 24, 0, 24) }; val refresh = Button(this).apply { text = "Refresh child status"; setOnClickListener { refresh() } }; panel.addView(title); panel.addView(content); panel.addView(refresh); setContentView(panel) }
    private fun refresh() { content.text = "Connecting to backend…"; thread { try { val c = URL(BuildConfig.API_BASE_URL + "/api/v1/emergencies").openConnection() as HttpURLConnection; val data = c.inputStream.bufferedReader().use { it.readText() }; val cases = JSONArray(data); val active = (0 until cases.length()).map { cases.getJSONObject(it) }.filter { it.getString("status") != "RESOLVED" }; if (active.isNotEmpty()) notifyCritical(active[0].getString("child_id")); val text = if (active.isEmpty()) "CHILD-001\nSTATUS: SAFE\n\nNo active emergency cases." else active.joinToString("\n\n") { item -> "🚨 ${item.getString("risk_level")} ALERT\nChild: ${item.getString("child_id")}\nStatus: ${item.getString("status")}\nReason: ${item.getString("reason")}" }; runOnUiThread { content.text = text } } catch (_: Exception) { runOnUiThread { content.text = "Backend unavailable. No current child status can be confirmed." } } } }
    private fun notifyCritical(child: String) { val manager = getSystemService(NotificationManager::class.java); val channel = NotificationChannel("emergency", "Emergency alerts", NotificationManager.IMPORTANCE_HIGH); manager.createNotificationChannel(channel); if (android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) manager.notify(101, NotificationCompat.Builder(this, "emergency").setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Critical child safety alert").setContentText("Emergency case open for $child").setPriority(NotificationCompat.PRIORITY_HIGH).build()) }
}
