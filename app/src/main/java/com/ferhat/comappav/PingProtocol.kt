package com.ferhat.comappav

internal object PingProtocol {
    const val ANDROID_UID = "57HVnDSvZKNbJBabrDF4c2gnv462"
    const val PI_UID = "N8V4nrLrELa83EXRLWJsiis85Bq1"
    const val ACTION = "PING"

    fun pendingCommand(senderUid: String): Map<String, String> = mapOf(
        "action" to ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
    )
}
