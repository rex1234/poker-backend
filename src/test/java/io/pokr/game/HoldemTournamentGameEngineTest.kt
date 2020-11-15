package io.pokr.game

import io.pokr.game.exceptions.*
import io.pokr.game.model.*
import org.junit.*

class HoldemTournamentGameEngineTest {

    private val startingSmallBlind = 10
    private val startingBigBlind = startingSmallBlind * 2

    private fun initEngine(adminUuid: String = "admin", nrOfPlayers: Int = 5) : HoldemTournamentGameEngine {
        val engine = HoldemTournamentGameEngine(
            gameUuid = "engine",
            gameConfig = GameConfig(2500, startingSmallBlind, 10, 60, 30, 2),
            updateStateListener = {},
            gameFinishedListener = {},
            playerKickedListener = { _, _ -> }
        )
        if (nrOfPlayers > 0) {
            engine.addPlayer(adminUuid)
        }
        for (i in 2..nrOfPlayers) {
            engine.addPlayer("player$i")
        }
        return engine
    }

    private fun initEngineAndStart(adminUuid: String = "admin", nrOfPlayers: Int = 5) : HoldemTournamentGameEngine {
        val engine = initEngine(adminUuid, nrOfPlayers)
        engine.startGame(adminUuid)
        return engine
    }

    @Test
    fun startGameTest() {
        val engine = initEngine("admin")
        Assert.assertEquals(GameData.State.CREATED, engine.gameData.gameState)

        engine.startGame("admin")
        Assert.assertEquals(GameData.State.ACTIVE, engine.gameData.gameState)
        Assert.assertEquals(startingBigBlind, engine.gameData.targetBet)
    }

    @Test
    fun foldTest() {
        val engine = initEngineAndStart(nrOfPlayers = 5)

        Assert.assertEquals(5, engine.gameData.players.size)
        Assert.assertEquals(5, engine.gameData.activePlayers.size)

        val currentPlayer = engine.gameData.currentPlayerOnMove
        engine.nextPlayerMove(currentPlayer.uuid, PlayerAction(PlayerAction.Action.FOLD))

        Assert.assertEquals(5, engine.gameData.players.size)
        Assert.assertEquals(4, engine.gameData.activePlayers.size)
    }

