package com.getmecookies.licenseplatequest.data.local

import androidx.room.TypeConverter
import com.getmecookies.licenseplatequest.domain.model.TripStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Room type converters for the value types used across entities.
 *
 * All timestamps are stored as ISO-8601 UTC strings ([Instant.toString]); dates as
 * ISO-8601 local dates. UUIDs are stored as their canonical string form. JSON-bearing
 * columns (fun_facts, additional_info, payload) are stored as raw JSON strings and are
 * therefore plain [String] columns needing no converter here.
 */
class Converters {

    @TypeConverter
    fun uuidToString(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun stringToUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun instantToString(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun tripStatusToString(value: TripStatus?): String? = value?.wire

    @TypeConverter
    fun stringToTripStatus(value: String?): TripStatus? = value?.let(TripStatus::fromWire)
}
