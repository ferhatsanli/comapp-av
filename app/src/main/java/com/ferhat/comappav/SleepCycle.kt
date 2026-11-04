package com.ferhat.comappav

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * /power/cycle, written by the agent's sleep_cycle.py: in power save the server computer
 * sleeps and wakes every [intervalMinutes] to run what the phone queued; in performance
 * ([STATE_OFF]) it stays on.
 */
internal data class SleepCycleStatus(
    val state: String,
    val intervalMinutes: Int,
    val nextWakeAt: Long?,
    val hold: String?,
    val updatedAt: Long?,
) {
    val sleeping: Boolean get() = state == SleepCycle.STATE_SLEEPING
    val running: Boolean get() = state != SleepCycle.STATE_OFF
}

internal object SleepCycle {
    const val STATE_OFF = "OFF"
    const val STATE_AWAKE = "AWAKE"
    const val STATE_SLEEPING = "SLEEPING"
    const val SETTINGS_PATH = "settings/sleepIntervalMinutes"
    const val STATUS_PATH = "power/cycle"
    val INTERVALS = listOf(10, 20, 30, 60)
    const val DEFAULT_INTERVAL = 20

    /** After the planned wake-up plus this, "asleep" turns into "should be awake by now". */
    const val OVERDUE_GRACE_MILLIS = 5 * 60_000L

    fun parseInterval(value: Any?): Int? {
        val minutes = (value as? Number)?.toDouble() ?: return null
        return INTERVALS.firstOrNull { it.toDouble() == minutes }
    }

    fun overdue(status: SleepCycleStatus, nowMs: Long): Boolean {
        val wake = status.nextWakeAt ?: return false
        return status.sleeping && nowMs > wake + OVERDUE_GRACE_MILLIS
    }

    /** "03:40" in the phone's time zone. */
    fun clock(epochMs: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("HH:mm", locale).format(Date(epochMs))

    fun holdLabel(hold: String?): String? = when (hold) {
        "NETWORK" -> "waiting for the network"
        "DOWNLOAD" -> "a download is running"
        "UPDATE" -> "a system update is running"
        "TASKS" -> "Server tasks is running"
        "SESSION" -> "someone is logged in (SSH or console)"
        "ALARM_FAILED" -> "the wake alarm couldn't be set, so it stays on"
        "SUSPEND_FAILED" -> "it couldn't go to sleep, trying again soon"
        else -> null
    }

    /** One line for the connection card, e.g. "Server computer: asleep · next wake 03:40". */
    fun summary(status: SleepCycleStatus?, nowMs: Long, clock: (Long) -> String = { clock(it) }): String? {
        if (status == null) return null
        return when (status.state) {
            STATE_OFF -> "Server computer: always on (Performance)"
            STATE_SLEEPING -> {
                val wake = status.nextWakeAt
                when {
                    wake == null -> "Server computer: asleep"
                    overdue(status, nowMs) -> "Server computer: asleep · should have woken at ${clock(wake)}"
                    else -> "Server computer: asleep · next wake ${clock(wake)}"
                }
            }
            STATE_AWAKE -> holdLabel(status.hold)?.let { "Server computer: awake · $it" }
                ?: "Server computer: awake · sleeps again when idle"
            else -> null
        }
    }

    /** What a PENDING command waits for while the server computer sleeps. */
    fun queuedLine(status: SleepCycleStatus?, nowMs: Long, clock: (Long) -> String = { clock(it) }): String? {
        if (status == null || !status.sleeping) return null
        val wake = status.nextWakeAt ?: return "Queued - it runs when the server computer wakes up."
        return if (overdue(status, nowMs)) {
            "Queued - the server computer should have woken at ${clock(wake)} and hasn't answered yet."
        } else {
            "Queued - it runs at about ${clock(wake)}, when the server computer wakes up."
        }
    }
}
