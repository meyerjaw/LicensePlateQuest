package com.getmecookies.licenseplatequest.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.getmecookies.licenseplatequest.domain.model.RegionOption
import com.getmecookies.licenseplatequest.ui.theme.LicensePlateQuestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Compose UI tests for the shared region picker sheet (search, exclude, selection). */
@RunWith(AndroidJUnit4::class)
class RegionPickerSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val texas = RegionOption(UUID.randomUUID(), "TX", "Texas")
    private val colorado = RegionOption(UUID.randomUUID(), "CO", "Colorado")
    private val options = listOf(texas, colorado)

    @Test
    fun showsAllOptions() {
        setSheet()
        composeTestRule.onNodeWithText("Texas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Colorado").assertIsDisplayed()
    }

    @Test
    fun searchFiltersByName() {
        setSheet()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("colo")
        composeTestRule.onNodeWithText("Colorado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Texas").assertDoesNotExist()
    }

    @Test
    fun excludeIdHidesThatOption() {
        setSheet(excludeId = texas.id)
        composeTestRule.onNodeWithText("Texas").assertDoesNotExist()
        composeTestRule.onNodeWithText("Colorado").assertIsDisplayed()
    }

    @Test
    fun tappingRowReportsSelection() {
        var picked: UUID? = null
        setSheet(onSelected = { picked = it })
        composeTestRule.onNodeWithText("Colorado").performClick()
        composeTestRule.runOnIdle { assertEquals(colorado.id, picked) }
    }

    private fun setSheet(
        excludeId: UUID? = null,
        onSelected: (UUID) -> Unit = {},
    ) {
        composeTestRule.setContent {
            LicensePlateQuestTheme {
                RegionPickerSheet(
                    options = options,
                    selectedId = null,
                    onSelected = onSelected,
                    onDismiss = {},
                    excludeId = excludeId,
                )
            }
        }
    }
}
