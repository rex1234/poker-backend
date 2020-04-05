package io.pokr.game

import io.pokr.game.model.*
import io.pokr.network.exceptions.GameException
import io.pokr.network.model.PlayerAction
import java.util.*

class HoldemTournamentGameEngine(
    gameUuid: String
) {
    private val handComparator = HandComparator()

    val game = Game(gameUuid)

    val gameTimer = GameTimer {
        gameTick()
    }

    fun initGame(config: GameConfig) {
        game.config = config
    }

    fun addPlayer(playerUUID: String) {
        if(game.players.size == 9) {
            throw GameException(10, "Game is already full")
        }

        if(!game.lateRegistrationEnabled) {
            throw GameException(11, "Late registration is not possible")
        }

        game.players.add(Player(playerUUID).apply {
            index = ((1..9).toList() - game.players.map { it.index }).shuffled().first()
        })
    }

    fun startGame() {
        if(game.gameState != Game.State.CREATED) {
            throw GameException(17, "Game already started")
        }

        if(game.players.size == 1) {
            throw GameException(12, "Cannot start a game with only 1 player")
        }

        game.gameState = Game.State.ACTIVE
        game.gameStart = System.currentTimeMillis()
        game.nextBlinds = game.gameStart + game.config.blindIncreaseTime * 1000

        game.players.sortBy { it.index }
        game.players.shuffled().first().isDealer = true

        game.players.forEach {
            it.chips = game.config.startingChips
        }

        startNewRound()

        gameTimer.start()
    }

    private fun startNewRound() {
        // init the round, set target bet to big blind, set players' blinds and draw cards
        game.round++
        game.cardStack = CardStack.create()
        game.targetBet = game.bigBlind
        game.roundState = Game.RoundState.ACTIVE

        game.players.forEach {
            it.action = PlayerAction.Action.NONE
            it.showCards = false
            it.isOnMove = false
            it.hand = null
            it.cards = game.cardStack.drawCards(2)
        }

        (game.activePlayers + game.activePlayers + game.activePlayers).apply {
            get(indexOf(game.currentDealer) + 1).apply {
                currentBet = kotlin.math.min(chips, game.smallBlind)
            }

            get(indexOf(game.currentDealer) + 2).apply {
                currentBet = kotlin.math.min(chips, game.bigBlind)
            }

            get(indexOf(game.currentDealer) + 3).apply {
                isOnMove = true
                moveStart = System.currentTimeMillis()
            }
        }

        game.tableCards = CardList()
    }

    fun nextPlayerMove(playerUuid: String, playerAction: PlayerAction) {
        val player = game.currentPlayerOnMove

        if(player.uuid != playerUuid) {
            throw GameException(16, "Player is not on move")
        }

        val passedMove = when(playerAction.action) {
            PlayerAction.Action.CALL -> {
                // call to the target bet or go all in
                player.currentBet = kotlin.math.min(game.targetBet, player.chips)
                true
            }

            // we can check only if we match the target bet
            PlayerAction.Action.CHECK ->
                player.currentBet == game.targetBet

            // raise and reset other players' actions
            PlayerAction.Action.RAISE -> {
                val raiseAmount = playerAction.numericValue!!
                if (raiseAmount > game.bigBlind) {// TODO: implement raise rules
                    if (player.chips - player.currentBet > raiseAmount) {
                        player.currentBet += raiseAmount
                        game.targetBet += raiseAmount

                        (game.players - player).forEach {
                            it.action = PlayerAction.Action.NONE
                        }

                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            // just fold
            PlayerAction.Action.FOLD -> true

            else -> false
        }

        if(passedMove) {
            player.action = playerAction.action

            // all but one folded
            if(game.players.count { it.action != PlayerAction.Action.FOLD } == 1) {
                finishRound()
                return
            }

            // everyone passed an action
            if(game.players.none { it.action == PlayerAction.Action.NONE }) {

                // 5 cards already, we will finish the round
                if(game.tableCards.cards.size == 5) {
                    finishRound()
                } else { // or we will draw a card and reset actions
                    drawCards()

                    game.players.forEach {
                        it.action = PlayerAction.Action.NONE
                        it.hand = handComparator.findHighestHand(it.cards, game.tableCards)
                    }

                    game.currentPlayerOnMove.isOnMove = false
                    (game.activePlayers + game.activePlayers).apply {
                        get(indexOf(game.currentDealer) + 1).isOnMove = true
                        get(indexOf(game.currentDealer) + 1).moveStart = System.currentTimeMillis()
                    }
                }
            } else {
                nextPlayer()
            }
        }
    }

    private fun drawCards() {
        if(game.tableCards.cards.size == 0) {
            game.tableCards = game.tableCards.with(game.cardStack.drawCards(3))
        } else if(game.tableCards.cards.size < 5) {
            game.tableCards = game.tableCards.with(game.cardStack.drawCards(1))
        }
    }

    private fun nextPlayer() {
        val playerOnMove = game.currentPlayerOnMove
        val nextPlayerOnMove = game.nextPlayerOnMove

        playerOnMove.isOnMove = false
        nextPlayerOnMove.isOnMove = true
        nextPlayerOnMove.moveStart = System.currentTimeMillis()
    }

    private fun finishRound() {
        val winners = if(game.players.count { it.action != PlayerAction.Action.FOLD } == 1) {
            listOf(game.players.first { it.action != PlayerAction.Action.FOLD })
        } else {
            val ranks = handComparator.evalPlayers(game.players, game.tableCards)
            ranks.filter { it.rank == ranks[0].rank }.map { it.player }
        }

        val betsSum = game.players.sumBy { it.currentBet }

        // TODO sidepot
        winners.forEach {
            it.chips += betsSum / winners.size
        }

        game.players.forEach {
            it.chips -= it.currentBet
            it.currentBet = 0
        }

        game.players.filter { it.chips == 0 }.forEach { it.isFinished = true }

        val currentDealer = game.currentDealer
        val nextPlayer = game.nextDealer

        currentDealer.isDealer = false
        nextPlayer.isDealer = true

        game.roundState == Game.RoundState.FINISHED

        startNewRound()
    }

    private fun gameTick() {
        return //disabled for now

        // player ran out of time limit
        val currentPlayerOnMove = game.currentPlayerOnMove
        if(System.currentTimeMillis() - currentPlayerOnMove.moveStart > game.config.playerMoveTime * 1000) {
            nextPlayerMove(
                currentPlayerOnMove.uuid,
                if(currentPlayerOnMove.currentBet == game.targetBet)
                    PlayerAction(PlayerAction.Action.CHECK, null, null)
                else {
                    PlayerAction(PlayerAction.Action.FOLD, null, null)
                }
            )
        }

        if(System.currentTimeMillis() > game.nextBlinds) {
            increaseBlinds()
            game.nextBlinds = System.currentTimeMillis() + game.config.blindIncreaseTime * 1000
        }
    }

    // TODO: Implement blind calculation
    private fun increaseBlinds() {
        val smallBlinds = listOf(10, 20, 30, 50, 75, 100, 200, 400, 600, 800, 1000)

        game.smallBlind = smallBlinds.firstOrNull { it > game.smallBlind } ?: game.smallBlind * 2
        game.bigBlind = game.smallBlind * 2
    }

    fun showCards(playerUuid: String) =
        applyOnPlayer(playerUuid) {
            it.showCards = true
        }

    fun changeName(playerUuid: String, name: String) =
        applyOnPlayer(playerUuid) {
            it.name = name
        }

    private fun applyOnPlayer(uuid: String, action: (Player) -> Unit) {
        game.players.firstOrNull { it.uuid == uuid }?.let {
            action(it)
        } ?: throw GameException(13, "Invalid player UUID")
    }

}