package com.ferhatsanli.comapp

import java.text.Normalizer

/** One app the server computer can open (from /apps/latest/items). [icon] is a base64 PNG. */
internal data class ServerApp(val key: String, val bundleId: String, val name: String, val icon: String?)

/** Searching and the "recently opened" row of the Apps card, kept free of Android for tests. */
internal object ServerApps {
    const val MAX_RECENT = 8
    const val COLLAPSED_COUNT = 12

    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    /**
     * Apps whose name contains [query], ignoring case and accents ("cal" finds Calculator and
     * Calendar). Names that start with it come first, then alphabetical. A blank query lists all.
     */
    fun search(apps: List<ServerApp>, query: String): List<ServerApp> {
        val wanted = fold(query.trim())
        val sorted = apps.sortedBy { fold(it.name) }
        if (wanted.isEmpty()) return sorted
        return sorted
            .filter { fold(it.name).contains(wanted) }
            .sortedBy { if (fold(it.name).startsWith(wanted)) 0 else 1 }
    }

    /** [bundleId] moved to the front of [recent], without duplicates, at most [MAX_RECENT]. */
    fun opened(recent: List<String>, bundleId: String): List<String> =
        (listOf(bundleId) + recent.filter { it != bundleId }).take(MAX_RECENT)

    /** The recently opened apps that are still installed, newest first. */
    fun recent(apps: List<ServerApp>, recent: List<String>): List<ServerApp> {
        val byId = apps.associateBy { it.bundleId }
        return recent.mapNotNull { byId[it] }
    }

    fun encode(recent: List<String>): String = recent.joinToString(",")

    fun decode(saved: String?): List<String> =
        saved.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_RECENT)
}
