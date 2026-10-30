package com.ferhat.comappav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadProtocolTest {
    @Test
    fun aSharedPageTitlePlusLinkYieldsTheLink() {
        val found = DownloadProtocol.findLink("Ubuntu 26.04 LTS https://releases.ubuntu.com/26.04/ubuntu-26.04-desktop-amd64.iso")!!
        assertEquals(DownloadProtocol.SOURCE_URL, found.source)
        assertEquals("https://releases.ubuntu.com/26.04/ubuntu-26.04-desktop-amd64.iso", found.url)
        assertEquals("ubuntu-26.04-desktop-amd64.iso", found.displayName)
    }

    @Test
    fun trailingPunctuationFromASentenceIsNotPartOfTheLink() {
        assertEquals("https://x.org/a.iso", DownloadProtocol.findLink("Get it here: https://x.org/a.iso).")!!.url)
    }

    @Test
    fun aMagnetLinkIsRecognisedAndNamedFromItsDn() {
        val magnet = "magnet:?xt=urn:btih:ABCDEF0123456789ABCDEF0123456789ABCDEF01&dn=Fedora%20Workstation%2041&tr=udp%3A%2F%2Ft"
        val found = DownloadProtocol.findLink("look $magnet")!!
        assertEquals(DownloadProtocol.SOURCE_MAGNET, found.source)
        assertEquals(magnet, found.url)
        assertEquals("Fedora Workstation 41", found.displayName)
    }

    @Test
    fun aMagnetWithoutAnInfoHashIsNotAccepted() {
        assertNull(DownloadProtocol.findLink("magnet:?dn=just-a-name"))
    }

    @Test
    fun thingsThatAreNotLinksAreNotAccepted() {
        assertNull(DownloadProtocol.findLink(null))
        assertNull(DownloadProtocol.findLink("no link here"))
        assertNull(DownloadProtocol.findLink("ftp://x.org/a.iso"))
        assertNull(DownloadProtocol.findLink("https://x.org/" + "a".repeat(4100)))
    }

    @Test
    fun linkWithoutAFileNameIsNamedAfterItsHost() {
        assertEquals("download.example.com", DownloadProtocol.findLink("https://download.example.com/?id=4")!!.displayName)
        assertEquals("a b.iso", DownloadProtocol.findLink("https://x.org/a%20b.iso?mirror=2#top")!!.displayName)
    }

    @Test
    fun aTorrentFileIsEncodedForUpload() {
        val bytes = "d4:infod4:name1:xee".toByteArray()
        val request = DownloadProtocol.torrentFile(bytes, "fedora.torrent")!!
        assertEquals(DownloadProtocol.SOURCE_TORRENT_FILE, request.source)
        assertNull(request.url)
        assertEquals("ZDQ6aW5mb2Q0Om5hbWUxOnhlZQ==", request.torrentBase64)
        assertEquals("fedora.torrent", request.displayName)
        assertEquals(mapOf("torrent" to "ZDQ6aW5mb2Q0Om5hbWUxOnhlZQ=="), DownloadProtocol.upload(request))
    }

    @Test
    fun filesThatCannotBeTorrentsAreRejected() {
        assertNull(DownloadProtocol.torrentFile(ByteArray(0), "x"))
        assertNull(DownloadProtocol.torrentFile("<html>".toByteArray(), "x.torrent"))
        assertNull(DownloadProtocol.torrentFile(ByteArray(DownloadProtocol.MAX_TORRENT_BYTES + 1) { 'd'.code.toByte() }, "x"))
    }

    @Test
    fun commandsMatchTheRules() {
        val link = DownloadProtocol.findLink("https://x.org/a.iso")!!
        assertEquals(
            mapOf(
                "action" to "DOWNLOAD_ADD", "senderUid" to "me", "targetUid" to PingProtocol.PI_UID,
                "status" to "PENDING", "source" to "URL", "url" to "https://x.org/a.iso",
            ),
            DownloadProtocol.command("me", link),
        )
        val torrent = DownloadProtocol.torrentFile("d4:infod4:name1:xee".toByteArray(), null)!!
        assertEquals(null, DownloadProtocol.command("me", torrent)["url"])
        assertEquals("TORRENT_FILE", DownloadProtocol.command("me", torrent)["source"])
        assertNull(DownloadProtocol.upload(link))
    }

    @Test
    fun everyErrorCodeTheAgentSendsHasAMessage() {
        for (code in listOf("NOT_A_FILE", "NO_SPACE", "BAD_INPUT", "ENGINE_DOWN")) {
            assert(DownloadProtocol.describeError(code) != null) { code }
        }
        assertNull(DownloadProtocol.describeError("AGENT_ERROR"))
    }

    @Test
    fun everyPerDownloadErrorTheAgentReportsReadsAsASentence() {
        for (code in listOf("NETWORK", "TIMEOUT", "HTTP_ERROR", "DISK")) {
            assert(DownloadProtocol.describeItemError(code).endsWith(".")) { code }
        }
        assertEquals("Torrent error (aria2 code 3).", DownloadProtocol.describeItemError("ARIA2_3"))
        assertEquals("Download error (curl code 35).", DownloadProtocol.describeItemError("CURL_35"))
        assertEquals("Failed.", DownloadProtocol.describeItemError(null))
    }

    @Test
    fun sizesSpeedsAndTimesAreReadable() {
        assertEquals("0 B", DownloadProtocol.formatBytes(0))
        assertEquals("512 B", DownloadProtocol.formatBytes(512))
        assertEquals("1.5 KB", DownloadProtocol.formatBytes(1536))
        assertEquals("6.0 GB", DownloadProtocol.formatBytes(6L * 1024 * 1024 * 1024))
        assertEquals("2.4 MB/s", DownloadProtocol.formatSpeed(2_516_582))
        assertEquals("45 s", DownloadProtocol.formatEta(45))
        assertEquals("12 min", DownloadProtocol.formatEta(12 * 60 + 5))
        assertEquals("1 h 5 min", DownloadProtocol.formatEta(3900))
    }

    @Test
    fun limitSettingIsReadDefensively() {
        assertEquals(20, DownloadProtocol.parseLimit(null))
        assertEquals(50, DownloadProtocol.parseLimit(50L))
        assertEquals(0, DownloadProtocol.parseLimit(0L))
        assertEquals(20, DownloadProtocol.parseLimit(2.5))
        assertEquals(20, DownloadProtocol.parseLimit(-1L))
        assertEquals(20, DownloadProtocol.parseLimit("50"))
        assertEquals("Unlimited", DownloadProtocol.limitLabel(0))
        assertEquals("20 Mbit/s", DownloadProtocol.limitLabel(20))
    }
}