    @Test(expected = GameException::class)
    fun notEnoughChipsRaiseTest() {
        val engine = initEngineAndStart()

        val currentPlayer = engine.gameData.currentPlayerOnMove
        currentPlayer.chips = 200

        val playerAction = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = 300
        )
        engine.nextPlayerMove(currentPlayer.uuid, playerAction)
    }

    @Test(expected = GameException::class)
    fun notFullRaiseTest() {
        val engine = initEngineAndStart()

        val currentPlayer = engine.gameData.currentPlayerOnMove

        val playerAction = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = 1
        )
        engine.nextPlayerMove(currentPlayer.uuid, playerAction)
    }

    @Test
    fun notFullRaiseButAllInTest() {
        val engine = initEngineAndStart()

        val currentPlayer = engine.gameData.currentPlayerOnMove
        currentPlayer.currentBet = 99
        currentPlayer.chips = 100

        val playerAction = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = 1
        )
        engine.nextPlayerMove(currentPlayer.uuid, playerAction)
        Assert.assertEquals(true, currentPlayer.isAllIn)
    }

    @Test
    fun nextPlayerMovePropChangeTest() {
        val engine = initEngineAndStart(nrOfPlayers = 9)
        Assert.assertEquals(9, engine.gameData.activePlayers.size)

        engine.gameData.activePlayers.forEach {
            Assert.assertEquals(true, it.pendingAction)
            Assert.assertEquals(true, it.canRaise)
        }

        val utg = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(PlayerAction.Action.NONE, utg.action)

        engine.nextPlayerMove(utg.uuid, PlayerAction(PlayerAction.Action.CALL))
        Assert.assertEquals(false, utg.pendingAction)
        Assert.assertEquals(false, utg.canRaise)
        Assert.assertEquals(PlayerAction.Action.CALL, utg.action)
        (engine.gameData.activePlayers - utg).forEach {
            Assert.assertEquals(true, it.pendingAction)
            Assert.assertEquals(true, it.canRaise)
        }

        // UTG+1 makes a full raise
        val utgPlus1 = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(PlayerAction.Action.NONE, utgPlus1.action)

        val utgPlus1action = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = startingBigBlind * 2
        )
        engine.nextPlayerMove(utgPlus1.uuid, utgPlus1action)
        Assert.assertEquals(false, utgPlus1.pendingAction)
        Assert.assertEquals(false, utgPlus1.canRaise)
        Assert.assertEquals(PlayerAction.Action.RAISE, utgPlus1.action)
        (engine.gameData.activePlayers - utgPlus1).forEach {
            Assert.assertEquals(true, it.pendingAction)
            Assert.assertEquals(true, it.canRaise)
        }
        Assert.assertEquals(utgPlus1action.numericValue, engine.gameData.targetBet)

        // UTG+2 goes all in (not a full raise)
        val utgPlus2 = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(PlayerAction.Action.NONE, utgPlus2.action)

        utgPlus2.chips = engine.gameData.targetBet + (startingBigBlind / 2)
        val utgPlus2action = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = utgPlus2.chips
        )
        engine.nextPlayerMove(utgPlus2.uuid, utgPlus2action)
        Assert.assertEquals(true, utgPlus2.isAllIn)
        Assert.assertEquals(false, utgPlus2.pendingAction)
        Assert.assertEquals(false, utgPlus2.canRaise)
        Assert.assertEquals(PlayerAction.Action.RAISE, utgPlus2.action)

        // UTG+1 can't raise again
        Assert.assertEquals(true, utgPlus1.pendingAction)
        Assert.assertEquals(false, utgPlus1.canRaise)

        Assert.assertEquals(8, engine.gameData.activePlayers.size)
        (engine.gameData.activePlayers - utgPlus1).forEach {
            Assert.assertEquals(true, it.pendingAction)
            Assert.assertEquals(true, it.canRaise)
        }

        Assert.assertEquals(utgPlus2action.numericValue, engine.gameData.targetBet)

        // UTG+3 goes all in (not a full raise)
        val utgPlus3 = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(PlayerAction.Action.NONE, utgPlus3.action)

        utgPlus3.chips = engine.gameData.targetBet + (startingBigBlind / 2) - 1
        val utgPlus3action = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = utgPlus3.chips
        )
        engine.nextPlayerMove(utgPlus3.uuid, utgPlus3action)
        Assert.assertEquals(true, utgPlus3.isAllIn)
        Assert.assertEquals(false, utgPlus3.pendingAction)
        Assert.assertEquals(false, utgPlus3.canRaise)
        Assert.assertEquals(PlayerAction.Action.RAISE, utgPlus3.action)

        // UTG+1 still can't raise
        Assert.assertEquals(true, utgPlus1.pendingAction)
        Assert.assertEquals(false, utgPlus1.canRaise)

        Assert.assertEquals(7, engine.gameData.activePlayers.size)
        (engine.gameData.activePlayers - utgPlus1).forEach {
            Assert.assertEquals(true, it.pendingAction)
            Assert.assertEquals(true, it.canRaise)
        }

        Assert.assertEquals(utgPlus3action.numericValue, engine.gameData.targetBet)

        // UTG+4 calls
        val utgPlus4 = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(PlayerAction.Action.NONE, utgPlus4.action)

        engine.nextPlayerMove(utgPlus4.uuid, PlayerAction(PlayerAction.Action.CALL))

        // UTG+5 goes all in (not a full raise)
        val utgPlus5 = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(PlayerAction.Action.NONE, utgPlus5.action)

        utgPlus5.chips = engine.gameData.targetBet + 11
        val utgPlus5action = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = utgPlus5.chips
        )
        engine.nextPlayerMove(utgPlus5.uuid, utgPlus5action)
        Assert.assertEquals(true, utgPlus5.isAllIn)
        Assert.assertEquals(false, utgPlus5.pendingAction)
        Assert.assertEquals(false, utgPlus5.canRaise)
        Assert.assertEquals(PlayerAction.Action.RAISE, utgPlus5.action)

        Assert.assertEquals(6, engine.gameData.activePlayers.size)

        // UTG+5's, UTG+3's and UTG+2's raises together make up for a full raise difference
        // from UTG+1's current bet, so UTG+1 can raise again
        Assert.assertEquals(true, utgPlus1.pendingAction)
        Assert.assertEquals(true, utgPlus1.canRaise)

        // however, there is not a full raise difference between UTG+5's raise
        // and UTG+4's call so UTG+4 can't raise
        Assert.assertEquals(true, utgPlus4.pendingAction)
        Assert.assertEquals(false, utgPlus4.canRaise)

        (engine.gameData.activePlayers - utgPlus5 - utgPlus4).forEach {
            Assert.assertEquals(true, it.pendingAction)
            Assert.assertEquals(true, it.canRaise)
        }

        Assert.assertEquals(utgPlus5action.numericValue, engine.gameData.targetBet)

        // UTG+6 goes all in (not a full raise)
        val utgPlus6 = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(PlayerAction.Action.NONE, utgPlus6.action)

        utgPlus6.chips = engine.gameData.targetBet + 19
        val utgPlus6action = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = utgPlus6.chips
        )
        engine.nextPlayerMove(utgPlus6.uuid, utgPlus6action)
        Assert.assertEquals(true, utgPlus6.isAllIn)
        Assert.assertEquals(false, utgPlus6.pendingAction)
        Assert.assertEquals(false, utgPlus6.canRaise)
        Assert.assertEquals(PlayerAction.Action.RAISE, utgPlus6.action)

        Assert.assertEquals(5, engine.gameData.activePlayers.size)

        // UTG+6's and UTG+5's raises together make up for a full raise difference
        // from UTG+4's current bet, so UTG+4 can raise again
        Assert.assertEquals(true, utgPlus4.pendingAction)
        Assert.assertEquals(true, utgPlus4.canRaise)

        (engine.gameData.activePlayers - utgPlus6 - utgPlus4).forEach {
            Assert.assertEquals(true, it.pendingAction)
            Assert.assertEquals(true, it.canRaise)
        }

        Assert.assertEquals(utgPlus6action.numericValue, engine.gameData.targetBet)

        // several players call
        engine.nextPlayerMove(engine.gameData.currentPlayerOnMove.uuid, PlayerAction(PlayerAction.Action.CALL))
        Assert.assertEquals(4, engine.gameData.activePlayers.filter { it.pendingAction }.size)

        engine.nextPlayerMove(engine.gameData.currentPlayerOnMove.uuid, PlayerAction(PlayerAction.Action.CALL))
        Assert.assertEquals(3, engine.gameData.activePlayers.filter { it.pendingAction }.size)

        engine.nextPlayerMove(engine.gameData.currentPlayerOnMove.uuid, PlayerAction(PlayerAction.Action.CALL))
        Assert.assertEquals(2, engine.gameData.activePlayers.filter { it.pendingAction }.size)

        engine.nextPlayerMove(engine.gameData.currentPlayerOnMove.uuid, PlayerAction(PlayerAction.Action.CALL))
        Assert.assertEquals(1, engine.gameData.activePlayers.filter { it.pendingAction }.size)

        // last player makes a full raise
        val nextPlayer = engine.gameData.currentPlayerOnMove
        val nextPlayerAction = PlayerAction(
            action = PlayerAction.Action.RAISE,
            numericValue = nextPlayer.currentBet + engine.gameData.targetBet + startingBigBlind
        )
        engine.nextPlayerMove(nextPlayer.uuid, nextPlayerAction)

        // all other players have to play again
        Assert.assertEquals(4, engine.gameData.activePlayers.filter { it.pendingAction }.size)
    }

    @Test
    fun lastPlayerHasDecisionWhenEveryoneElseIsAllInTest() {
        val engine = initEngineAndStart(nrOfPlayers = 3)

        val button = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(true, button.isDealer)
        Assert.assertEquals(true, button.isOnMove)

        var action = PlayerAction(action = PlayerAction.Action.RAISE, numericValue = 2400)
        engine.nextPlayerMove(button.uuid, action)

        Assert.assertEquals(false, button.isOnMove)

        val smallBlind = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(true, smallBlind.isOnMove)

        action = PlayerAction(action = PlayerAction.Action.RAISE, numericValue = 2490)
        engine.nextPlayerMove(smallBlind.uuid, action)

        Assert.assertEquals(false, button.isOnMove)

        val bigBlind = engine.gameData.currentPlayerOnMove
        Assert.assertEquals(true, bigBlind.isOnMove)

        engine.nextPlayerMove(bigBlind.uuid, PlayerAction(PlayerAction.Action.CALL))

        Assert.assertEquals(true, button.isDealer)
        Assert.assertEquals(true, button.isOnMove)
    }

}
