package com.ferhatsanli.comapp

import com.google.firebase.database.ServerValue

/**
 * One paired computer: its entry in /users/{uid}/devices/{id}. The id is the agent's own
 * account id. See docs/data-model.md.
 */
internal data class Device(
    val id: String,
    val name: String,
    val platform: String,
    val pairedAt: Long? = null,
    val hostname: String? = null,
    val osVersion: String? = null,
    val agentVersion: String? = null,
    val capabilities: Map<String, Boolean> = emptyMap(),
) {
    /**
     * Whether this computer can do [capability]. Only an explicit "false" from the agent hides
     * a feature, so a computer that has not reported yet shows everything.
     */
    fun supports(capability: String?): Boolean = capability == null || capabilities[capability] != false
}

/** What an unpaired agent published under /pairing/{agentUid}. */
internal data class PairingOffer(val agentUid: String, val name: String, val platform: String)

internal object Devices {
    const val CODE_LENGTH = 8
    const val MAX_NAME_LENGTH = 40
    const val PAIRING_PATH = "pairing"

    // The agent's alphabet: capitals and digits without the look-alikes O, I, 0 and 1.
    private val CODE = Regex("^[A-HJ-NP-Z2-9]{$CODE_LENGTH}$")

    fun devicesPath(uid: String) = "users/$uid/devices"

    fun dataPath(uid: String, deviceId: String) = "users/$uid/data/$deviceId"

    /** What the user typed, as a code: capitals, no spaces or dashes, at most 8 characters. */
    fun normalizeCode(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }.take(CODE_LENGTH)

    fun isValidCode(code: String): Boolean = CODE.matches(code)

    fun cleanName(input: String): String = input.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH)

    fun platformLabel(platform: String): String = when (platform) {
        "macos" -> "macOS"
        "linux" -> "Linux"
        "windows" -> "Windows"
        else -> platform
    }

    /** A device from its fields, or null when the entry is not usable (no name or platform). */
    fun parse(id: String, fields: Map<String, Any?>): Device? {
        val name = (fields["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val platform = fields["platform"] as? String ?: return null
        val capabilities = (fields["capabilities"] as? Map<*, *>).orEmpty()
            .mapNotNull { (key, value) -> if (key is String && value is Boolean) key to value else null }
            .toMap()
        return Device(
            id = id,
            name = name,
            platform = platform,
            pairedAt = (fields["pairedAt"] as? Number)?.toLong(),
            hostname = fields["hostname"] as? String,
            osVersion = fields["osVersion"] as? String,
            agentVersion = fields["agentVersion"] as? String,
            capabilities = capabilities,
        )
    }

    /**
     * The one update that pairs a computer: claim the agent with its code, add it to the
     * user's list, and remove the used code. The Rules accept it only with the right, fresh
     * code and only while the agent has no owner.
     */
    fun claimUpdate(uid: String, offer: PairingOffer, code: String): Map<String, Any?> = mapOf(
        "agents/${offer.agentUid}" to mapOf("owner" to uid, "code" to code, "pairedAt" to ServerValue.TIMESTAMP),
        "${devicesPath(uid)}/${offer.agentUid}" to mapOf(
            "name" to (cleanName(offer.name).ifEmpty { platformLabel(offer.platform) }),
            "platform" to offer.platform,
            "pairedAt" to ServerValue.TIMESTAMP,
        ),
        "$PAIRING_PATH/${offer.agentUid}" to null,
    )

    /** Removes one computer: the agent loses access at once and can be paired again. */
    fun unpairUpdate(uid: String, deviceId: String): Map<String, Any?> = mapOf(
        "agents/$deviceId" to null,
        "${devicesPath(uid)}/$deviceId" to null,
        dataPath(uid, deviceId) to null,
    )

    /** Everything the account owns: its agents' ownership records and its whole tree. */
    fun deleteAccountUpdate(uid: String, deviceIds: Collection<String>): Map<String, Any?> =
        deviceIds.associate { "agents/$it" to null } + ("users/$uid" to null)
}
