package com.ferhat.comappav

import com.google.firebase.database.ServerValue

internal object PingProtocol {
    const val ANDROID_UID = "57HVnDSvZKNbJBabrDF4c2gnv462"
    const val PI_UID = "N8V4nrLrELa83EXRLWJsiis85Bq1"
    const val ACTION = "PING"
    const val SPEAK_ACTION = "SPEAK"
    const val SLEEP_ACTION = "SLEEP"
    const val WAKE_TIME_LABEL = "08:00"
    const val MAX_SPEAK_LENGTH = 300

    fun pendingCommand(senderUid: String): Map<String, String> = mapOf(
        "action" to ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
    )

    fun speakCommand(senderUid: String, text: String): Map<String, String> = mapOf(
        "action" to SPEAK_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
        "text" to text.take(MAX_SPEAK_LENGTH),
    )

    fun sleepCommand(senderUid: String): Map<String, Any> = mapOf(
        "action" to SLEEP_ACTION,
        "senderUid" to senderUid,
        "targetUid" to PI_UID,
        "status" to "PENDING",
        "createdAt" to ServerValue.TIMESTAMP,
    )
}
