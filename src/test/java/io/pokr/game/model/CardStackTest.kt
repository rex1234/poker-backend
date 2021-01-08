package io.pokr.game.model

import org.junit.*

class CardStackTest {

    @Test
    fun cardStackCreateTest() {
        val cardStack = CardStack.create()

        println(cardStack.stack)

        Assert.assertEquals(
            cardStack.stack.size,
            52
        )
    }

    @Test
    fun cardStackDrawTest() {
        val cardStack = CardStack.create()

        val firstThreeCards = CardList(cardStack.cards.take(3))
        val taken = cardStack.drawCards(3)

        Assert.assertEquals(
            taken,
            firstThreeCards
        )

        Assert.assertEquals(
            cardStack.stack.size,
            52 - 3
        )

        cardStack.drawCards()

        Assert.assertEquals(
            cardStack.stack.size,
            52 - 3 - 1
        )
    }
}