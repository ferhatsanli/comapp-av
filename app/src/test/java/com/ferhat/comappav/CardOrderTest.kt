package com.ferhat.comappav

import org.junit.Assert.assertEquals
import org.junit.Test

class CardOrderTest {
    private enum class Card { A, B, C }

    @Test
    fun savedOrdersSurviveARoundTrip() {
        val orders = mapOf("POWER" to listOf("SHUTDOWN", "SLEEP"), "categories" to listOf("NETWORK", "COMMANDS"))
        assertEquals(orders, CardOrder.decode(CardOrder.encode(orders)))
    }

    @Test
    fun nothingSavedMeansTheDefaultOrder() {
        assertEquals(Card.entries, CardOrder.apply(null, Card.entries) { it.name })
    }

    @Test
    fun theSavedOrderIsUsed() {
        assertEquals(listOf(Card.C, Card.A, Card.B), CardOrder.apply(listOf("C", "A", "B"), Card.entries) { it.name })
    }

    // An app update can add or remove cards; a saved order must neither lose new cards
    // nor bring back ones that no longer exist.
    @Test
    fun newCardsAreAppendedAndRemovedOnesAreDropped() {
        assertEquals(
            listOf(Card.C, Card.A, Card.B),
            CardOrder.apply(listOf("C", "GONE", "A"), Card.entries) { it.name },
        )
    }

    @Test
    fun duplicatesInAHandEditedOrBrokenFileDoNotDuplicateCards() {
        assertEquals(listOf(Card.B, Card.A, Card.C), CardOrder.apply(listOf("B", "B", "A"), Card.entries) { it.name })
    }

    @Test
    fun garbageLinesAreIgnored() {
        assertEquals(
            mapOf("POWER" to listOf("SLEEP")),
            CardOrder.decode("not a line\n=NOKEY\nPOWER=SLEEP,\n\n"),
        )
    }
}
