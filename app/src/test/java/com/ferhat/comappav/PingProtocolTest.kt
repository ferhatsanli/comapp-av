package com.ferhat.comappav

import org.junit.Assert.assertEquals
import org.junit.Test

class PingProtocolTest {
    @Test
    fun pendingCommandContainsOnlyTheFixedPingEnvelope() {
        assertEquals(
            mapOf(
                "action" to "PING",
                "senderUid" to PingProtocol.ANDROID_UID,
                "targetUid" to PingProtocol.PI_UID,
                "status" to "PENDING",
            ),
            PingProtocol.pendingCommand(PingProtocol.ANDROID_UID),
        )
    }

    @Test
    fun systemInfoCommandIsTheBareEnvelopeTheRulesAccept() {
        assertEquals(
            mapOf(
                "action" to "SYSTEM_INFO",
                "senderUid" to PingProtocol.ANDROID_UID,
                "targetUid" to PingProtocol.PI_UID,
                "status" to "PENDING",
            ),
            PingProtocol.systemInfoCommand(PingProtocol.ANDROID_UID),
        )
    }

    @Test
    fun speakCommandCarriesTheLanguage() {
        val english = PingProtocol.speakCommand(PingProtocol.ANDROID_UID, "Hello", PingProtocol.LANGUAGE_EN)
        assertEquals("en", english["language"])
        assertEquals("Hello", english["text"])
        assertEquals("tr", PingProtocol.speakCommand(PingProtocol.ANDROID_UID, "Merhaba")["language"])
        // Anything unknown falls back to Turkish rather than sending a value the Rules refuse.
        assertEquals("tr", PingProtocol.speakCommand(PingProtocol.ANDROID_UID, "Hallo", "de")["language"])
    }

    @Test
    fun tailscaleAndUpdateCommandsMatchTheRules() {
        assertEquals("UP", PingProtocol.tailscaleCommand(PingProtocol.ANDROID_UID, true)["state"])
        assertEquals("DOWN", PingProtocol.tailscaleCommand(PingProtocol.ANDROID_UID, false)["state"])
        assertEquals("TAILSCALE", PingProtocol.tailscaleCommand(PingProtocol.ANDROID_UID, true)["action"])
        assertEquals(
            mapOf(
                "action" to "SYSTEM_UPDATE",
                "senderUid" to PingProtocol.ANDROID_UID,
                "targetUid" to PingProtocol.PI_UID,
                "status" to "PENDING",
            ),
            PingProtocol.systemUpdateCommand(PingProtocol.ANDROID_UID),
        )
    }

    @Test
    fun sharedErrorsAndTailscaleStatesHaveWords() {
        for (code in listOf("NOT_PERMITTED", "UPDATE_RUNNING", "TASKS_RUNNING")) {
            assert(PingProtocol.describeError(code) != null) { code }
        }
        assertEquals(null, PingProtocol.describeError("AGENT_ERROR"))
        for (state in listOf("RUNNING", "STOPPED", "STARTING", "NEEDS_LOGIN", "DAEMON_OFF", "NOT_INSTALLED", "UNKNOWN")) {
            assert(PingProtocol.tailscaleLabel(state) != null) { state }
        }
        assertEquals(null, PingProtocol.tailscaleLabel(null))
    }

    @Test
    fun fedoraTasksCommandIsTheBareEnvelope() {
        assertEquals(
            mapOf(
                "action" to "FEDORA_TASKS",
                "senderUid" to PingProtocol.ANDROID_UID,
                "targetUid" to PingProtocol.PI_UID,
                "status" to "PENDING",
            ),
            PingProtocol.fedoraTasksCommand(PingProtocol.ANDROID_UID),
        )
    }
}
