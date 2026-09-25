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
}
