package com.ferhatsanli.comapp

import org.junit.Assert.assertEquals
import org.junit.Test

class PingProtocolTest {
    @Test
    fun pendingCommandContainsOnlyTheFixedPingEnvelope() {
        assertEquals(
            mapOf(
                "action" to "PING",
                "status" to "PENDING",
            ),
            PingProtocol.pendingCommand(),
        )
    }

    @Test
    fun systemInfoCommandIsTheBareEnvelopeTheRulesAccept() {
        assertEquals(
            mapOf(
                "action" to "SYSTEM_INFO",
                "status" to "PENDING",
            ),
            PingProtocol.systemInfoCommand(),
        )
    }

    @Test
    fun speakCommandCarriesTheLanguage() {
        val english = PingProtocol.speakCommand("Hello", PingProtocol.LANGUAGE_EN)
        assertEquals("en", english["language"])
        assertEquals("Hello", english["text"])
        assertEquals("tr", PingProtocol.speakCommand("Merhaba")["language"])
        // Anything unknown falls back to Turkish rather than sending a value the Rules refuse.
        assertEquals("tr", PingProtocol.speakCommand("Hallo", "de")["language"])
    }

    @Test
    fun tailscaleAndUpdateCommandsMatchTheRules() {
        assertEquals("UP", PingProtocol.tailscaleCommand(true)["state"])
        assertEquals("DOWN", PingProtocol.tailscaleCommand(false)["state"])
        assertEquals("TAILSCALE", PingProtocol.tailscaleCommand(true)["action"])
        assertEquals(
            mapOf(
                "action" to "SYSTEM_UPDATE",
                "status" to "PENDING",
            ),
            PingProtocol.systemUpdateCommand(),
        )
    }

    @Test
    fun sharedErrorsAndTailscaleStatesHaveWords() {
        for (code in listOf("NOT_PERMITTED", "UPDATE_RUNNING", "SLEEP_CYCLE_ACTIVE")) {
            assert(PingProtocol.describeError(code) != null) { code }
        }
        assertEquals(null, PingProtocol.describeError("AGENT_ERROR"))
        for (state in listOf("RUNNING", "STOPPED", "STARTING", "NEEDS_LOGIN", "DAEMON_OFF", "NOT_INSTALLED", "UNKNOWN")) {
            assert(PingProtocol.tailscaleLabel(state) != null) { state }
        }
        assertEquals(null, PingProtocol.tailscaleLabel(null))
    }
}
