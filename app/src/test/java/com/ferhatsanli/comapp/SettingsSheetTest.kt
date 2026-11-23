package com.ferhatsanli.comapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsSheetTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rowsStepperAndImportWork() {
        var rows by mutableIntStateOf(2)
        lateinit var sheet: SettingsSheetState
        var developer by mutableStateOf(false)
        var imported: Map<String, String>? = null
        rule.setContent {
            sheet = rememberSettingsSheetState(initiallyOpen = true)
            SettingsSheet(
                state = sheet,
                model = SettingsModel(
                    favoriteRows = rows,
                    onFavoriteRows = { rows = FavoriteApps.clampRows(it) },
                    deviceNameCount = 0,
                    onImportNames = { names, done ->
                        imported = names
                        done("Saved ${names.size} names.")
                    },
                    appVersion = "v2.9.0 (39)",
                    computerName = "Ferhat-MacBook",
                    serverVersion = "2.6.1",
                    developerMode = developer,
                    onDeveloperMode = { developer = it },
                ),
            )
        }
        rule.onNodeWithText("Settings").assertExists()
        rule.onNodeWithContentDescription("One row more").performClick()
        assertEquals(3, rows)
        rule.onNodeWithText("12 favorites fit (4 per row).").assertExists()

        rule.onNodeWithText("Nothing to import yet").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("\"AA:BB:CC:DD:EE:FF\": \"Living Room TV\"").performScrollTo().performTextInput(
            "\"48:D3:43:4A:1D:60\": \"JVD37-Modem\",\n\"B8:27:EB:75:27:A0\": \"RPi\",",
        )
        rule.onNodeWithText("Import 2 names").performScrollTo().assertIsEnabled().performClick()
        assertEquals(mapOf("48-D3-43-4A-1D-60" to "JVD37-Modem", "B8-27-EB-75-27-A0" to "RPi"), imported)
        rule.onNodeWithText("Saved 2 names.").assertExists()

        rule.onNodeWithText("Developer Mode").performScrollTo()
        rule.onNode(isToggleable()).assertIsOff().performClick()
        assertEquals(true, developer)
        rule.onNode(isToggleable()).assertIsOn()

        rule.onNodeWithContentDescription("Close settings").performClick()
        rule.mainClock.advanceTimeBy(1500)
        rule.onNodeWithText("Settings").assertDoesNotExist()
        assertEquals(0f, sheet.progress)
    }

    @Test
    fun theSheetFollowsAFingerAndSettlesByPosition() {
        lateinit var sheet: SettingsSheetState
        rule.setContent {
            sheet = rememberSettingsSheetState()
            SettingsSheet(state = sheet, model = model())
        }
        rule.onNodeWithText("Settings").assertDoesNotExist()
        // A third of the way up and let go slowly: it falls back closed.
        rule.runOnIdle {
            sheet.dragBy(-sheet.heightPx / 3)
            assertEquals(1f / 3, sheet.progress, 0.01f)
            assertTrue(sheet.visible)
            sheet.release(0f)
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.runOnIdle { assertEquals(0f, sheet.progress) }
        // Two thirds and let go: it opens, and the title is drawn.
        rule.runOnIdle {
            sheet.dragBy(-sheet.heightPx * 2 / 3)
            sheet.release(0f)
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.runOnIdle { assertEquals(1f, sheet.progress) }
        rule.onNodeWithText("Settings").assertExists()
        // A real swipe down on the title closes it.
        rule.onNodeWithText("Settings").performTouchInput { swipeDown(startY = centerY, endY = centerY + 1500f) }
        rule.mainClock.advanceTimeBy(1500)
        rule.runOnIdle { assertEquals(0f, sheet.progress) }
        rule.onNodeWithText("Settings").assertDoesNotExist()
    }

    private fun model() = SettingsModel(
        favoriteRows = 2,
        onFavoriteRows = {},
        deviceNameCount = 0,
        onImportNames = { _, _ -> },
        appVersion = "v2.10.2 (42)",
        computerName = "Ferhat-MacBook",
        serverVersion = "2.7.0",
    )
}
