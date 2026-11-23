package com.ferhatsanli.comapp

// Plain Kotlin with no Android dependency on purpose, so it can be unit-tested on the JVM.

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
     * While running: "No new output for 12 min" once the update has been silent for
     * [STALL_MINUTES] or more, so a stuck step is visible; null otherwise.
     */
    fun stallHint(now: Long): String? {
        if (!running) return null
        val since = outputAt ?: startedAt ?: return null
        val minutes = ((now - since) / 60_000L).coerceAtLeast(0)
        return if (minutes >= STALL_MINUTES) "No new output for $minutes min" else null
    }

    /** 0..1 for the progress bar: the run's percentage while it goes, full when it worked. */
    val fraction: Float
        get() = when (state) {
            "DONE" -> 1f
            else -> (percent.coerceIn(0, 100)) / 100f
        }

    /** One line under the progress bar. */
    fun describe(): String = when (state) {
        "RUNNING" -> {
            val step = when (phase) {
                "APP_STORE" -> "Updating App Store apps"
                "REFRESHING" -> "Checking for updates"
                "DOWNLOADING" -> if (total > 0) "Downloading $done of $total" else "Downloading"
                "INSTALLING" -> if (total > 0) "Installing $done of $total" else "Installing"
                "VERIFYING" -> "Verifying"
                else -> "Finishing"
            }
            "$step · $percent%"
        }
        "DONE" -> when {
            phase == "UP_TO_DATE" -> "Already up to date."
            packages > 0 -> (if (macos) "Installed $packages update" else "Updated $packages package") +
                "${if (packages == 1) "" else "s"}." +
                if (rebootRecommended == true) " A restart is recommended." else ""
            else -> "Update finished." + if (rebootRecommended == true) " A restart is recommended." else ""
        }
        else -> when (error) {
            "CANCELLED" -> "The update was cancelled. What finished before stays installed."
            "INTERRUPTED" -> "The update stopped before it finished (the server computer restarted or it was killed)."
            null -> "The update failed."
            else -> "The update failed ($error)."
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
