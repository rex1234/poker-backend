package io.pokr.game

import io.pokr.game.model.Card
import io.pokr.game.model.CardList
import org.junit.Assert
import org.junit.Test

class HandComparatorTest {

    val cardListPair = CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.KING),
        Card(Card.Color.DIAMONDS, Card.Value.N_4),
        Card(Card.Color.DIAMONDS, Card.Value.QUEEN),
        Card(Card.Color.HEARTS, Card.Value.ACE)
    ))

    val cardListTwoPair = CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.DIAMONDS, Card.Value.N_4),
        Card(Card.Color.DIAMONDS, Card.Value.N_4),
        Card(Card.Color.HEARTS, Card.Value.ACE)
    ))

    val cardListThreeKind = CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.DIAMONDS, Card.Value.N_4),
        Card(Card.Color.DIAMONDS, Card.Value.QUEEN),
        Card(Card.Color.HEARTS, Card.Value.ACE)
    ))

    val cardListFourKind =  CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.DIAMONDS, Card.Value.QUEEN),
        Card(Card.Color.DIAMONDS, Card.Value.QUEEN),
        Card(Card.Color.HEARTS, Card.Value.ACE)
    ))

    val cardListFullHouse = CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.DIAMONDS, Card.Value.QUEEN),
        Card(Card.Color.DIAMONDS, Card.Value.ACE),
        Card(Card.Color.HEARTS, Card.Value.ACE)
    ))

    val cardListStraight = CardList(listOf(
        Card(Card.Color.DIAMONDS, Card.Value.N_10),
        Card(Card.Color.CLUBS, Card.Value.N_8),
        Card(Card.Color.DIAMONDS, Card.Value.N_9),
        Card(Card.Color.HEARTS, Card.Value.JACK),
        Card(Card.Color.CLUBS, Card.Value.N_7)
    ))

    val cardListFlush = CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.N_10),
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.JACK),
        Card(Card.Color.CLUBS, Card.Value.N_9),
        Card(Card.Color.CLUBS, Card.Value.N_9)
    ))

    val cardListRoyalFlush = CardList(listOf(
        Card(Card.Color.CLUBS, Card.Value.N_10),
        Card(Card.Color.CLUBS, Card.Value.QUEEN),
        Card(Card.Color.CLUBS, Card.Value.JACK),
        Card(Card.Color.CLUBS, Card.Value.KING),
        Card(Card.Color.CLUBS, Card.Value.N_9)
    ))

    val handComparator = HandComparator()

    @Test
    fun testPair() {
        Assert.assertTrue(HandComparator.Hands.PAIR.evalFunction(cardListPair))
        Assert.assertTrue(handComparator.evaluateHand(cardListPair) == HandComparator.Hands.PAIR)
    }

    @Test
    fun testTwoPair() {
        Assert.assertTrue(HandComparator.Hands.TWO_PAIR.evalFunction(cardListTwoPair))
        Assert.assertTrue(handComparator.evaluateHand(cardListTwoPair) == HandComparator.Hands.TWO_PAIR)
    }

    @Test
    fun testThreeKind() {
        Assert.assertTrue(HandComparator.Hands.THREE_KIND.evalFunction(cardListThreeKind))
        Assert.assertTrue(handComparator.evaluateHand(cardListThreeKind) == HandComparator.Hands.THREE_KIND)
    }

    @Test
    fun testFourKind() {
        Assert.assertTrue(HandComparator.Hands.FOUR_KIND.evalFunction(cardListFourKind))
        Assert.assertTrue(handComparator.evaluateHand(cardListFourKind) == HandComparator.Hands.FOUR_KIND)
    }

    @Test
    fun testFullHouse() {
        Assert.assertTrue(HandComparator.Hands.FULL_HOUSE.evalFunction(cardListFullHouse))
        Assert.assertFalse(HandComparator.Hands.FULL_HOUSE.evalFunction(cardListThreeKind))
        Assert.assertFalse(HandComparator.Hands.FULL_HOUSE.evalFunction(cardListPair))
        Assert.assertTrue(handComparator.evaluateHand(cardListFullHouse) == HandComparator.Hands.FULL_HOUSE)
    }

    @Test
    fun testStraight() {
        Assert.assertTrue(HandComparator.Hands.STRAIGHT.evalFunction(cardListStraight))
        Assert.assertFalse(HandComparator.Hands.STRAIGHT.evalFunction(cardListPair))
        Assert.assertTrue(handComparator.evaluateHand(cardListStraight) == HandComparator.Hands.STRAIGHT)
    }

    @Test
    fun testFlush() {
        Assert.assertTrue(HandComparator.Hands.FLUSH.evalFunction(cardListFlush))
        Assert.assertFalse(HandComparator.Hands.FLUSH.evalFunction(cardListTwoPair))
        Assert.assertTrue(handComparator.evaluateHand(cardListFlush) == HandComparator.Hands.FLUSH)
    }

    @Test
    fun testStraightFlush() {
        Assert.assertTrue(HandComparator.Hands.STRAIGHT_FLUSH.evalFunction(cardListRoyalFlush))
        Assert.assertTrue(handComparator.evaluateHand(cardListRoyalFlush) == HandComparator.Hands.STRAIGHT_FLUSH)
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

    }
}