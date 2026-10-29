package com.ferhat.comappav

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Plain Kotlin with no Android dependency on purpose, so it can be unit-tested on the JVM.

enum class LogLevel { INFO, WARN, ERROR }

data class LogEntry(
    val timeMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

/**
 * How [LogEntry] values are stored on disk and copied to the clipboard.
 *
 * On disk: one entry per line, four tab-separated fields (time, level, tag, message). Backslash,
 * tab, CR and LF inside a field are escaped, so a multi-line stack trace still takes exactly one
 * line and comes back unchanged. Lines that don't parse are skipped, never fatal.
 */
internal object LogFormat {
    private val clipboardTime: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val screenTime: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun encode(entry: LogEntry): String =
        listOf(entry.timeMillis.toString(), entry.level.name, escape(entry.tag), escape(entry.message))
            .joinToString("\t")

    fun decode(line: String): LogEntry? {
        val parts = line.split('\t')
        if (parts.size != 4) return null
        val time = parts[0].toLongOrNull() ?: return null
        val level = LogLevel.entries.firstOrNull { it.name == parts[1] } ?: return null
        return LogEntry(time, level, unescape(parts[2]), unescape(parts[3]))
    }

    /** One readable line per entry, e.g. "2026-10-30 00:42:13 ERROR [Command] ...". */
    fun format(entry: LogEntry, zone: ZoneId = ZoneId.systemDefault()): String =
        "${clipboardTime.format(Instant.ofEpochMilli(entry.timeMillis).atZone(zone))} " +
            "${entry.level.name} [${entry.tag}] ${entry.message}"

    /** Just the clock time, for the compact on-screen list. */
    fun timeOfDay(entry: LogEntry, zone: ZoneId = ZoneId.systemDefault()): String =
        screenTime.format(Instant.ofEpochMilli(entry.timeMillis).atZone(zone))

    private fun escape(value: String): String = buildString(value.length) {
        for (c in value) {
            when (c) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(c)
            }
        }
    }

    private fun unescape(value: String): String = buildString(value.length) {
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (val next = value[i + 1]) {
                    'n' -> append('\n')
                    't' -> append('\t')
                    'r' -> append('\r')
                    '\\' -> append('\\')
                    else -> {
                        append(c)
                        append(next)
                    }
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}
