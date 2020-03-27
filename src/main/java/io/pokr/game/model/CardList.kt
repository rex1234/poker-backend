package io.pokr.game.model

class CardList(
    val cards: List<Card> = mutableListOf()
) {

    fun with(cardList: CardList) =
        CardList(cardList.cards + cards)
}