package com.ferhat.comappav

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
) {
    val running: Boolean get() = state == "RUNNING"

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
            packages > 0 -> "Updated $packages package${if (packages == 1) "" else "s"}." +
                if (rebootRecommended == true) " A restart is recommended." else ""
            else -> "Update finished." + if (rebootRecommended == true) " A restart is recommended." else ""
        }
        else -> when (error) {
            "INTERRUPTED" -> "The update stopped before it finished (the server computer restarted or it was killed)."
            null -> "The update failed."
            else -> "The update failed ($error)."
        }
    }
}
