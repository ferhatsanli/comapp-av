package com.ferhatsanli.comapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.ferhatsanli.comapp.ui.glass.CommandLight
import com.ferhatsanli.comapp.ui.glass.CommandStatusLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CommandLightTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theLightFollowsTheCommand() {
        var light = CommandLight.OFF
        light = CommandLightRules.next(light, pending = true, status = null)
        assertEquals(CommandLight.PENDING, light)
        light = CommandLightRules.next(light, pending = false, status = "SUCCESS")
        assertEquals(CommandLight.SUCCESS, light)
        // A later status change while green does not restart anything.
        assertEquals(CommandLight.SUCCESS, CommandLightRules.next(light, pending = false, status = "SUCCESS"))
        assertEquals(CommandLight.FAILED, CommandLightRules.next(CommandLight.PENDING, false, "FAILED"))
        // A send that never reached Firebase (no status at all) is a failure too.
        assertEquals(CommandLight.FAILED, CommandLightRules.next(CommandLight.PENDING, false, null))
        assertEquals(CommandLight.OFF, CommandLightRules.next(CommandLight.OFF, false, "SUCCESS"))
    }

    /** Draws the light on black, 300 dp wide, and counts pixels of its color on the middle row. */
    private fun drawn(light: CommandLight, matches: (Int, Int, Int) -> Boolean): Triple<Int, Boolean, Boolean> {
        rule.setContent {
            Box(Modifier.width(300.dp).height(10.dp).background(Color.Black).testTag("light")) {
                CommandStatusLight(light)
            }
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val bitmap = rule.onNodeWithTag("light").captureToImage().asAndroidBitmap()
        val y = bitmap.height / 2
        fun hit(x: Int): Boolean {
            val pixel = bitmap.getPixel(x, y)
            return matches((pixel shr 16) and 0xFF, (pixel shr 8) and 0xFF, pixel and 0xFF)
        }
        val count = (0 until bitmap.width).count { hit(it) }
        return Triple(count, hit(bitmap.width / 20), hit(bitmap.width * 19 / 20))
    }

    @Test
    fun pendingIsAnOrangeLineAcrossTheWholeWidth() {
        val (count, left, right) = drawn(CommandLight.PENDING) { r, g, b -> r > 150 && g in 60..220 && b < 140 }
        assertTrue("only $count orange pixels", count > 250)
        assertTrue("no light at the left end", left)
        assertTrue("no light at the right end", right)
    }

    @Test
    fun successIsGreenAndFailureIsRed() {
        val (green, _, _) = drawn(CommandLight.SUCCESS) { r, g, b -> g > 120 && g > r + 30 && g > b }
        assertTrue("only $green green pixels", green > 250)
    }

    @Test
    fun failureIsRed() {
        val (red, _, _) = drawn(CommandLight.FAILED) { r, g, b -> r > 150 && g < 140 && b < 140 }
        assertTrue("only $red red pixels", red > 250)
    }

    @Test
    fun offDrawsNothing() {
        val (lit, _, _) = drawn(CommandLight.OFF) { r, g, b -> r + g + b > 60 }
        assertEquals(0, lit)
    }
}
