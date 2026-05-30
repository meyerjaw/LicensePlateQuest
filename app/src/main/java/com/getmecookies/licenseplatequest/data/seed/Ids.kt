package com.getmecookies.licenseplatequest.data.seed

import java.util.UUID

/**
 * Deterministic UUIDs for bundled reference data so re-seeding (on a content update) updates
 * the *same* rows rather than creating duplicates — and so foreign keys from trips into
 * regions stay valid across updates. User-generated entities (trips, players, spottings)
 * still use random [UUID.randomUUID].
 */
object Ids {
    fun region(countryCode: String, regionCode: String): UUID =
        UUID.nameUUIDFromBytes("region:$countryCode-$regionCode".toByteArray())

    fun gameType(code: String): UUID =
        UUID.nameUUIDFromBytes("game_type:$code".toByteArray())
}
