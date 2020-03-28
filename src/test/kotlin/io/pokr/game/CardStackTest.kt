package io.pokr.game

import io.pokr.game.model.CardList
import io.pokr.game.model.CardStack
import org.junit.Assert
import org.junit.Test

class CardStackTest {

    @Test
    fun cardStackCreateTest() {
        val cardStack = CardStack.create()

        println(CardList.fromCardStack(cardStack))

        Assert.assertEquals(
            cardStack.stack.size,
            52
        )
    }

    @Test
    fun cardStackDrawTest() {
        val cardStack = CardStack.create()

        val firstThreeCards = CardList(cardStack.stack.take(3))
        val taken = cardStack.takeCards(3)

        Assert.assertEquals(
            taken,
            firstThreeCards
        )

        Assert.assertEquals(
            cardStack.stack.size,
            52 - 3
        )

        cardStack.takeCards()

        Assert.assertEquals(
            cardStack.stack.size,
            52 - 3 - 1
        )
    }
}