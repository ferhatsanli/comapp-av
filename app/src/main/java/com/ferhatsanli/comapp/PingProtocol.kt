package com.ferhatsanli.comapp

import android.content.res.Resources
import com.google.firebase.database.ServerValue

internal object PingProtocol {
    const val ACTION = "PING"
    const val SPEAK_ACTION = "SPEAK"
    const val SLEEP_ACTION = "SLEEP"
    const val SCAN_ACTION = "SCAN"
    const val SHUTDOWN_ACTION = "SHUTDOWN"
    const val RESTART_ACTION = "RESTART"
    const val CAFFEINATE_ACTION = "CAFFEINATE"
    const val OPEN_APP_ACTION = "OPEN_APP"
    const val LIST_APPS_ACTION = "LIST_APPS"
    const val POWER_MODE_ACTION = "POWER_MODE"
    const val SYSTEM_INFO_ACTION = "SYSTEM_INFO"
    const val TAILSCALE_ACTION = "TAILSCALE"
    const val SYSTEM_UPDATE_ACTION = "SYSTEM_UPDATE"
    const val SYSTEM_UPDATE_CANCEL_ACTION = "SYSTEM_UPDATE_CANCEL"
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

    /** Restarts the server computer right away; like SHUTDOWN, only while the command is fresh. */
    fun restartCommand(): Map<String, Any> = mapOf(
        "action" to RESTART_ACTION,
        "status" to "PENDING",
        "createdAt" to ServerValue.TIMESTAMP,
    )

    /** Opens the app with this bundle ID (one from /apps/latest) on the computer. */
    fun openAppCommand(bundleId: String): Map<String, String> = mapOf(
        "action" to OPEN_APP_ACTION,
        "status" to "PENDING",
        "app" to bundleId,
    )

    /** Asks the computer to send its app list again (after installing something). */
    fun listAppsCommand(): Map<String, String> = mapOf("action" to LIST_APPS_ACTION, "status" to "PENDING")

    /** Keeps the Mac awake with `caffeinate` ([on]) or lets it sleep again. */
    fun caffeinateCommand(on: Boolean): Map<String, String> = mapOf(
        "action" to CAFFEINATE_ACTION,
        "status" to "PENDING",
        "state" to if (on) "ON" else "OFF",
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

    /** Stops a running system update on the server computer (macOS: the whole update job). */
    fun systemUpdateCancelCommand(): Map<String, String> = mapOf(
        "action" to SYSTEM_UPDATE_CANCEL_ACTION,
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
    fun describeError(res: Resources, code: String?, action: String? = null): String? = when (code) {
        "BAD_INPUT" -> if (action == OPEN_APP_ACTION) res.getString(R.string.cmd_err_app_gone) else null
        "NOT_PERMITTED" -> res.getString(
            when (action) {
                MEDIA_ACTION -> R.string.cmd_err_media_permission
                SLEEP_ACTION -> R.string.cmd_err_sleep_permission
                SYSTEM_UPDATE_CANCEL_ACTION -> R.string.cmd_err_cancel_permission
                else -> R.string.cmd_err_admin_setup
            },
        )
        "UPDATE_RUNNING" -> res.getString(R.string.cmd_err_update_running)
        "SLEEP_CYCLE_ACTIVE" -> res.getString(R.string.cmd_err_sleep_cycle)
        else -> null
    }

    /** "Tailscale" line of System Info for a /power/latest tailscale value. */
    fun tailscaleLabel(res: Resources, state: String?): String? = when (state) {
        "RUNNING" -> R.string.ts_connected
        "STOPPED" -> R.string.ts_off
        "STARTING" -> R.string.ts_starting
        "NEEDS_LOGIN" -> R.string.ts_needs_login
        "DAEMON_OFF" -> R.string.ts_daemon_off
        "NOT_INSTALLED" -> R.string.ts_not_installed
        "UNKNOWN" -> R.string.common_unknown
        else -> null
    }?.let(res::getString)

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

    /** 80 -> "1 h 20 min", 120 -> "2 h", 2 -> "2 min" (in English). */
    fun formatDuration(res: Resources, totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> res.getString(R.string.duration_h_min, hours, minutes)
            hours > 0 -> res.getString(R.string.duration_h, hours)
            else -> res.getString(R.string.duration_min, minutes)
        }
    }
}
