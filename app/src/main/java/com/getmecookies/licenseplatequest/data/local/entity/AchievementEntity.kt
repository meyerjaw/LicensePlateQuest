package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** An earned achievement (id from the domain `Achievement` catalog) and when it was unlocked. */
@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "earned_at") val earnedAt: Instant,
)
