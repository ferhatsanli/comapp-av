package com.ferhat.comappav

// Plain Kotlin with no Android dependency on purpose, so it can be unit-tested on the JVM.

/**
 * The user's own order of cards and categories, saved on the phone.
 *
 * Stored as one line per list, "listName=ITEM,ITEM,ITEM", using enum names. Reading is lenient:
 * lines that don't parse are skipped, and [apply] reconciles a saved order with the cards that
 * exist in this version of the app.
 */
internal object CardOrder {
    fun encode(orders: Map<String, List<String>>): String =
        orders.entries
            .sortedBy { it.key }
            .joinToString(separator = "") { (list, items) -> "$list=${items.joinToString(",")}\n" }

    fun decode(text: String): Map<String, List<String>> =
        text.lineSequence()
            .mapNotNull { line ->
                val split = line.indexOf('=')
                if (split <= 0) return@mapNotNull null
                val list = line.substring(0, split).trim()
                val items = line.substring(split + 1).split(',').map { it.trim() }.filter { it.isNotEmpty() }
                list to items
            }
            .toMap()

    /**
     * [defaults] put in the [saved] order. Names that no longer exist are dropped, duplicates are
     * ignored, and anything new since the order was saved is appended in its default position
     * order - so an app update never loses or resurrects a card.
     */
    fun <T> apply(saved: List<String>?, defaults: List<T>, name: (T) -> String): List<T> {
        if (saved == null) return defaults
        val byName = defaults.associateBy(name)
        val ordered = saved.distinct().mapNotNull { byName[it] }
        return ordered + defaults.filter { it !in ordered }
    }
}
