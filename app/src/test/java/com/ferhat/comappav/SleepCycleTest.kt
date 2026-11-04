package com.ferhat.comappav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepCycleTest {
    private val clock: (Long) -> String = { "T$it" }

    private fun status(state: String, nextWakeAt: Long? = null, hold: String? = null) =
        SleepCycleStatus(state, 20, nextWakeAt, hold, 0L)

    @Test
    fun intervalsAreOnlyTheOfferedOnes() {
        assertEquals(20, SleepCycle.parseInterval(20L))
        assertEquals(30, SleepCycle.parseInterval(30.0))
        assertNull(SleepCycle.parseInterval(15L))
        assertNull(SleepCycle.parseInterval("20"))
        assertNull(SleepCycle.parseInterval(null))
    }

    @Test
    fun summaries() {
        assertNull(SleepCycle.summary(null, 0, clock))
        assertEquals("Server computer: always on (Performance)", SleepCycle.summary(status("OFF"), 0, clock))
        assertEquals(
            "Server computer: asleep · next wake T1000",
            SleepCycle.summary(status("SLEEPING", nextWakeAt = 1000), 500, clock),
        )
        assertEquals(
            "Server computer: asleep · should have woken at T1000",
            SleepCycle.summary(status("SLEEPING", nextWakeAt = 1000), 1000 + SleepCycle.OVERDUE_GRACE_MILLIS + 1, clock),
        )
        assertEquals(
            "Server computer: awake · someone is logged in (SSH or console)",
            SleepCycle.summary(status("AWAKE", hold = "SESSION"), 0, clock),
        )
        assertEquals("Server computer: awake · sleeps again when idle", SleepCycle.summary(status("AWAKE"), 0, clock))
    }

    @Test
    fun logLinesSayWhatHoldsItAndOnlyOnChanges() {
        val ssh = SleepCycleStatus("AWAKE", 10, null, "SESSION", 1L, "session 4: ferhat via sshd on pts/0")
        assertEquals(
            "Server computer stays awake: someone is logged in (SSH or console) - session 4: ferhat via sshd on pts/0",
            SleepCycle.logLine(null, ssh, 0, clock),
        )
        assertNull(SleepCycle.logLine(ssh, ssh.copy(updatedAt = 2L), 0, clock))
        assertEquals(
            "Server computer went to sleep; next wake T5000 (every 10 min)",
            SleepCycle.logLine(ssh, SleepCycleStatus("SLEEPING", 10, 5000L, null, 3L), 0, clock),
        )
        assertEquals(
            "Server computer is awake, nothing holds it; it sleeps after 2 min without commands",
            SleepCycle.logLine(ssh, status("AWAKE"), 0, clock),
        )
        assertEquals(
            "Server computer: awake · someone is logged in (SSH or console) (session 4: ferhat via sshd on pts/0)",
            SleepCycle.summary(ssh, 0, clock),
        )
    }

    @Test
    fun queuedLineOnlyWhileAsleep() {
        assertNull(SleepCycle.queuedLine(status("AWAKE"), 0, clock))
        assertNull(SleepCycle.queuedLine(status("OFF"), 0, clock))
        assertEquals(
            "Queued - it runs at about T1000, when the server computer wakes up.",
            SleepCycle.queuedLine(status("SLEEPING", nextWakeAt = 1000), 0, clock),
        )
    }

    @Test
    fun runningAndSleeping() {
        assertTrue(status("AWAKE").running)
        assertFalse(status("OFF").running)
        assertTrue(status("SLEEPING").sleeping)
        assertFalse(status("AWAKE").sleeping)
    }
}
