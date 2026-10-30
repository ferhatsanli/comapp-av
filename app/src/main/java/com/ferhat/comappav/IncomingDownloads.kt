package com.ferhat.comappav

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateOf

/**
 * Links, magnets and .torrent files that arrive from other apps ("Share" -> Download on the server computer,
 * tapping a magnet link, opening a .torrent). They only wait here: the main screen asks for a
 * confirmation before anything is sent, so another app can never start a download on its own.
 */
internal object IncomingDownloads {
    /** The download waiting for the user's confirmation, if any. */
    val pending = mutableStateOf<DownloadRequest?>(null)

    /** Reads [intent]; returns true when it carried something downloadable. */
    fun accept(context: Context, intent: Intent?): Boolean {
        val request = read(context, intent) ?: return false
        pending.value = request
        AppLog.info("Downloads", "Received \"${request.displayName}\" from another app; waiting for confirmation")
        return true
    }

    private fun read(context: Context, intent: Intent?): DownloadRequest? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val stream = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                if (stream != null) {
                    readTorrent(context, stream)
                } else {
                    DownloadProtocol.findLink(intent.getStringExtra(Intent.EXTRA_TEXT)).also {
                        if (it == null) AppLog.warn("Downloads", "The shared text had no magnet or http(s) link")
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val data = intent.data ?: return null
                if (data.scheme.equals("magnet", ignoreCase = true)) {
                    DownloadProtocol.findLink(data.toString())
                } else {
                    readTorrent(context, data)
                }
            }
            else -> null
        }
    }

    /** A .torrent from a content:// address (file managers, the download list, the picker). */
    fun readTorrent(context: Context, uri: Uri): DownloadRequest? {
        if (uri.scheme != "content") {
            AppLog.warn("Downloads", "Ignored a file that wasn't shared through a content:// address")
            return null
        }
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // Read one byte past the limit, so an oversized file is noticed without reading it all.
                val buffer = ByteArray(DownloadProtocol.MAX_TORRENT_BYTES + 1)
                var total = 0
                while (total < buffer.size) {
                    val read = input.read(buffer, total, buffer.size - total)
                    if (read < 0) break
                    total += read
                }
                buffer.copyOf(total)
            }
        } catch (e: Exception) {
            AppLog.error("Downloads", "Could not read the torrent file", e)
            null
        } ?: return null
        val request = DownloadProtocol.torrentFile(bytes, displayName(context, uri))
        if (request == null) {
            AppLog.warn("Downloads", "That file isn't a torrent (or is over 2 MB)")
        }
        return request
    }

    private fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }
}
