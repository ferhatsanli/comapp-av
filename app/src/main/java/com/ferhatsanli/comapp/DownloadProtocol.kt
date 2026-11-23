package com.ferhatsanli.comapp

import android.content.res.Resources
import java.net.URLDecoder
import java.util.Base64
import java.util.Locale

// Plain Kotlin apart from reading texts from Resources, so it can be unit-tested on the JVM
// (Robolectric supplies the Resources).

/** Something to download on the server computer: a magnet link, a direct link, or an uploaded .torrent file. */
internal data class DownloadRequest(
    val source: String,
    val url: String?,
    val torrentBase64: String?,
    val displayName: String,
)

/**
 * The phone's side of DOWNLOAD_ADD (see the agent's downloads.py and the Rules): recognising
 * links and torrent files, building the command, and presenting /downloads/latest.
 */
internal object DownloadProtocol {
    const val ACTION = "DOWNLOAD_ADD"
    const val SOURCE_MAGNET = "MAGNET"
    const val SOURCE_URL = "URL"
    const val SOURCE_TORRENT_FILE = "TORRENT_FILE"
    const val RESULT_STARTED = "DOWNLOAD_STARTED"
    const val MAX_URL_LENGTH = 4096
    const val MAX_TORRENT_BYTES = 2 * 1024 * 1024
    const val DEFAULT_LIMIT_MBPS = 20
    const val MAX_LIMIT_MBPS = 10_000

    /** The speed-limit choices on screen; 0 is "Unlimited". */
    val LIMIT_PRESETS_MBPS = listOf(5, 10, 20, 50, 100, 0)

    private val magnetPattern = Regex("""magnet:\?\S+""", RegexOption.IGNORE_CASE)
    private val urlPattern = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val infoHashParam = Regex("""[?&]xt=urn:bt(ih|mh):""", RegexOption.IGNORE_CASE)

    /**
     * The first magnet or http(s) link in text shared from another app - browsers often share
     * "Page title https://...", and a link at the end of a sentence may carry its punctuation.
     */
    fun findLink(text: String?): DownloadRequest? {
        if (text == null) return null
        val magnet = magnetPattern.find(text)?.value
        val link = (magnet ?: urlPattern.find(text)?.value)?.trimEnd('.', ',', ';', ')', ']', '>', '"', '\'')
            ?: return null
        if (link.length > MAX_URL_LENGTH) return null
        return if (magnet != null) {
            if (!infoHashParam.containsMatchIn(link)) return null
            DownloadRequest(SOURCE_MAGNET, link, null, magnetName(link))
        } else {
            DownloadRequest(SOURCE_URL, link, null, linkName(link))
        }
    }

    /** A .torrent file picked or shared on the phone, ready to upload; null if it can't be one. */
    fun torrentFile(bytes: ByteArray, name: String?): DownloadRequest? {
        if (bytes.isEmpty() || bytes.size > MAX_TORRENT_BYTES || bytes[0] != 'd'.code.toByte()) return null
        return DownloadRequest(
            source = SOURCE_TORRENT_FILE,
            url = null,
            torrentBase64 = Base64.getEncoder().encodeToString(bytes),
            displayName = name?.takeIf { it.isNotBlank() } ?: "Torrent file",
        )
    }

    fun command(request: DownloadRequest): Map<String, String> = buildMap {
        put("action", ACTION)
        put("status", "PENDING")
        put("source", request.source)
        request.url?.let { put("url", it) }
    }

    /** What goes to uploads/<commandId> before the command itself (torrent files only). */
    fun upload(request: DownloadRequest): Map<String, String>? = request.torrentBase64?.let { mapOf("torrent" to it) }

    /** The agent's specific DOWNLOAD_ADD error codes, in words. */
    fun describeError(res: Resources, code: String?): String? = when (code) {
        "NOT_A_FILE" -> R.string.dl_err_not_a_file
        "NO_SPACE" -> R.string.dl_err_no_space
        "BAD_INPUT" -> R.string.dl_err_bad_input
        "ENGINE_DOWN" -> R.string.dl_err_engine_down
        else -> null
    }?.let(res::getString)

    /** A single download's failure reason from /downloads/latest, in words. */
    fun describeItemError(res: Resources, code: String?): String = when {
        code == null -> res.getString(R.string.dl_item_failed)
        code == "NETWORK" -> res.getString(R.string.dl_item_network)
        code == "TIMEOUT" -> res.getString(R.string.dl_item_timeout)
        code == "HTTP_ERROR" -> res.getString(R.string.dl_item_http)
        code == "DISK" -> res.getString(R.string.dl_item_disk)
        code.startsWith("ARIA2_") -> res.getString(R.string.dl_item_aria2, code.removePrefix("ARIA2_"))
        code.startsWith("CURL_") -> res.getString(R.string.dl_item_curl, code.removePrefix("CURL_"))
        else -> "$code."
    }

    fun stateLabel(res: Resources, state: String): String = when (state) {
        "QUEUED" -> R.string.dl_state_queued
        "DOWNLOADING" -> R.string.dl_state_downloading
        "SEEDING" -> R.string.dl_state_seeding
        "COMPLETE" -> R.string.dl_state_complete
        "FAILED" -> R.string.dl_state_failed
        "PAUSED" -> R.string.dl_state_paused
        else -> null
    }?.let(res::getString) ?: state

    fun limitLabel(res: Resources, mbps: Int): String = if (mbps <= 0) res.getString(R.string.dl_unlimited) else "$mbps Mbit/s"

    /** settings/downloadLimitMbps as the phone reads it back; anything odd means the default. */
    fun parseLimit(value: Any?): Int {
        val number = when (value) {
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is Double -> value
            else -> return DEFAULT_LIMIT_MBPS
        }
        if (number % 1.0 != 0.0 || number < 0 || number > MAX_LIMIT_MBPS) return DEFAULT_LIMIT_MBPS
        return number.toInt()
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit])
    }

    fun formatSpeed(bytesPerSecond: Long): String = formatBytes(bytesPerSecond) + "/s"

    fun formatEta(res: Resources, seconds: Long): String = when {
        seconds < 60 -> res.getString(R.string.duration_s, seconds.toInt())
        seconds < 3600 -> res.getString(R.string.duration_min, (seconds / 60).toInt())
        else -> {
            val minutes = ((seconds % 3600) / 60).toInt()
            val hours = (seconds / 3600).toInt()
            if (minutes == 0) res.getString(R.string.duration_h, hours) else res.getString(R.string.duration_h_min, hours, minutes)
        }
    }

    private fun magnetName(magnet: String): String {
        val dn = magnet.substringAfter('?').split('&').firstOrNull { it.startsWith("dn=", ignoreCase = true) }
        val name = dn?.substringAfter('=')?.let { decode(it) }?.trim()
        return if (name.isNullOrEmpty()) "Magnet link" else name
    }

    private fun linkName(url: String): String {
        val afterScheme = url.substringAfter("://")
        val host = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val path = afterScheme.substringAfter('/', "").substringBefore('?').substringBefore('#').trimEnd('/')
        val last = decode(path.substringAfterLast('/')).trim()
        return last.ifEmpty { host }
    }

    private fun decode(value: String): String = try {
        URLDecoder.decode(value, Charsets.UTF_8)
    } catch (e: IllegalArgumentException) {
        value
    }
}
