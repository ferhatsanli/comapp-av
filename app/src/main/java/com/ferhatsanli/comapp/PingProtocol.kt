package com.ferhatsanli.comapp

import com.google.firebase.database.ServerValue

internal object PingProtocol {
    const val ACTION = "PING"
    const val SPEAK_ACTION = "SPEAK"
    const val SLEEP_ACTION = "SLEEP"
    const val SCAN_ACTION = "SCAN"
    const val SHUTDOWN_ACTION = "SHUTDOWN"
    const val POWER_MODE_ACTION = "POWER_MODE"
    const val SYSTEM_INFO_ACTION = "SYSTEM_INFO"
    const val TAILSCALE_ACTION = "TAILSCALE"
    const val SYSTEM_UPDATE_ACTION = "SYSTEM_UPDATE"
    const val MEDIA_ACTION = "MEDIA"
    const val MEDIA_PREVIOUS = "PREVIOUS"
    const val MEDIA_PLAY_PAUSE = "PLAY_PAUSE"
    const val MEDIA_NEXT = "NEXT"
    const val MEDIA_VOLUME = "VOLUME"
    const val MODE_PERFORMANCE = "PERFORMANCE"
    const val MODE_POWERSAVE = "POWERSAVE"
    const val MAX_SLEEP_MINUTES = 1440
    const val MAX_SPEAK_LENGTH = 300
    const val LANGUAGE_TR = "tr"
    const val LANGUAGE_EN = "en"

    fun pendingCommand(): Map<String, String> = mapOf(
        "action" to ACTION,
        "status" to "PENDING",
    )

    fun scanCommand(): Map<String, String> = mapOf(
        "action" to SCAN_ACTION,
        "status" to "PENDING",
    )

    /** [language] is [LANGUAGE_TR] or [LANGUAGE_EN]; the agent picks the matching voice. */
    fun speakCommand(text: String, language: String = LANGUAGE_TR): Map<String, String> = mapOf(
        "action" to SPEAK_ACTION,
        "status" to "PENDING",
        "text" to text.take(MAX_SPEAK_LENGTH),
        "language" to if (language == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_TR,
    )

    /** Without [durationMinutes] the laptop sleeps until the daily morning wake-up. */
    fun sleepCommand(durationMinutes: Int? = null): Map<String, Any> {
        val command = mutableMapOf<String, Any>(
            "action" to SLEEP_ACTION,
            "status" to "PENDING",
            "createdAt" to ServerValue.TIMESTAMP,
        )
        if (durationMinutes != null) {
            command["durationMinutes"] = durationMinutes.coerceIn(1, MAX_SLEEP_MINUTES)
        }
        return command
    }

    /** Powers the laptop off immediately - no grace delay, and no automatic wake-up. */
    fun shutdownCommand(): Map<String, Any> = mapOf(
        "action" to SHUTDOWN_ACTION,
        "status" to "PENDING",
        "createdAt" to ServerValue.TIMESTAMP,
    )

    /** Turns Tailscale on the server computer on ([on]) or off. */
    fun tailscaleCommand(on: Boolean): Map<String, String> = mapOf(
        "action" to TAILSCALE_ACTION,
        "status" to "PENDING",
        "state" to if (on) "UP" else "DOWN",
    )

    /** Starts installing every available update on the server computer (dnf upgrade). */
    fun systemUpdateCommand(): Map<String, String> = mapOf(
        "action" to SYSTEM_UPDATE_ACTION,
        "status" to "PENDING",
    )

    /**
     * A media key ([MEDIA_PREVIOUS], [MEDIA_PLAY_PAUSE], [MEDIA_NEXT]), or [MEDIA_VOLUME] with
     * [volume] 0-100 (clamped). The Rules want volume exactly on a VOLUME command.
     */
    fun mediaCommand(kind: String, volume: Int? = null): Map<String, Any> = buildMap {
        put("action", MEDIA_ACTION)
        put("status", "PENDING")
        put("media", kind)
        if (kind == MEDIA_VOLUME) put("volume", (volume ?: 0).coerceIn(0, 100))
    }

    /** Error codes shared by several commands, in words; null for ones that need no explanation. */
    fun describeError(code: String?, action: String? = null): String? = when (code) {
        "NOT_PERMITTED" -> if (action == MEDIA_ACTION) {
            "The computer hasn't allowed the agent to press media keys yet. On the Mac, open System Settings > " +
                "Privacy & Security > Accessibility, turn on Python, then try again."
        } else {
            "The server computer isn't set up for this yet - run install-remote-admin.sh on it once."
        }
        "UPDATE_RUNNING" -> "A system update is running on the server computer. Try again when it has finished."
        "SLEEP_CYCLE_ACTIVE" -> "Power save already puts the server computer to sleep between checks. Switch to Performance to use Sleep."
        else -> null
    }

    /** "Tailscale" line of System Info for a /power/latest tailscale value. */
    fun tailscaleLabel(state: String?): String? = when (state) {
        "RUNNING" -> "Connected"
        "STOPPED" -> "Off"
        "STARTING" -> "Starting"
        "NEEDS_LOGIN" -> "Needs login"
        "DAEMON_OFF" -> "Off (service stopped)"
        "NOT_INSTALLED" -> "Not installed"
        "UNKNOWN" -> "Unknown"
        else -> null
    }

    /** Asks the agent to measure and publish /power/latest right now. */
    fun systemInfoCommand(): Map<String, String> = mapOf(
        "action" to SYSTEM_INFO_ACTION,
        "status" to "PENDING",
    )

    /** [mode] must be [MODE_PERFORMANCE] or [MODE_POWERSAVE]. */
    fun powerModeCommand(mode: String): Map<String, String> = mapOf(
        "action" to POWER_MODE_ACTION,
        "status" to "PENDING",
        "mode" to mode,
    )

    /** 80 -> "1 h 20 min", 120 -> "2 h", 2 -> "2 min". */
    fun formatDuration(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "$hours h $minutes min"
            hours > 0 -> "$hours h"
            else -> "$minutes min"
        }
    }
}
