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
    const val MODE_PERFORMANCE = "PERFORMANCE"
    const val MODE_POWERSAVE = "POWERSAVE"
    const val MAX_SLEEP_MINUTES = 1440
    const val MAX_SPEAK_LENGTH = 300

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

    fun speakCommand(senderUid: String, text: String): Map<String, String> = mapOf(
        "action" to SPEAK_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
        "text" to text.take(MAX_SPEAK_LENGTH),
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
