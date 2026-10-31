package com.ferhat.comappav

// Plain Kotlin with no Android dependency on purpose, so it can be unit-tested on the JVM.

/** One of the phone's latest commands, as read back from /commands. */
internal data class RecentCommand(
    val id: String,
    val action: String,
    val status: String,
    val result: String?,
    val errorCode: String?,
    /** When the phone sent it, from the push ID; null if the ID isn't a push ID. */
    val sentAtMs: Long?,
    /** The command's own fields worth showing (text, mode, link, ...), label to value. */
    val details: List<Pair<String, String>>,
)

internal object RecentCommands {
    /** How many of the latest commands the card shows. */
    const val SHOWN = 5

    private const val PUSH_CHARS = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz"

    /**
     * Firebase push IDs start with 8 characters that encode the creation time in milliseconds
     * (base 64, in [PUSH_CHARS] order), which is when the phone sent the command.
     */
    fun pushIdTimeMs(id: String): Long? {
        if (id.length < 8) return null
        var time = 0L
        for (char in id.substring(0, 8)) {
            val digit = PUSH_CHARS.indexOf(char)
            if (digit < 0) return null
            time = time * 64 + digit
        }
        return time
    }

    /** The extra fields of a command, in a readable order; anything empty is left out. */
    fun details(fields: Map<String, Any?>): List<Pair<String, String>> {
        val labels = listOf(
            "text" to "Text",
            "language" to "Language",
            "mode" to "Mode",
            "state" to "Turn",
            "source" to "Source",
            "url" to "Link",
            "durationMinutes" to "Duration",
        )
        return labels.mapNotNull { (key, label) ->
            val value = fields[key] ?: return@mapNotNull null
            val text = when (key) {
                "language" -> if (value == PingProtocol.LANGUAGE_EN) "English" else "Turkish"
                "state" -> if (value == "UP") "On" else "Off"
                "durationMinutes" -> (value as? Number)?.toInt()?.let { PingProtocol.formatDuration(it) } ?: value.toString()
                else -> value.toString()
            }
            if (text.isBlank()) null else label to text
        }
    }

    /** The status line of an expanded row. */
    fun outcome(command: RecentCommand): String = when (command.status) {
        "SUCCESS" -> "Done" + (command.result?.let { " ($it)" } ?: "")
        "FAILED" -> "Failed" + (command.errorCode?.let { " ($it)" } ?: "")
        "PROCESSING" -> "The agent is working on it"
        "PENDING" -> "Waiting for the agent"
        else -> command.status
    }
}
