package io.pokr.game.model

import io.pokr.game.model.CardList.Companion.toCardList

/**
 * Shuffled stack from which cards can be drawn
 */
class CardStack(
    var stack: CardList,
) {

    val cards
        get() = stack.cards

    companion object {
        fun create() = CardStack(
            Card.Color.values().flatMap { color ->
                Card.Value.values().map { value -> Card(color, value) }
            }.shuffled().toCardList()
        )
    }

    fun drawCards(n: Int = 1) = CardList(
        cards.take(n).also {
            stack = stack.cards.drop(n).toCardList()
        })
}