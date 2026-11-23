package com.ferhatsanli.comapp

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemUpdateStatusTest {
    private val res get() = RuntimeEnvironment.getApplication().resources
    private fun status(
        state: String,
        phase: String,
        percent: Int = 0,
        done: Int = 0,
        total: Int = 0,
        packages: Int = 0,
        error: String? = null,
        reboot: Boolean? = null,
    ) = SystemUpdateStatus(state, phase, percent, done, total, packages, null, null, error, reboot)

    @Test
    fun runningShowsTheStepAndPercentage() {
        assertEquals("Checking for updates · 2%", status("RUNNING", "REFRESHING", percent = 2).describe(res))
        assertEquals("Downloading 12 of 37 · 18%", status("RUNNING", "DOWNLOADING", 18, 12, 37).describe(res))
        assertEquals("Installing 3 of 6 · 68%", status("RUNNING", "INSTALLING", 68, 3, 6).describe(res))
        assertEquals(0.68f, status("RUNNING", "INSTALLING", 68).fraction, 0.0001f)
    }

    @Test
    fun finishedRuns() {
        assertEquals("Already up to date.", status("DONE", "UP_TO_DATE", 100).describe(res))
        assertEquals("Updated 1 package.", status("DONE", "FINISHED", 100, packages = 1, reboot = false).describe(res))
        assertEquals(
            "Updated 37 packages. A restart is recommended.",
            status("DONE", "FINISHED", 100, packages = 37, reboot = true).describe(res),
        )
        assertEquals(1f, status("DONE", "FINISHED", 100).fraction, 0f)
    }

    @Test
    fun failures() {
        assertEquals("The update failed (DNF_EXIT_1).", status("FAILED", "FINISHED", 40, error = "DNF_EXIT_1").describe(res))
        assert(status("FAILED", "FINISHED", error = "INTERRUPTED").describe(res).contains("stopped before it finished"))
        assertEquals("The update failed.", status("FAILED", "FINISHED").describe(res))
    }

    @Test
    fun macosUpdatesStartWithTheAppStoreAndCountUpdates() {
        val running = SystemUpdateStatus("RUNNING", "APP_STORE", 12, 0, 0, 1, null, null, null, null, macos = true)
        assertEquals("Updating App Store apps · 12%", running.describe(res))
        val done = SystemUpdateStatus("DONE", "FINISHED", 100, 0, 0, 2, null, null, null, true, macos = true)
        assertEquals("Installed 2 updates. A restart is recommended.", done.describe(res))
    }

    @Test
    fun cancelledRunSaysSoAndIsNotAnError() {
        val cancelled = SystemUpdateStatus(
            "FAILED", "CANCELLED", 0, 0, 0, 0, null, null, "CANCELLED", null, macos = true,
        )
        assert(cancelled.cancelled)
        assertEquals("The update was cancelled. What finished before stays installed.", cancelled.describe(res))
    }

    @Test
    fun stallMinutesAppearAfterFiveSilentMinutesWhileRunning() {
        val start = 1_000_000L
        val running = SystemUpdateStatus(
            "RUNNING", "APP_STORE", 0, 0, 0, 0, start, null, null, null, macos = true,
            log = "== App Store ==\n\nWarning: x", outputAt = start + 60_000,
        )
        assertEquals(null, running.stallMinutes(start + 4 * 60_000))
        assertEquals(30L, running.stallMinutes(start + 31 * 60_000))
        assertEquals(listOf("== App Store ==", "Warning: x"), running.logLines)
        val noOutputYet = running.copy(outputAt = null)
        assertEquals(6L, noOutputYet.stallMinutes(start + 6 * 60_000))
        assertEquals(null, running.copy(state = "DONE").stallMinutes(start + 60 * 60_000))
    }

    @Test
    fun newLinesFollowTheSlidingTail() {
        assertEquals(listOf("a", "b"), SystemUpdateStatus.newLines(emptyList(), listOf("a", "b")))
        assertEquals(listOf("c", "d"), SystemUpdateStatus.newLines(listOf("a", "b"), listOf("a", "b", "c", "d")))
        assertEquals(listOf("d"), SystemUpdateStatus.newLines(listOf("a", "b", "c"), listOf("b", "c", "d")))
        assertEquals(emptyList<String>(), SystemUpdateStatus.newLines(listOf("a", "b"), listOf("a", "b")))
        assertEquals(listOf("x", "y"), SystemUpdateStatus.newLines(listOf("a", "b"), listOf("x", "y")))
    }

    @Test
    @Config(qualifiers = "tr")
    fun turkishTexts() {
        assertEquals("Kuruluyor: 3 / 6 · %68", status("RUNNING", "INSTALLING", 68, 3, 6).describe(res))
        assertEquals("37 paket güncellendi. Yeniden başlatma önerilir.", status("DONE", "FINISHED", 100, packages = 37, reboot = true).describe(res))
    }

    @Test
    @Config(qualifiers = "nl")
    fun dutchTexts() {
        assertEquals("Installeren 3 van 6 · 68%", status("RUNNING", "INSTALLING", 68, 3, 6).describe(res))
        assertEquals("1 pakket bijgewerkt.", status("DONE", "FINISHED", 100, packages = 1, reboot = false).describe(res))
    }
}
