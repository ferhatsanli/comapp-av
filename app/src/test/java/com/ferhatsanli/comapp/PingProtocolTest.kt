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

    @Test
    fun mediaCommandsCarryVolumeOnlyForVolume() {
        assertEquals(
            mapOf("action" to "MEDIA", "status" to "PENDING", "media" to "PLAY_PAUSE"),
            PingProtocol.mediaCommand(PingProtocol.MEDIA_PLAY_PAUSE),
        )
        // A stray volume on a track button is dropped: the Rules refuse it there.
        assertEquals(null, PingProtocol.mediaCommand(PingProtocol.MEDIA_NEXT, 40)["volume"])
        assertEquals(35, PingProtocol.mediaCommand(PingProtocol.MEDIA_VOLUME, 35)["volume"])
        assertEquals(100, PingProtocol.mediaCommand(PingProtocol.MEDIA_VOLUME, 140)["volume"])
        assertEquals(0, PingProtocol.mediaCommand(PingProtocol.MEDIA_VOLUME, -5)["volume"])
    }

    @Test
    fun notPermittedExplainsAccessibilityForMedia() {
        val media = PingProtocol.describeError("NOT_PERMITTED", PingProtocol.MEDIA_ACTION)!!
        assertEquals(true, media.contains("Accessibility"))
        val other = PingProtocol.describeError("NOT_PERMITTED", PingProtocol.SHUTDOWN_ACTION)!!
        assertEquals(true, other.contains("install-remote-admin.sh"))
        assertEquals(other, PingProtocol.describeError("NOT_PERMITTED"))
    }

    @Test
    fun timedSleepWithoutTheAdminSetupPointsToTheMacScript() {
        assertEquals(true, PingProtocol.describeError("NOT_PERMITTED", PingProtocol.SLEEP_ACTION)!!.contains("install-macos-admin.sh"))
        val update = PingProtocol.describeError("NOT_PERMITTED", PingProtocol.SYSTEM_UPDATE_ACTION)!!
        assertEquals(true, update.contains("install-macos-admin.sh") && update.contains("install-remote-admin.sh"))
    }

    @Test
    fun caffeinateCommandIsOnOrOff() {
        assertEquals(
            mapOf("action" to "CAFFEINATE", "status" to "PENDING", "state" to "ON"),
            PingProtocol.caffeinateCommand(true),
        )
        assertEquals("OFF", PingProtocol.caffeinateCommand(false)["state"])
    }
}
