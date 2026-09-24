package com.childsafety.watch.data.sync

import com.childsafety.watch.data.local.PendingEventDao
import com.childsafety.watch.data.local.PendingEventEntity
import com.childsafety.watch.data.remote.WatchApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class SyncRepository(
    private val pendingDao: PendingEventDao,
    private val apiService: WatchApiService
) {
    private val mutex = Mutex()
    private val gson = Gson()

    suspend fun enqueueEvent(
        eventType: String,
        payload: Map<String, Any?>,
        isCritical: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val eventId = UUID.randomUUID().toString()
        val entity = PendingEventEntity(
            eventId = eventId,
            eventType = eventType,
            payloadJson = gson.toJson(payload),
            timestamp = System.currentTimeMillis(),
            isCritical = isCritical
        )
        pendingDao.insertEvent(entity)
    }

    suspend fun syncPendingEvents(deviceToken: String, childId: String, deviceId: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val pendingList = pendingDao.getAllPending()
            var syncedCount = 0

            for (event in pendingList) {
                val mapType = object : TypeToken<Map<String, Any?>>() {}.type
                val payload: Map<String, Any?> = gson.fromJson(event.payloadJson, mapType)

                val result = when (event.eventType) {
                    "SOS" -> {
                        val lat = (payload["latitude"] as? Number)?.toDouble()
                        val lng = (payload["longitude"] as? Number)?.toDouble()
                        val acc = (payload["accuracy_meters"] as? Number)?.toFloat()
                        val bat = (payload["battery_percent"] as? Number)?.toInt()
                        val net = payload["network_transport"] as? String
                        apiService.sendSOS(deviceToken, childId, deviceId, lat, lng, acc, bat, net)
                    }
                    "LOCATION" -> {
                        val lat = (payload["latitude"] as? Number)?.toDouble() ?: 0.0
                        val lng = (payload["longitude"] as? Number)?.toDouble() ?: 0.0
                        val acc = (payload["accuracy_meters"] as? Number)?.toFloat()
                        val speed = (payload["speed"] as? Number)?.toFloat()
                        apiService.sendLocation(deviceToken, childId, deviceId, lat, lng, acc, speed)
                    }
                    "SENSOR", "FALL" -> {
                        val eventType = payload["event_type"] as? String ?: "possible_fall"
                        val rawData = payload["raw_data"] as? Map<String, Any>
                        apiService.sendSensorEvent(deviceToken, childId, deviceId, eventType, rawData)
                    }
                    else -> Result.failure(Exception("Unknown event type: ${event.eventType}"))
                }

                if (result.isSuccess) {
                    pendingDao.deleteEvent(event.eventId)
                    syncedCount++
                } else {
                    // Stop on network failure to preserve order and retry on next synchronization trigger
                    break
                }
            }
            syncedCount
        }
    }
}

