package com.getmecookies.licenseplatequest.domain.model

import java.util.UUID

/**
 * UI-facing player model. Kept separate from the Room entity so screens don't depend on
 * persistence fields (created_at, soft-delete flag, etc.).
 */
data class Player(
    val id: UUID,
    val name: String,
)
