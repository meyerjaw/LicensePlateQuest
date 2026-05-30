package com.getmecookies.licenseplatequest.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A kind of road-trip game (SPEC §7). MVP seeds exactly one: `license_plate`. The table
 * exists so future games (slug bug, alphabet, trivia) are just new rows + game-specific UI,
 * with no trip refactor.
 */
@Entity(
    tableName = "game_type",
    indices = [Index(value = ["code"], unique = true)],
)
data class GameTypeEntity(
    @PrimaryKey val id: UUID,
    val code: String,
    val name: String,
    val description: String,
)
