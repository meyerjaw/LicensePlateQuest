package com.getmecookies.licenseplatequest.ui.screens.passport

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import com.getmecookies.licenseplatequest.R
import com.getmecookies.licenseplatequest.domain.Achievement

/** Presentation for one achievement: its title, description, and icon. */
data class AchievementMeta(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val icon: ImageVector,
)

/** Display order + presentation for the catalog (kept in the UI layer so the domain stays pure). */
fun achievementMeta(achievement: Achievement): AchievementMeta = when (achievement) {
    Achievement.FIRST_PLATE -> AchievementMeta(
        R.string.ach_first_plate_title,
        R.string.ach_first_plate_desc,
        Icons.Filled.Flag
    )

    Achievement.COLLECT_10 -> AchievementMeta(
        R.string.ach_collect_10_title,
        R.string.ach_collect_10_desc,
        Icons.Filled.Explore
    )

    Achievement.COLLECT_25 -> AchievementMeta(
        R.string.ach_collect_25_title,
        R.string.ach_collect_25_desc,
        Icons.Filled.Map
    )

    Achievement.COLLECT_40 -> AchievementMeta(
        R.string.ach_collect_40_title,
        R.string.ach_collect_40_desc,
        Icons.Filled.Public
    )

    Achievement.COLLECT_50 -> AchievementMeta(
        R.string.ach_collect_50_title,
        R.string.ach_collect_50_desc,
        Icons.Filled.EmojiEvents
    )

    Achievement.FIRST_TRIP -> AchievementMeta(
        R.string.ach_first_trip_title,
        R.string.ach_first_trip_desc,
        Icons.Filled.DirectionsCar
    )

    Achievement.FIFTY_ON_ONE_TRIP -> AchievementMeta(
        R.string.ach_fifty_one_trip_title,
        R.string.ach_fifty_one_trip_desc,
        Icons.Filled.Star
    )

    Achievement.TEN_IN_A_DAY -> AchievementMeta(
        R.string.ach_ten_in_a_day_title,
        R.string.ach_ten_in_a_day_desc,
        Icons.Filled.Bolt
    )

    Achievement.RARE_CATCH -> AchievementMeta(
        R.string.ach_rare_catch_title,
        R.string.ach_rare_catch_desc,
        Icons.Filled.AutoAwesome
    )

    Achievement.TREASURE_HUNTER -> AchievementMeta(
        R.string.ach_treasure_hunter_title,
        R.string.ach_treasure_hunter_desc,
        Icons.Filled.Stars
    )

    Achievement.NEW_ENGLAND -> AchievementMeta(
        R.string.ach_new_england_title,
        R.string.ach_new_england_desc,
        Icons.Filled.Place
    )

    Achievement.WEST_COAST -> AchievementMeta(
        R.string.ach_west_coast_title,
        R.string.ach_west_coast_desc,
        Icons.Filled.Waves
    )

    Achievement.FOUR_CORNERS -> AchievementMeta(
        R.string.ach_four_corners_title,
        R.string.ach_four_corners_desc,
        Icons.Filled.Terrain
    )

    Achievement.GREAT_LAKES -> AchievementMeta(
        R.string.ach_great_lakes_title,
        R.string.ach_great_lakes_desc,
        Icons.Filled.Water
    )

    Achievement.DEEP_SOUTH -> AchievementMeta(
        R.string.ach_deep_south_title,
        R.string.ach_deep_south_desc,
        Icons.Filled.Spa
    )

    Achievement.MOUNTAIN_WEST -> AchievementMeta(
        R.string.ach_mountain_west_title,
        R.string.ach_mountain_west_desc,
        Icons.Filled.Landscape
    )

    Achievement.GOOD_NEIGHBORS -> AchievementMeta(
        R.string.ach_good_neighbors_title,
        R.string.ach_good_neighbors_desc,
        Icons.Filled.People
    )

    Achievement.COAST_TO_COAST -> AchievementMeta(
        R.string.ach_coast_to_coast_title,
        R.string.ach_coast_to_coast_desc,
        Icons.Filled.SwapHoriz
    )

    Achievement.TEAM_EFFORT -> AchievementMeta(
        R.string.ach_team_effort_title,
        R.string.ach_team_effort_desc,
        Icons.Filled.Groups
    )

    Achievement.EARLY_BIRD -> AchievementMeta(
        R.string.ach_early_bird_title,
        R.string.ach_early_bird_desc,
        Icons.Filled.WbSunny
    )

    Achievement.NIGHT_OWL -> AchievementMeta(
        R.string.ach_night_owl_title,
        R.string.ach_night_owl_desc,
        Icons.Filled.DarkMode
    )

    Achievement.WEEKEND_WARRIOR -> AchievementMeta(
        R.string.ach_weekend_warrior_title,
        R.string.ach_weekend_warrior_desc,
        Icons.Filled.Weekend
    )
}
