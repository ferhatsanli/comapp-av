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
}
