package io.pokr.game

import io.pokr.game.model.CardList
import io.pokr.game.model.CardStack
import io.pokr.game.model.GameConfig
import io.pokr.game.model.PlayerAction
import org.junit.After
import org.junit.Before
import org.junit.Test

class HoldemTournamentGameEngineTest {

    lateinit var engine: HoldemTournamentGameEngine

    @Before
    fun init() {
        engine = HoldemTournamentGameEngine("1", {}, {})

        engine.initGame(GameConfig(
            25000,
            20,
            720,
            12,
            720
        ))

        engine.mockCardStack = CardStack(listOf())

        engine.addPlayer("P1")
        engine.addPlayer("P2")
        engine.addPlayer("P3")
    }

    @After
    fun destroy() {
        engine.finishGame()
    }

    @Test
    fun testAllIn() {
        engine.mockCardStack = CardStack(
            CardList.parse("QCQH KCKH 5C6C  2H3D4H 8C 9D").cards
        )

        engine.startGame("P1")

        printState()

        engine.nextPlayerMove("P2", PlayerAction(PlayerAction.Action.CALL, null, null))
        engine.nextPlayerMove("P3", PlayerAction(PlayerAction.Action.FOLD, null, null))
        engine.nextPlayerMove("P1", PlayerAction(PlayerAction.Action.CHECK, null, null))

        printState()
    }

    fun printState() {
        System.err.println(engine.game.allPlayers.map {
            (if (it.isOnMove) "*" else "") + "[${it.uuid}] [${it.cards}] Bet: ${it.currentBet}   "
        })
    }

}