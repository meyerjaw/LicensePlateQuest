package com.getmecookies.licenseplatequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.getmecookies.licenseplatequest.data.local.entity.TripEntity
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TripDao {

    @Insert
    suspend fun insert(trip: TripEntity)

    @Update
    suspend fun update(trip: TripEntity)

    @Query("SELECT * FROM trip ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<TripEntity>>

    /** The single active trip, if any (SPEC invariant: at most one). */
    @Query("SELECT * FROM trip WHERE status = :status LIMIT 1")
    suspend fun getByStatus(status: TripStatus): TripEntity?

    @Query("SELECT * FROM trip WHERE id = :id")
    suspend fun getById(id: UUID): TripEntity?
}
