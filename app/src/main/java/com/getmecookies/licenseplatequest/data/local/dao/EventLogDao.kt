package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import com.getmecookies.licenseplatequest.data.local.entity.EventLogEntity

@Dao
interface EventLogDao {

    @Insert
    suspend fun insert(event: EventLogEntity)
}
