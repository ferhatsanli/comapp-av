package com.ferhatsanli.comapp

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUpdateStatusTest {
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
        assertEquals("Checking for updates · 2%", status("RUNNING", "REFRESHING", percent = 2).describe())
        assertEquals("Downloading 12 of 37 · 18%", status("RUNNING", "DOWNLOADING", 18, 12, 37).describe())
        assertEquals("Installing 3 of 6 · 68%", status("RUNNING", "INSTALLING", 68, 3, 6).describe())
        assertEquals(0.68f, status("RUNNING", "INSTALLING", 68).fraction, 0.0001f)
    }

    @Test
    fun finishedRuns() {
        assertEquals("Already up to date.", status("DONE", "UP_TO_DATE", 100).describe())
        assertEquals("Updated 1 package.", status("DONE", "FINISHED", 100, packages = 1, reboot = false).describe())
        assertEquals(
            "Updated 37 packages. A restart is recommended.",
            status("DONE", "FINISHED", 100, packages = 37, reboot = true).describe(),
        )
        assertEquals(1f, status("DONE", "FINISHED", 100).fraction, 0f)
    }

    @Test
    fun failures() {
        assertEquals("The update failed (DNF_EXIT_1).", status("FAILED", "FINISHED", 40, error = "DNF_EXIT_1").describe())
        assert(status("FAILED", "FINISHED", error = "INTERRUPTED").describe().contains("stopped before it finished"))
        assertEquals("The update failed.", status("FAILED", "FINISHED").describe())
    }

    @Test
    fun macosUpdatesStartWithTheAppStoreAndCountUpdates() {
        val running = SystemUpdateStatus("RUNNING", "APP_STORE", 12, 0, 0, 1, null, null, null, null, macos = true)
        assertEquals("Updating App Store apps · 12%", running.describe())
        val done = SystemUpdateStatus("DONE", "FINISHED", 100, 0, 0, 2, null, null, null, true, macos = true)
        assertEquals("Installed 2 updates. A restart is recommended.", done.describe())
    }

    @Test
    fun cancelledRunSaysSoAndIsNotAnError() {
        val cancelled = SystemUpdateStatus(
            "FAILED", "CANCELLED", 0, 0, 0, 0, null, null, "CANCELLED", null, macos = true,
        )
        assert(cancelled.cancelled)
        assertEquals("The update was cancelled. What finished before stays installed.", cancelled.describe())
    }

    @Test
    fun stallHintAppearsAfterFiveSilentMinutesWhileRunning() {
        val start = 1_000_000L
        val running = SystemUpdateStatus(
            "RUNNING", "APP_STORE", 0, 0, 0, 0, start, null, null, null, macos = true,
            log = "== App Store ==\n\nWarning: x", outputAt = start + 60_000,
        )
        assertEquals(null, running.stallHint(start + 4 * 60_000))
        assertEquals("No new output for 30 min", running.stallHint(start + 31 * 60_000))
        assertEquals(listOf("== App Store ==", "Warning: x"), running.logLines)
        val noOutputYet = running.copy(outputAt = null)
        assertEquals("No new output for 6 min", noOutputYet.stallHint(start + 6 * 60_000))
        assertEquals(null, running.copy(state = "DONE").stallHint(start + 60 * 60_000))
    }

    @Test
    fun newLinesFollowTheSlidingTail() {
        assertEquals(listOf("a", "b"), SystemUpdateStatus.newLines(emptyList(), listOf("a", "b")))
        assertEquals(listOf("c", "d"), SystemUpdateStatus.newLines(listOf("a", "b"), listOf("a", "b", "c", "d")))
        assertEquals(listOf("d"), SystemUpdateStatus.newLines(listOf("a", "b", "c"), listOf("b", "c", "d")))
        assertEquals(emptyList<String>(), SystemUpdateStatus.newLines(listOf("a", "b"), listOf("a", "b")))
        assertEquals(listOf("x", "y"), SystemUpdateStatus.newLines(listOf("a", "b"), listOf("x", "y")))
    }
}
