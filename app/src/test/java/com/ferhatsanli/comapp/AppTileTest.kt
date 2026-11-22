package com.ferhatsanli.comapp

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppTileTest {
    @get:Rule
    val rule = createComposeRule()

    private val safari = ServerApp("com_apple_Safari", "com.apple.Safari", "Safari", null)

    @Test
    fun editingShowsTheRedCrossAndOnlyTheCrossDoesAnything() {
        var editing by mutableStateOf(false)
        var opened = 0
        var removed = 0
        rule.setContent {
            AppTile(
                app = safari,
                enabled = true,
                working = false,
                onClick = { opened++ },
                editing = editing,
                onRemove = { removed++ },
                modifier = Modifier.width(90.dp),
            )
        }
        rule.onNodeWithContentDescription("Remove Safari from Recent").assertDoesNotExist()
        rule.onNodeWithText("Safari").performClick()
        assertEquals(1, opened)

        editing = true
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithContentDescription("Remove Safari from Recent", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Safari").performClick()
        assertEquals("a tap on the app doesn't open it while editing", 1, opened)
        rule.onNodeWithContentDescription("Remove Safari from Recent", useUnmergedTree = true).performClick()
        assertEquals(1, removed)

        editing = false
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithContentDescription("Remove Safari from Recent", useUnmergedTree = true).assertDoesNotExist()
    }
}
