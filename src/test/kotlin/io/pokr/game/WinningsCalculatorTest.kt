package io.pokr.game

import io.pokr.game.model.*
import io.pokr.game.tools.*
import org.junit.*

class WinningsCalculatorTest {

    @Test
    fun testWinnings() {
        val players =
            listOf(
                HandComparator.PlayerHandComparisonResult(100 - 7 , Player("1").apply { currentBet = 300; name = "Adam" }),
                HandComparator.PlayerHandComparisonResult(100 - 10, Player("1").apply { currentBet = 200; name = "Herbert" }),
                HandComparator.PlayerHandComparisonResult(100 - 10, Player("1").apply { currentBet = 150; name = "Bretislav" }),
                HandComparator.PlayerHandComparisonResult(100 - 0 , Player("1").apply { currentBet = 20; name = "Picka" }),
                HandComparator.PlayerHandComparisonResult(100 - 3 , Player("1").apply { currentBet = 40; name = "Kukatko" })
            )

        WinningsCalculator.calculateWinnings(players)

        Assert.assertEquals(
            listOf(100, 355, 255, 0, 0),
            players.map { it.player.chips }
        )
    }

    @Test
    fun testWinnings2() {
        val players =
            listOf(
                HandComparator.PlayerHandComparisonResult(100 - 7 , Player("1").apply { currentBet = 300; name = "Adam" }),
                HandComparator.PlayerHandComparisonResult(100 - 10, Player("1").apply { currentBet = 200; name = "Herbert" })
            )

        WinningsCalculator.calculateWinnings(players)

        Assert.assertEquals(
            listOf(100, 400),
            players.map { it.player.chips }
        )
    }

    @Test
    fun testWinnings3() {
        val players =
            listOf(
                HandComparator.PlayerHandComparisonResult(100 - 7 , Player("1").apply { currentBet = 300; name = "Adam" }),
                HandComparator.PlayerHandComparisonResult(100 - 7 , Player("1").apply { currentBet = 200; name = "Herbert" }),
                HandComparator.PlayerHandComparisonResult(100 - 12, Player("1").apply { currentBet = 150; name = "Bretislav" }),
                HandComparator.PlayerHandComparisonResult(100 - 13, Player("1").apply { currentBet = 20; name = "Picka" }),
                HandComparator.PlayerHandComparisonResult(100 - 12, Player("1").apply { currentBet = 70; name = "Kukatko" })
            )

        WinningsCalculator.calculateWinnings(players)

        Assert.assertEquals(
            listOf(150, 50, 340, 100, 100),
            players.map { it.player.chips }
        )
    }

    @Test
    fun testWinnings4() {
        val players =
            listOf(
                HandComparator.PlayerHandComparisonResult(100 - 10, Player("1").apply { currentBet = 400; name = "Adam" }),
                HandComparator.PlayerHandComparisonResult(100 - 10, Player("1").apply { currentBet = 400; name = "Herbert" }),
                HandComparator.PlayerHandComparisonResult(100 - 9 , Player("1").apply { currentBet = 400; name = "Bretislav" })
            )

        WinningsCalculator.calculateWinnings(players)

        Assert.assertEquals(
            listOf(600, 600, 0),
            players.map { it.player.chips }
        )
    }

    @Test
    fun testWinnings5() {
        val players =
            listOf(
                HandComparator.PlayerHandComparisonResult(100 - 0, Player("1").apply { currentBet = 300; name = "Adam" }),
                HandComparator.PlayerHandComparisonResult(100 - 10, Player("1").apply { currentBet = 200; name = "Herbert" }),
                HandComparator.PlayerHandComparisonResult(100 - 10 , Player("1").apply { currentBet = 99; name = "John" })
            )

        WinningsCalculator.calculateWinnings(players)

        Assert.assertEquals(
            listOf(101, 350, 148),
            players.map { it.player.chips }
        )
    }

    @Test
    fun testWinnings6() {
        val players =
            listOf(
                HandComparator.PlayerHandComparisonResult(100 - 10, Player("1").apply { currentBet = 400; name = "Adam" }),
                HandComparator.PlayerHandComparisonResult(100 - 9 , Player("1").apply { currentBet = 400; name = "Herbert" }),
                HandComparator.PlayerHandComparisonResult(100 - 9 , Player("1").apply { currentBet = 400; name = "Bretislav" })
            )

        WinningsCalculator.calculateWinnings(players)

        Assert.assertEquals(
            listOf(1200, 0, 0),
            players.map { it.player.chips }
        )
    }

}