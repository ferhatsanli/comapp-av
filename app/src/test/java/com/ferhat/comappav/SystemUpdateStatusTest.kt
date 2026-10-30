package com.ferhat.comappav

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
}
