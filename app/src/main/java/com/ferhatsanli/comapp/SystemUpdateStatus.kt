package com.ferhatsanli.comapp

// Plain Kotlin apart from reading texts from Resources, so it can be unit-tested on the JVM
// (Robolectric supplies the Resources).

import android.content.res.Resources

/** /system/update as the agent publishes it (see the agent's system_update.py). */
internal data class SystemUpdateStatus(
    val state: String,
    val phase: String,
    val percent: Int,
    val done: Int,
    val total: Int,
    val packages: Int,
    val startedAt: Long?,
    val finishedAt: Long?,
    val error: String?,
    val rebootRecommended: Boolean?,
    /** macOS reports App Store apps and system updates rather than packages. */
    val macos: Boolean = false,
    /** The last lines of the update's output (newest last), as the agent tidied them. */
    val log: String? = null,
    /** When the update last printed anything (server time, ms). */
    val outputAt: Long? = null,
) {
    val running: Boolean get() = state == "RUNNING"

    val cancelled: Boolean get() = error == "CANCELLED" || phase == "CANCELLED"

    /** The log as lines, empty ones dropped. */
    val logLines: List<String> get() = log.orEmpty().lines().filter { it.isNotBlank() }

    /**
     * While running: minutes without new output once the update has been silent for
     * [STALL_MINUTES] or more, so a stuck step is visible; null otherwise.
     */
    fun stallMinutes(now: Long): Long? {
        if (!running) return null
        val since = outputAt ?: startedAt ?: return null
        val minutes = ((now - since) / 60_000L).coerceAtLeast(0)
        return if (minutes >= STALL_MINUTES) minutes else null
    }

    /** 0..1 for the progress bar: the run's percentage while it goes, full when it worked. */
    val fraction: Float
        get() = when (state) {
            "DONE" -> 1f
            else -> (percent.coerceIn(0, 100)) / 100f
        }

    /** One line under the progress bar. */
    fun describe(res: Resources): String = when (state) {
        "RUNNING" -> {
            val step = when (phase) {
                "APP_STORE" -> res.getString(R.string.upd_step_app_store)
                "REFRESHING" -> res.getString(R.string.upd_step_checking)
                "DOWNLOADING" -> if (total > 0) {
                    res.getString(R.string.upd_step_downloading_n, done, total)
                } else {
                    res.getString(R.string.upd_step_downloading)
                }
                "INSTALLING" -> if (total > 0) {
                    res.getString(R.string.upd_step_installing_n, done, total)
                } else {
                    res.getString(R.string.upd_step_installing)
                }
                "VERIFYING" -> res.getString(R.string.upd_step_verifying)
                else -> res.getString(R.string.upd_step_finishing)
            }
            res.getString(R.string.upd_running, step, percent)
        }
        "DONE" -> {
            val restart = if (rebootRecommended == true) " " + res.getString(R.string.upd_restart_recommended) else ""
            when {
                phase == "UP_TO_DATE" -> res.getString(R.string.upd_up_to_date)
                packages > 0 -> res.getQuantityString(
                    if (macos) R.plurals.upd_mac_updates else R.plurals.upd_packages,
                    packages,
                    packages,
                ) + restart
                else -> res.getString(R.string.upd_finished) + restart
            }
        }
        else -> when (error) {
            "CANCELLED" -> res.getString(R.string.upd_cancelled)
            "INTERRUPTED" -> res.getString(R.string.upd_interrupted)
            null -> res.getString(R.string.upd_failed)
            else -> res.getString(R.string.upd_failed_code, error)
        }
    }

    companion object {
        const val STALL_MINUTES = 5L

        /**
         * The lines of [current] that were not in [previous]: the log is a sliding tail, so the
         * new part starts after the longest end of [previous] that [current] begins with.
         */
        fun newLines(previous: List<String>, current: List<String>): List<String> {
            if (previous.isEmpty()) return current
            for (overlap in minOf(previous.size, current.size) downTo 1) {
                if (previous.subList(previous.size - overlap, previous.size) == current.subList(0, overlap)) {
                    return current.drop(overlap)
                }
            }
            return current
        }
    }
}
