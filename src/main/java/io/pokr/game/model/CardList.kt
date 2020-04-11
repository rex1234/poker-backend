package io.pokr.game.model

/**
 * Class encapsulating a list of cards. Can be parsed or formatted to a String:
 * the string contains pairs of letters(card val, card color).
 */
class CardList(
    val cards: List<Card> = mutableListOf()
) {

    companion object {
        fun parse(code: String) =
            CardList(
                code.replace(" ", "").chunked(2).map { Card.parse(it) }
            )

        fun fromCardStack(cardStack: CardStack) =
            CardList(cardStack.stack)
    }

    val size
        get() = cards.size

    fun with(cardList: CardList) =
        CardList(cardList.cards + cards)

    override fun equals(other: Any?): Boolean {
        return other is CardList && other.cards.containsAll(cards) && other.cards.size == cards.size
    }

    override fun toString() =
        cards.joinToString(" ")
}