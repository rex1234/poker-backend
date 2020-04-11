package io.pokr.game.tools

import io.pokr.game.model.CardList
import io.pokr.game.model.Player
import io.pokr.game.model.PlayerAction

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
        var bestCards: Pair<CardList, CardList>? = null
    )

    fun findHighestHand(cardList: CardList) =
        Hand.values()[cardList.highestHand]

    fun compareHands(c1: CardList, c2: CardList) =
        c2.evalHand - c1.evalHand


    private fun getAllCardCombinations(playerCards: CardList, tableCards: CardList): List<Pair<CardList, CardList>> {
        val allCards = playerCards.with(tableCards).cards

        if(allCards.size == 5) {
            return listOf(Pair(playerCards, tableCards))
        } else {
            val discardCards = allCards.size - 5

            val result =
                if(discardCards == 1) {
                    (0..5).map { allCards - allCards[it] }
                } else {
                    (0..6).map { i -> (0..6).map { j -> Pair(i, j) }}.flatten().filter { it.first != it.second }.map {
                        allCards - allCards[it.first] - allCards[it.second]
                    }
                }

            return result.map {
                val losingCards = allCards - it
                Pair(CardList(playerCards.cards - losingCards), CardList(tableCards.cards - losingCards))
            }
        }
    }

    fun findHighestHand(playerCards: CardList, tableCards: CardList) =
        getAllCardCombinations(playerCards, tableCards).minBy {
            it.first.with(it.second).evalHand
        }!!.let {
            findHighestHand(it.first.with(it.second))
        }


    fun evalPlayers(players: List<Player>, tableCards: CardList) =
        players.map { player ->
            getAllCardCombinations(player.cards, tableCards).minBy {
                it.first.with(it.second).evalHand
            }!!.let {
                PlayerHandComparisonResult(
                    rank = if (player.action == PlayerAction.Action.FOLD)
                        Integer.MAX_VALUE
                    else
                        it.first.with(it.second).evalHand,
                    player = player,
                    hand = findHighestHand(it.first.with(it.second)),
                    bestCards = it
                )
            }
        }.sortedBy { it.rank }
}
