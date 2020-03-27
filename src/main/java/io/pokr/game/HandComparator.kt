package io.pokr.game

import io.pokr.game.model.CardList

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
            get() = Hands.values().map { hand -> hand.evalFunction(this) }.lastIndexOf(true)
    }

    enum class Hands(
        val evalFunction: (CardList) -> Boolean
    ) {
        HIGH ({ true }),
        PAIR({ it.hasPair }),
        TWO_PAIR({ it.hasTwoPairs }),
        THREE_KIND({ it.hasThreeKind }),
        STRAIGHT({ it.hasStraight }),
        FLUSH({ it.hasFlush }),
        FULL_HOUSE({ it.hasFullHouse }),
        FOUR_KIND({ it.hasFourKind }),
        STRAIGHT_FLUSH({ it.hasStraightFlush })
    }

    fun evaluateHand(cardList: CardList) =
        Hands.values()[cardList.highestHand]

    fun compareHands(c1: CardList, c2: CardList): Int {
        val c1Combination = c1.highestHand
        val c2Combination = c2.highestHand

        return if(c1Combination == c2Combination) {
            // TODO: compare cards in same hands
            c1.highCard.value.ordinal - c2.highCard.value.ordinal
        } else {
            c1Combination - c2Combination
        }
    }
}
