package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity

@Dao
interface EventLogDao {

    @Insert
    suspend fun insert(event: EventLogEntity)

    /** Delete every event-log row (debug-only wipe). */
    @Query("DELETE FROM event_log")
    suspend fun deleteAll()
}
