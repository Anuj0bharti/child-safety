package com.childsafety.watch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PendingEventEntity)

    @Query("SELECT * FROM pending_events ORDER BY isCritical DESC, timestamp ASC")
    suspend fun getAllPending(): List<PendingEventEntity>

    @Query("DELETE FROM pending_events WHERE eventId = :eventId")
    suspend fun deleteEvent(eventId: String)

    @Query("SELECT COUNT(*) FROM pending_events")
    suspend fun getPendingCount(): Int
}

