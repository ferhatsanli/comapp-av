package com.ferhat.comappav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentCommandsTest {
    @Test
    fun pushIdsCarryTheirCreationTime() {
        // "-Nfkd3RF..." was created at 1696254412560 ms (2023-10-02).
        assertEquals(1_696_254_412_560L, RecentCommands.pushIdTimeMs("-Nfkd3RFAbCdEfGhIjKl"))
        assertNull(RecentCommands.pushIdTimeMs("short"))
        assertNull(RecentCommands.pushIdTimeMs("not*a*push*id"))
    }

    @Test
    fun detailsAreReadableAndInOrder() {
        assertEquals(
            listOf("Text" to "Hello", "Language" to "English"),
            RecentCommands.details(mapOf("language" to "en", "text" to "Hello", "status" to "SUCCESS")),
        )
        assertEquals(listOf("Turn" to "Off"), RecentCommands.details(mapOf("state" to "DOWN")))
        assertEquals(listOf("Duration" to "1 h 20 min"), RecentCommands.details(mapOf("durationMinutes" to 80L)))
        assertEquals(emptyList<Pair<String, String>>(), RecentCommands.details(mapOf("text" to " ")))
    }

    @Test
    fun outcomes() {
        fun command(status: String, result: String? = null, error: String? = null) =
            RecentCommand("id", "PING", status, result, error, null, emptyList())
        assertEquals("Done (PONG)", RecentCommands.outcome(command("SUCCESS", result = "PONG")))
        assertEquals("Failed (AGENT_ERROR)", RecentCommands.outcome(command("FAILED", error = "AGENT_ERROR")))
        assertEquals("Waiting for the agent", RecentCommands.outcome(command("PENDING")))
    }
}
