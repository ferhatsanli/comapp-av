package com.ferhatsanli.comapp

/**
 * Reads a pasted list of device names for "On the network": one MAC address and a name per
 * line, in whatever shape it was kept, for example a Python dict
 *
 *     "48:D3:43:4A:1D:60": "JVD37-Modem",
 *
 * or plain `48-D3-43-4A-1D-60 = JVD37-Modem` / `48:d3:43:4a:1d:60, JVD37-Modem`. Lines without a
 * MAC (braces, comments, blank lines) are skipped. MACs come out in the scan's form,
 * AA-BB-CC-DD-EE-FF, and a MAC listed twice keeps its last name.
 */
internal object DeviceNames {
    const val MAX_NAME_LENGTH = 60

    private val LINE = Regex(
        """^\s*["']?([0-9A-Fa-f]{1,2}(?:[:-][0-9A-Fa-f]{1,2}){5})["']?\s*[:=,\t ]\s*["']?(.*?)["']?\s*,?\s*$""",
    )

    fun parse(text: String): Map<String, String> {
        val names = LinkedHashMap<String, String>()
        for (line in text.lines()) {
            val match = LINE.find(line) ?: continue
            val mac = normalizeMac(match.groupValues[1])
            val name = match.groupValues[2].trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH)
            if (name.isEmpty()) continue
            names.remove(mac) // a later line wins, and takes the later position
            names[mac] = name
        }
        return names
    }

    /** "0:1a:2B:3c:4D:5e" -> "00-1A-2B-3C-4D-5E". */
    fun normalizeMac(mac: String): String =
        mac.split(':', '-').joinToString("-") { it.padStart(2, '0').uppercase() }
}
