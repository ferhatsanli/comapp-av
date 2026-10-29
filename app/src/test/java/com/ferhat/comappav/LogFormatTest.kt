package com.ferhat.comappav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class LogFormatTest {
    private val sample = LogEntry(
        timeMillis = 1_793_000_000_000,
        level = LogLevel.ERROR,
        tag = "Command",
        message = "POWER_MODE failed: AGENT_ERROR",
    )

    @Test
    fun aPlainEntrySurvivesARoundTrip() {
        assertEquals(sample, LogFormat.decode(LogFormat.encode(sample)))
    }

    @Test
    fun aMultiLineStackTraceStaysOneLineOnDiskAndComesBackIntact() {
        val crash = sample.copy(
            tag = "Crash",
            message = "java.lang.IllegalStateException: boom\n\tat a.b.C.d(C.kt:12)\n\tat x.y(Z.kt:3)",
        )
        val line = LogFormat.encode(crash)
        assertTrue("encoded entry must be a single line", '\n' !in line && '\r' !in line)
        assertEquals(crash, LogFormat.decode(line))
    }

    @Test
    fun backslashesAndTabsInsideFieldsDoNotBreakTheFormat() {
        val tricky = sample.copy(tag = "a\tb", message = "path C:\\temp\\n is not a newline\\")
        assertEquals(tricky, LogFormat.decode(LogFormat.encode(tricky)))
    }

    @Test
    fun corruptOrForeignLinesAreSkippedInsteadOfCrashing() {
        assertNull(LogFormat.decode(""))
        assertNull(LogFormat.decode("not a log line"))
        assertNull(LogFormat.decode("abc\tERROR\ttag\tmsg"))
        assertNull(LogFormat.decode("1\tLOUD\ttag\tmsg"))
    }

    @Test
    fun theClipboardFormIsReadable() {
        assertEquals(
            "2026-10-26 07:33:20 ERROR [Command] POWER_MODE failed: AGENT_ERROR",
            LogFormat.format(sample, ZoneOffset.UTC),
        )
    }
}
