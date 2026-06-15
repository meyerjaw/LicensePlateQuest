package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity

@Dao
interface EventLogDao {

    @Insert
    suspend fun insert(event: EventLogEntity)

    /** Delete every event-log row (debug-only wipe). */
    @Query("DELETE FROM event_log")
    suspend fun deleteAll()

    /** All event-log rows (backup export). */
    @Query("SELECT * FROM event_log")
    suspend fun getAll(): List<EventLogEntity>

    /** Bulk insert, skipping rows whose primary key already exists (backup import / merge). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(events: List<EventLogEntity>)
}
