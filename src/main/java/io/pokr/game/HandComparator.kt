package io.pokr.game

import io.pokr.game.model.CardList
import io.pokr.game.model.Player

import handeval.Hand as EvalHand

class HandComparator {

    companion object {

        // TODO: take the highest card outside of the combination
        val CardList.highCard
            get() = cards.maxBy { it.value.ordinal }!!

        val CardList.hasPair
            get() = hasNSameCards(2)

        val CardList.hasTwoPairs
            get() = cards.groupBy { it.value }.filter { it.value.count() == 2 }.count() == 2

        val CardList.hasThreeKind
            get() = hasNSameCards(3)

        val CardList.hasFullHouse
            get() = hasPair && hasThreeKind

        val CardList.hasFourKind
            get() = hasNSameCards(4)

        val CardList.hasFlush
            get() = cards.groupBy { it.color }.count() == 1

        val CardList.hasStraight
            get() = cards.sortedBy { it.value }.zipWithNext { a, b -> b.value.ordinal - a.value.ordinal }.all { it == 1 }

        val CardList.hasStraightFlush
            get() = hasStraight && hasFlush

        fun CardList.hasNSameCards(n: Int) =
            cards.groupBy { it.value }.any { it.value.count() == n }

        val CardList.highestHand
            get() = Hand.values().map { hand -> hand.evalFunction(this) }.lastIndexOf(true)

        val CardList.evalHand
            get() = EvalHand.evaluate(EvalHand.fromString(toString().replace("0", "T")))

        val CardList.allTriples
            get() = (0..3).map { i ->
                (i + 1..4).map { j -> Pair(i, j) } // indices of all pairs of cards
            }.flatten(). map {
                CardList(cards - cards[it.first] - cards[it.second]) // substract the cards from the stack of 5
            }
    }

    enum class Hand(
        val evalFunction: (CardList) -> Boolean
    ) {
        HIGH({ true }),
        PAIR({ it.hasPair }),
        TWO_PAIR({ it.hasTwoPairs }),
        THREE_KIND({ it.hasThreeKind }),
        STRAIGHT({ it.hasStraight }),
        FLUSH({ it.hasFlush }),
        FULL_HOUSE({ it.hasFullHouse }),
        FOUR_KIND({ it.hasFourKind }),
        STRAIGHT_FLUSH({ it.hasStraightFlush })
    }

    class PlayerHandComparisonResult(
        val rank: Int,
        val player: Player,
        val hand: Hand,
        val bestCards: CardList
    )

    fun findHighestHand(cardList: CardList) =
        Hand.values()[cardList.highestHand]

    fun compareHands(c1: CardList, c2: CardList) =
        c2.evalHand - c1.evalHand

    fun evalPlayers(players: List<Player>, tableCards: CardList) =
        players.map { player ->
            tableCards.allTriples.maxBy { player.cards.with(it).evalHand }!!.let {
                PlayerHandComparisonResult(
                    rank = player.cards.with(it).evalHand,
                    player = player,
                    hand = findHighestHand(it.with(player.cards)),
                    bestCards = it
                )
            }
        }.sortedByDescending { it.rank }
}
