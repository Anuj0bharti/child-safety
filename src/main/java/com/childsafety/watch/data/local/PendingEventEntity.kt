package com.childsafety.watch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_events")
data class PendingEventEntity(
    @PrimaryKey
    val eventId: String,
    val eventType: String, // "SOS", "LOCATION", "FALL", "HEART_RATE"
    val payloadJson: String,
    val timestamp: Long,
    val isCritical: Boolean = false,
    val retryCount: Int = 0
)

