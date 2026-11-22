package com.ferhatsanli.comapp

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerAppsTest {
    private val apps = listOf(
        ServerApp("com_apple_Safari", "com.apple.Safari", "Safari", null),
        ServerApp("com_apple_calculator", "com.apple.calculator", "Calculator", null),
        ServerApp("com_apple_iCal", "com.apple.iCal", "Calendar", null),
        ServerApp("org_videolan_vlc", "org.videolan.vlc", "VLC", null),
        ServerApp("com_apple_ScriptEditor2", "com.apple.ScriptEditor2", "Script Editor", null),
        ServerApp("com_example_cafe", "com.example.cafe", "Café Menu", null),
    )

    @Test
    fun searchIgnoresCaseAndAccentsAndPutsPrefixesFirst() {
        assertEquals(listOf("Calculator", "Calendar"), ServerApps.search(apps, "CAL").map { it.name })
        assertEquals(listOf("Café Menu"), ServerApps.search(apps, "cafe").map { it.name })
        assertEquals(listOf("Safari", "Script Editor"), ServerApps.search(apps, "s").map { it.name })
        // Containing the query anywhere is enough; those stay alphabetical.
        assertEquals(listOf("Calendar", "Script Editor"), ServerApps.search(apps, "e").map { it.name }.filter { it in setOf("Calendar", "Script Editor") })
        assertEquals(listOf("Script Editor"), ServerApps.search(apps, "edit").map { it.name })
        assertEquals(6, ServerApps.search(apps, "  ").size)
        assertEquals("Café Menu", ServerApps.search(apps, "").first().name)
    }

    @Test
    fun recentlyOpenedIsNewestFirstWithoutDuplicates() {
        var recent = emptyList<String>()
        for (id in listOf("a", "b", "a", "c")) recent = ServerApps.opened(recent, id)
        assertEquals(listOf("c", "a", "b"), recent)
        repeat(20) { recent = ServerApps.opened(recent, "x$it") }
        assertEquals(ServerApps.MAX_RECENT, recent.size)
        assertEquals(listOf("org.videolan.vlc"), ServerApps.recent(apps, listOf("gone.app", "org.videolan.vlc")).map { it.bundleId })
        assertEquals(listOf("a", "b"), ServerApps.decode(ServerApps.encode(listOf("a", "b"))))
        assertEquals(emptyList<String>(), ServerApps.decode(null))
    }

    @Test
    fun removingFromRecentKeepsTheRestInOrder() {
        assertEquals(listOf("a", "c"), ServerApps.removed(listOf("a", "b", "c"), "b"))
        assertEquals(listOf("a"), ServerApps.removed(listOf("a"), "missing"))
    }
}
