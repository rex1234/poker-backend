package io.pokr.game

import io.pokr.game.model.CardList
import io.pokr.game.model.Player
import io.pokr.network.model.PlayerAction

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

        val CardList.allTriplesFromFour
            get() = (0..3).map { i ->
                CardList(cards - cards[i])
            }

        val CardList.allTriplesFromFive
            get() = (0..3).map { i ->
                (i + 1..4).map { j -> Pair(i, j) } // indices of all pairs of cards
            }.flatten(). map {
                CardList(cards - cards[it.first] - cards[it.second]) // subtract the cards from the stack of 5
            }
    }

    enum class Hand(
        val handName: String,
        val evalFunction: (CardList) -> Boolean
    ) {
        HIGH("High card", { true }),
        PAIR("Pair", { it.hasPair }),
        TWO_PAIR("Two pairs", { it.hasTwoPairs }),
        THREE_KIND("Three of a kind", { it.hasThreeKind }),
        STRAIGHT("Straight", { it.hasStraight }),
        FLUSH("Flush", { it.hasFlush }),
        FULL_HOUSE("Full house", { it.hasFullHouse }),
        FOUR_KIND("Four of a kind", { it.hasFourKind }),
        STRAIGHT_FLUSH("Straight flush", { it.hasStraightFlush })
    }

    class PlayerHandComparisonResult(
        var rank: Int,
        var player: Player,
        var hand: Hand? = null,
        var bestCards: CardList? = null
    )

    fun findHighestHand(cardList: CardList) =
        Hand.values()[cardList.highestHand]

    fun compareHands(c1: CardList, c2: CardList) =
        c2.evalHand - c1.evalHand

    fun findHighestHand(playerCards: CardList, tableCards: CardList) =
        when {
            tableCards.cards.size == 3 -> findHighestHand(playerCards.with(tableCards))
            tableCards.cards.size == 4 -> findHighestHand(
                tableCards.allTriplesFromFour.minBy { playerCards.with(it).evalHand }!!.with(playerCards)
            )
            else -> findHighestHand(
                tableCards.allTriplesFromFive.minBy { playerCards.with(it).evalHand }!!.with(playerCards)
            )
        }


    fun evalPlayers(players: List<Player>, tableCards: CardList) =
        players.map { player ->
            tableCards.allTriplesFromFive.minBy { player.cards.with(it).evalHand }!!.let {
                PlayerHandComparisonResult(
                    rank = if(player.action == PlayerAction.Action.FOLD) Integer.MAX_VALUE else player.cards.with(it).evalHand,
                    player = player,
                    hand = findHighestHand(it.with(player.cards)),
                    bestCards = it
                )
            }
        }.sortedBy { it.rank }
}
