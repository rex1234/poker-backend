package io.pokr.game

import io.pokr.game.model.CardList
import org.junit.Assert
import org.junit.Test

class HandComparatorTest {

    val cardListPair = CardList.parse("QC QH 2D 3D AH")
    val cardListTwoPair = CardList.parse("QC QH 4D 2H 4H")
    val cardListTwoPair2 = CardList.parse("KC KH 4D 4H 7H")
    val cardListTwoPair3 = CardList.parse("KC KH 4D 4H AH")
    val cardListThreeKind = CardList.parse("QC QH 4D QD AH")
    val cardListFourKind =  CardList.parse("QC QH QD QS AH")
    val cardListFullHouse = CardList.parse("QC QS QD AD AH")
    val cardListStraight = CardList.parse("0D 8C 9D JH 7C")
    val cardListFlush = CardList.parse("0C QC JC 9C 5C")
    val cardListRoyalFlush = CardList.parse("0C QC JC KC 9C")

    val handComparator = HandComparator()

    @Test
    fun testPair() {
        Assert.assertTrue(HandComparator.Hand.PAIR.evalFunction(cardListPair))
        Assert.assertTrue(handComparator.findHighestHand(cardListPair) == HandComparator.Hand.PAIR)
    }

    @Test
    fun testTwoPair() {
        Assert.assertTrue(HandComparator.Hand.TWO_PAIR.evalFunction(cardListTwoPair))
        Assert.assertTrue(handComparator.findHighestHand(cardListTwoPair) == HandComparator.Hand.TWO_PAIR)
    }

    @Test
    fun testThreeKind() {
        Assert.assertTrue(HandComparator.Hand.THREE_KIND.evalFunction(cardListThreeKind))
        Assert.assertTrue(handComparator.findHighestHand(cardListThreeKind) == HandComparator.Hand.THREE_KIND)
    }

    @Test
    fun testFourKind() {
        Assert.assertTrue(HandComparator.Hand.FOUR_KIND.evalFunction(cardListFourKind))
        Assert.assertTrue(handComparator.findHighestHand(cardListFourKind) == HandComparator.Hand.FOUR_KIND)
    }

    @Test
    fun testFullHouse() {
        Assert.assertTrue(HandComparator.Hand.FULL_HOUSE.evalFunction(cardListFullHouse))
        Assert.assertFalse(HandComparator.Hand.FULL_HOUSE.evalFunction(cardListThreeKind))
        Assert.assertFalse(HandComparator.Hand.FULL_HOUSE.evalFunction(cardListPair))
        Assert.assertTrue(handComparator.findHighestHand(cardListFullHouse) == HandComparator.Hand.FULL_HOUSE)
    }

    @Test
    fun testStraight() {
        Assert.assertTrue(HandComparator.Hand.STRAIGHT.evalFunction(cardListStraight))
        Assert.assertFalse(HandComparator.Hand.STRAIGHT.evalFunction(cardListPair))
        Assert.assertTrue(handComparator.findHighestHand(cardListStraight) == HandComparator.Hand.STRAIGHT)
    }

    @Test
    fun testFlush() {
        Assert.assertTrue(HandComparator.Hand.FLUSH.evalFunction(cardListFlush))
        Assert.assertFalse(HandComparator.Hand.FLUSH.evalFunction(cardListTwoPair))
        Assert.assertTrue(handComparator.findHighestHand(cardListFlush) == HandComparator.Hand.FLUSH)
    }

    @Test
    fun testStraightFlush() {
        Assert.assertTrue(HandComparator.Hand.STRAIGHT_FLUSH.evalFunction(cardListRoyalFlush))
        Assert.assertTrue(handComparator.findHighestHand(cardListRoyalFlush) == HandComparator.Hand.STRAIGHT_FLUSH)
    }

    @Test
    fun handComparisonTest() {
        Assert.assertTrue(handComparator.compareHands(cardListFullHouse, cardListStraight) > 0)
        Assert.assertTrue(handComparator.compareHands(cardListPair, cardListThreeKind) < 0)
        Assert.assertTrue(handComparator.compareHands(cardListTwoPair, cardListStraight) < 0)
        Assert.assertTrue(handComparator.compareHands(cardListStraight, cardListThreeKind) > 0)
        Assert.assertTrue(handComparator.compareHands(cardListRoyalFlush, cardListThreeKind) > 0)
        Assert.assertTrue(handComparator.compareHands(cardListFourKind, cardListFullHouse) > 0)
        Assert.assertTrue(handComparator.compareHands(cardListRoyalFlush, cardListFullHouse) > 0)
    }

    @Test
    fun highCardComparisonTest() {
        Assert.assertTrue(handComparator.compareHands(cardListTwoPair3, cardListTwoPair2) > 0)
        Assert.assertTrue(handComparator.compareHands(cardListTwoPair2, cardListTwoPair) > 0)
        Assert.assertTrue(handComparator.compareHands(cardListTwoPair3, cardListTwoPair) > 0)
    }

    @Test
    fun highestHandFromTableOf3() {
        val playerCards = CardList.parse("2C 5H")
        val tableCards = CardList.parse("2D 3C 2H")

        Assert.assertEquals(HandComparator.Hand.THREE_KIND, handComparator.findHighestHand(playerCards, tableCards))

        val playerCards2 = CardList.parse("2C 5H")
        val tableCards2 = CardList.parse("2D 5C 2H")

        Assert.assertEquals(HandComparator.Hand.FULL_HOUSE, handComparator.findHighestHand(playerCards2, tableCards2))
    }

    @Test
    fun highestHandFromTableOf4() {
        val playerCards = CardList.parse("2C 5H")
        val tableCards = CardList.parse("2D 3C 2H 2S")

        Assert.assertEquals(HandComparator.Hand.FOUR_KIND, handComparator.findHighestHand(playerCards, tableCards))

        val playerCards2 = CardList.parse("2C 3H")
        val tableCards2 = CardList.parse("2D 3C 2H 5S")

        Assert.assertEquals(HandComparator.Hand.FULL_HOUSE, handComparator.findHighestHand(playerCards2, tableCards2))
    }

    @Test
    fun highestHandFromTableOf5() {
        val playerCards = CardList.parse("2C AH")
        val tableCards = CardList.parse("2D 3C 2H KC 2S")

        Assert.assertEquals(HandComparator.Hand.FOUR_KIND, handComparator.findHighestHand(playerCards, tableCards))

        val playerCards2 = CardList.parse("2C QC")
        val tableCards2 = CardList.parse("KC 3C 2H AC")

        Assert.assertEquals(HandComparator.Hand.FLUSH, handComparator.findHighestHand(playerCards2, tableCards2))
    }

}