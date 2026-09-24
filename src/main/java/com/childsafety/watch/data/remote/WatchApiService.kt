package com.childsafety.watch.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class DeviceRegisterResponse(
    val id: String,
    val device_identifier: String,
    val pairing_code: String?,
    val device_token: String?,
    val is_paired: Boolean,
    val child_id: String?
)

data class DeviceStatusResponse(
    val id: String,
    val device_identifier: String,
    val is_paired: Boolean,
    val child_id: String?,
    val pairing_code: String?
)

data class HeartbeatResponse(
    val status: String,
    val last_heartbeat: String? = null,
    val child_status: String? = null,
    val dispatch_status: String? = null,
    val responder_name: String? = null
)

class WatchApiService(private var baseUrl: String = "http://10.0.2.2:8000/api/v1") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    fun updateBaseUrl(url: String) {
        this.baseUrl = url.trimEnd('/')
    }

    suspend fun registerDevice(
        deviceIdentifier: String,
        deviceModel: String = "Wear OS Smartwatch"
    ): Result<DeviceRegisterResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "device_identifier" to deviceIdentifier,
                "device_model" to deviceModel
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/devices/register")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val res = gson.fromJson(bodyStr, DeviceRegisterResponse::class.java)
                    Result.success(res)
                } else {
                    Result.failure(Exception("Registration failed HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkDeviceStatus(
        deviceId: String,
        deviceToken: String
    ): Result<DeviceStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/devices/$deviceId/status")
                .header("X-Device-Token", deviceToken)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val res = gson.fromJson(bodyStr, DeviceStatusResponse::class.java)
                    Result.success(res)
                } else {
                    Result.failure(Exception("Status check failed HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendLocation(
        deviceToken: String,
        childId: String,
        deviceId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        speed: Float?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "child_id" to childId,
                "device_id" to deviceId,
                "latitude" to latitude,
                "longitude" to longitude,
                "accuracy_meters" to accuracyMeters,
                "speed" to speed
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/telemetry/location")
                .header("X-Device-Token", deviceToken)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "")
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendSensorEvent(
        deviceToken: String,
        childId: String,
        deviceId: String,
        eventType: String,
        rawData: Map<String, Any>?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "child_id" to childId,
                "device_id" to deviceId,
                "event_type" to eventType,
                "raw_data" to rawData
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/telemetry/sensors")
                .header("X-Device-Token", deviceToken)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "")
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendSOS(
        deviceToken: String,
        childId: String,
        deviceId: String,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Float?,
        batteryPercent: Int?,
        networkTransport: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "child_id" to childId,
                "device_id" to deviceId,
                "latitude" to latitude,
                "longitude" to longitude,
                "accuracy_meters" to accuracyMeters,
                "battery_percent" to batteryPercent,
                "network_transport" to networkTransport
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/telemetry/sos")
                .header("X-Device-Token", deviceToken)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "")
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendHeartbeat(
        deviceToken: String,
        deviceId: String,
        batteryPercent: Int?,
        networkTransport: String?
    ): Result<HeartbeatResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "battery_percent" to batteryPercent,
                "network_transport" to networkTransport
            )
            val body = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/devices/$deviceId/heartbeat")
                .header("X-Device-Token", deviceToken)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val res = gson.fromJson(bodyStr, HeartbeatResponse::class.java)
                    Result.success(res)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


