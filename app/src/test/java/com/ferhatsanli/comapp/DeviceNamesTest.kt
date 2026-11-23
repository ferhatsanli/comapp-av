package com.ferhatsanli.comapp

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceNamesTest {
    @Test
    fun readsAPythonDictAsPasted() {
        val pasted = """
            mac_to_device = {
            	"48:D3:43:4A:1D:60": "JVD37-Modem",
            	"B8:27:EB:75:27:A0": "RPi",
             	"2E:F0:BC:99:B1:9B": "Ferhat-S25",
            	"B0:E4:D5:BF:65:BE": "Living Room TV",
            	"86:AC:94:A9:43:58": "Unknown-Viet/Ashish",
            	
            }
        """.trimIndent()
        assertEquals(
            mapOf(
                "48-D3-43-4A-1D-60" to "JVD37-Modem",
                "B8-27-EB-75-27-A0" to "RPi",
                "2E-F0-BC-99-B1-9B" to "Ferhat-S25",
                "B0-E4-D5-BF-65-BE" to "Living Room TV",
                "86-AC-94-A9-43-58" to "Unknown-Viet/Ashish",
            ),
            DeviceNames.parse(pasted),
        )
    }

    @Test
    fun acceptsOtherShapesAndSkipsTheRest() {
        val names = DeviceNames.parse(
            """
            # my devices
            0:1a:2b:3c:4d:5e = Router
            AA-BB-CC-DD-EE-FF, Printer
            11:22:33:44:55:66	Tab separated
            not a mac: "Nothing"
            22:33:44:55:66:77 ""
            0:1a:2b:3c:4d:5e = Router (renamed)
            """.trimIndent(),
        )
        assertEquals(
            mapOf(
                "AA-BB-CC-DD-EE-FF" to "Printer",
                "11-22-33-44-55-66" to "Tab separated",
                "00-1A-2B-3C-4D-5E" to "Router (renamed)",
            ),
            names,
        )
    }

    @Test
    fun longNamesAreCutToWhatTheRulesAccept() {
        val name = DeviceNames.parse("\"AA:BB:CC:DD:EE:FF\": \"${"x".repeat(80)}\"").values.single()
        assertEquals(DeviceNames.MAX_NAME_LENGTH, name.length)
    }
}
