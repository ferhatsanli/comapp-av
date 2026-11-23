package com.ferhatsanli.comapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
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
        var open by mutableStateOf(true)
        var imported: Map<String, String>? = null
        rule.setContent {
            SettingsSheet(
                visible = open,
                onClose = { open = false },
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

        rule.onNodeWithContentDescription("Close settings").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Settings").assertDoesNotExist()
    }
}
