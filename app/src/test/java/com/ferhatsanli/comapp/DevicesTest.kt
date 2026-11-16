package com.ferhatsanli.comapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicesTest {
    @Test
    fun codesAreNormalizedAndChecked() {
        assertEquals("ABCD2345", Devices.normalizeCode("abcd 2345"))
        assertEquals("ABCD2345", Devices.normalizeCode("ABCD-2345-extra"))
        assertTrue(Devices.isValidCode("ABCD2345"))
        assertFalse(Devices.isValidCode("ABCD234"))
        assertFalse(Devices.isValidCode("ABCD0OI1")) // look-alikes are never used
        assertFalse(Devices.isValidCode("abcd2345"))
    }

    @Test
    fun parsesAListEntry() {
        val device = Devices.parse(
            "agent-1",
            mapOf(
                "name" to "MacBook Pro", "platform" to "macos", "pairedAt" to 1_700_000_000_000L,
                "agentVersion" to "2.0.0",
                "capabilities" to mapOf("speak" to true, "systemUpdate" to false, "odd" to "yes"),
            ),
        )!!
        assertEquals("MacBook Pro", device.name)
        assertEquals(1_700_000_000_000L, device.pairedAt)
        assertEquals(mapOf("speak" to true, "systemUpdate" to false), device.capabilities)
        assertNull(Devices.parse("x", mapOf("platform" to "macos")))
        assertNull(Devices.parse("x", mapOf("name" to " ", "platform" to "macos")))
    }

    @Test
    fun onlyAnExplicitFalseHidesAFeature() {
        val device = Device("a", "Mac", "macos", capabilities = mapOf("systemUpdate" to false, "speak" to true))
        assertFalse(device.supports("systemUpdate"))
        assertTrue(device.supports("speak"))
        assertTrue(device.supports("downloads")) // not reported yet
        assertTrue(device.supports(null)) // cards that every computer has
    }

    @Test
    fun claimWritesTheThreePlacesTheRulesExpect() {
        val update = Devices.claimUpdate("user-1", PairingOffer("agent-1", "  Living   room Mac ", "macos"), "ABCD2345")
        assertEquals(setOf("agents/agent-1", "users/user-1/devices/agent-1", "pairing/agent-1"), update.keys)
        assertNull(update["pairing/agent-1"])
        val agent = update["agents/agent-1"] as Map<*, *>
        assertEquals("user-1", agent["owner"])
        assertEquals("ABCD2345", agent["code"])
        val entry = update["users/user-1/devices/agent-1"] as Map<*, *>
        assertEquals("Living room Mac", entry["name"])
        assertEquals("macos", entry["platform"])
    }

    @Test
    fun unpairAndDeleteAccountRemoveEverything() {
        assertEquals(
            mapOf("agents/a" to null, "users/u/devices/a" to null, "users/u/data/a" to null),
            Devices.unpairUpdate("u", "a"),
        )
        assertEquals(
            mapOf("agents/a" to null, "agents/b" to null, "users/u" to null),
            Devices.deleteAccountUpdate("u", listOf("a", "b")),
        )
        assertEquals(mapOf("users/u" to null), Devices.deleteAccountUpdate("u", emptyList()))
    }

    @Test
    fun namesAndPlatforms() {
        assertEquals("macOS", Devices.platformLabel("macos"))
        assertEquals("x".repeat(40), Devices.cleanName("x".repeat(60)))
    }
}
