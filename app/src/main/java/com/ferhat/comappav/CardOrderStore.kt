package com.ferhat.comappav

import android.content.Context
import java.io.File

/**
 * Keeps [CardOrder] lists in a small file in the app's private storage on the phone
 * (files/card-order.txt). Writes go to a temporary file first and are then renamed over the
 * old one, so a crash mid-write can't leave a half-written order behind.
 */
internal class CardOrderStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, FILE_NAME)

    fun load(): Map<String, List<String>> = try {
        if (file.exists()) CardOrder.decode(file.readText()) else emptyMap()
    } catch (e: Exception) {
        AppLog.warn("Layout", "Could not read the saved card order; using the default order")
        emptyMap()
    }

    fun save(orders: Map<String, List<String>>) {
        try {
            val temp = File(file.parentFile, "$FILE_NAME.tmp")
            temp.writeText(CardOrder.encode(orders))
            if (!temp.renameTo(file)) {
                file.writeText(CardOrder.encode(orders))
                temp.delete()
            }
            AppLog.info("Layout", "Card order saved")
        } catch (e: Exception) {
            AppLog.error("Layout", "Could not save the card order", e)
        }
    }

    private companion object {
        const val FILE_NAME = "card-order.txt"
    }
}
