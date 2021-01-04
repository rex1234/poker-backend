package io.pokr.game.tools

import io.pokr.game.model.*
import handeval.Hand as EvalHand

class HandComparator {

    companion object {

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
            get() = cards.sortedBy { it.value }.zipWithNext { a, b -> b.value.ordinal - a.value.ordinal }
                .all { it == 1 } ||
                    hasStraighWithA

        val CardList.hasStraighWithA
            get() = cards.map { it.value }.containsAll(
                listOf(Card.Value.ACE, Card.Value.N_2, Card.Value.N_3, Card.Value.N_4, Card.Value.N_5)
            )

        val CardList.hasStraightFlush
            get() = hasStraight && hasFlush

        fun CardList.hasNSameCards(n: Int) =
            cards.groupBy { it.value }.any { it.value.count() == n }

        val CardList.highestHand
            get() = Hand.values().map { hand -> hand.evalFunction(this) }.lastIndexOf(true)

        val CardList.handValue
            get() = EvalHand.evaluate(EvalHand.fromString(toString().replace("0", "T")))
    }

    enum class Hand(
        val handName: String,
        val evalFunction: (CardList) -> Boolean,
    ) {
        HIGH("High card", { true }),
        PAIR("Pair", { it.hasPair }),
        TWO_PAIR("Two pairs", { it.hasTwoPairs }),
        THREE_KIND("Three of a kind", { it.hasThreeKind }),
        STRAIGHT("Straight", { it.hasStraight }),
        FLUSH("Flush", { it.hasFlush }),
        FULL_HOUSE("Full house", { it.hasFullHouse }),
        FOUR_KIND("Four of a kind", { it.hasFourKind }),
        STRAIGHT_FLUSH("Straight flush", { it.hasStraightFlush }),
    }

    class CardCombination(
        val playerCards: CardList,
        val tableCards: CardList,
    ) {

        val all
            get() = playerCards.with(tableCards)
    }

    class PlayerHandComparisonResult(
        var rank: Int,
        var player: Player,
        var hand: Hand? = null,
        var bestCards: CardCombination? = null,
        var winnings: Int = 0,
    )

    fun findHighestHand(cardList: CardList) =
        Hand.values()[cardList.highestHand]

    fun compareHands(c1: CardList, c2: CardList) =
        c2.handValue - c1.handValue

    fun findHighestHand(playerCards: CardList, tableCards: CardList) =
        getAllCardCombinations(playerCards, tableCards).minBy {
            it.all.handValue
        }!!.let {
            findHighestHand(it.all)
        }

    fun evalPlayers(players: List<Player>, tableCards: CardList) =
        players.map { player ->
            getAllCardCombinations(player.cards, tableCards).minBy {
                it.all.handValue
            }!!.let {
                PlayerHandComparisonResult(
                    rank = if (player.action == PlayerAction.Action.FOLD)
                        Integer.MAX_VALUE
                    else
                        it.all.handValue,
                    player = player,
                    hand = findHighestHand(it.all),
                    bestCards = it
                )
            }
        }.sortedBy { it.rank }

    // returns all possible combinations by merging players cards with the cards on the table
    private fun getAllCardCombinations(playerCards: CardList, tableCards: CardList): List<CardCombination> {
        val allCards = playerCards.with(tableCards).cards

        val cardsToDiscard = allCards.size - 5

        if (cardsToDiscard == 0) {
            return listOf(CardCombination(playerCards, tableCards))
        } else {

            val combinations =
                if (cardsToDiscard == 1) {
                    (0..5).map { allCards - allCards[it] }
                } else {
                    (0..6).distinctPairs()
                        .map {
                            allCards - allCards[it.first] - allCards[it.second]
                        }
                }

            return combinations.map {
                val discardedCards = allCards - it

                CardCombination(
                    CardList(playerCards.cards - discardedCards),
                    CardList(tableCards.cards - discardedCards)
                )
            }
        }
    }

    private fun IntRange.distinctPairs() =
        flatMap { i -> map { j -> Pair(i, j) } }.filter { it.first != it.second }
}
