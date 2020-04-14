package io.pokr.game

import io.pokr.game.model.*
import org.junit.*

class HandParserTest {

    val cardListPair = CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.KING),
        Card(Card.Color.DIAMONDS, Card.Value.N_4),
        Card(Card.Color.DIAMONDS, Card.Value.QUEEN),
        Card(Card.Color.HEARTS, Card.Value.ACE)
    ))

    @Test
    fun parseHandTest() {
        Assert.assertEquals(
            CardList.parse("qc 4DkCQd   AH"),
            cardListPair
        )
    }

    @Test
    fun formatHandTest() {
        Assert.assertEquals(
            "QC KC 4D QD AH",
            cardListPair.toString()
        )
    }
}