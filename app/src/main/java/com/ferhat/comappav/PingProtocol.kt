package com.ferhat.comappav

import com.google.firebase.database.ServerValue

internal object PingProtocol {
    const val ANDROID_UID = "57HVnDSvZKNbJBabrDF4c2gnv462"
    const val PI_UID = "N8V4nrLrELa83EXRLWJsiis85Bq1"
    const val ACTION = "PING"
    const val SPEAK_ACTION = "SPEAK"
    const val SLEEP_ACTION = "SLEEP"
    const val SCAN_ACTION = "SCAN"
    const val SHUTDOWN_ACTION = "SHUTDOWN"
    const val POWER_MODE_ACTION = "POWER_MODE"
    const val SYSTEM_INFO_ACTION = "SYSTEM_INFO"
    const val TAILSCALE_ACTION = "TAILSCALE"
    const val SYSTEM_UPDATE_ACTION = "SYSTEM_UPDATE"
    const val MODE_PERFORMANCE = "PERFORMANCE"
    const val MODE_POWERSAVE = "POWERSAVE"
    const val MAX_SLEEP_MINUTES = 1440
    const val MAX_SPEAK_LENGTH = 300
    const val LANGUAGE_TR = "tr"
    const val LANGUAGE_EN = "en"

    fun pendingCommand(senderUid: String): Map<String, String> = mapOf(
        "action" to ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
    )

    fun scanCommand(senderUid: String): Map<String, String> = mapOf(
        "action" to SCAN_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
    )

    /** [language] is [LANGUAGE_TR] or [LANGUAGE_EN]; the agent picks the matching voice. */
    fun speakCommand(senderUid: String, text: String, language: String = LANGUAGE_TR): Map<String, String> = mapOf(
        "action" to SPEAK_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
        "text" to text.take(MAX_SPEAK_LENGTH),
        "language" to if (language == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_TR,
    )

    /** Without [durationMinutes] the laptop sleeps until the daily morning wake-up. */
    fun sleepCommand(senderUid: String, durationMinutes: Int? = null): Map<String, Any> {
        val command = mutableMapOf<String, Any>(
            "action" to SLEEP_ACTION,
            "senderUid" to senderUid,
            "targetUid" to PI_UID,
            "status" to "PENDING",
            "createdAt" to ServerValue.TIMESTAMP,
        )
        if (durationMinutes != null) {
            command["durationMinutes"] = durationMinutes.coerceIn(1, MAX_SLEEP_MINUTES)
        }
        return command
    }

    /** Powers the laptop off immediately - no grace delay, and no automatic wake-up. */
    fun shutdownCommand(senderUid: String): Map<String, Any> = mapOf(
        "action" to SHUTDOWN_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
        "createdAt" to ServerValue.TIMESTAMP,
    )

    /** Turns Tailscale on the server computer on ([on]) or off. */
    fun tailscaleCommand(senderUid: String, on: Boolean): Map<String, String> = mapOf(
        "action" to TAILSCALE_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
        "state" to if (on) "UP" else "DOWN",
    )

    /** Starts installing every available update on the server computer (dnf upgrade). */
    fun systemUpdateCommand(senderUid: String): Map<String, String> = mapOf(
        "action" to SYSTEM_UPDATE_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
    )

    /** Error codes shared by several commands, in words; null for ones that need no explanation. */
    fun describeError(code: String?): String? = when (code) {
        "NOT_PERMITTED" -> "The server computer isn't set up for this yet - run install-remote-admin.sh on it once."
        "UPDATE_RUNNING" -> "A system update is running on the server computer. Try again when it has finished."
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
    fun systemInfoCommand(senderUid: String): Map<String, String> = mapOf(
        "action" to SYSTEM_INFO_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
    )

    /** [mode] must be [MODE_PERFORMANCE] or [MODE_POWERSAVE]. */
    fun powerModeCommand(senderUid: String, mode: String): Map<String, String> = mapOf(
        "action" to POWER_MODE_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
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
