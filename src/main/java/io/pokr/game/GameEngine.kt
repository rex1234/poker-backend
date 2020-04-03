package io.pokr.game

import io.pokr.game.model.*
import io.pokr.network.model.PlayerAction

class GameEngine(
    gameUuid: String
) {

    val handComparator = HandComparator()
    var gameStateUpdated: (Game) -> Unit = {}

    val game = Game.withConfig(
        gameUuid,
        GameConfig(
            10_000,
            40,
            300
        ))

    fun addPlayer(playerUUID: String) {
        game.players.add(Player(playerUUID))

        gameStateUpdated(game)
    }

    fun startGame() {
        game.gameState = Game.State.ACTIVE
        game.gameStart = System.currentTimeMillis()

        game.players.shuffle()
        game.players[0].isDealer = true

        startNewRound()

        gameStateUpdated(game)
    }

    fun startNewRound() {
        game.round++
        game.cardStack = CardStack.create()
        game.targetBet = game.bigBlind

        game.players.forEach {
            it.action = PlayerAction.Action.NONE
            it.showCards = false
            it.isOnMove = false
            it.cards = it.cards.with(game.cardStack.drawCards(2))
        }

        (game.activePlayers + game.activePlayers + game.activePlayers).apply {
            get(indexOf(game.currentDealer) + 1).apply {
                currentBet = kotlin.math.min(chips, game.bigBlind)
            }

            get(indexOf(game.currentDealer) + 2).apply {
                currentBet = kotlin.math.min(chips, game.smallBlind)
                isOnMove = true
            }
        }

        game.tableCards = CardList()
        game.tableCards = game.tableCards.with(game.cardStack.drawCards(3))
    }

    fun nextPlayerMove(playerAction: PlayerAction) {
        val player = game.currentPlayerOnMove

        val passedMove = when(playerAction.action) {
            PlayerAction.Action.CALL ->
                if(player.chips - player.currentBet > game.targetBet) {
                    player.currentBet = game.targetBet
                    true
                } else {
                    false
                }

            PlayerAction.Action.CHECK ->
                player.currentBet == game.targetBet

            PlayerAction.Action.RAISE -> {
                val raiseAmount = playerAction.numericValue!!
                if (raiseAmount > game.bigBlind) {// TODO: implement raise rules
                    if (player.chips - player.currentBet > raiseAmount) {
                        player.currentBet = raiseAmount
                        game.targetBet += raiseAmount

                        game.players.forEach {
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

            PlayerAction.Action.FOLD -> true

            else -> false
        }

        if(passedMove) {
            player.action = playerAction.action

            if(game.players.all { it.action == PlayerAction.Action.NONE }) {
                if(game.tableCards.cards.size == 5) {
                    finishRound()
                } else {
                    drawCards()

                    game.players.forEach {
                        it.action = PlayerAction.Action.NONE
                    }

                    nextPlayer()
                }
            }
        }
    }

    fun drawCards() {
        if(game.tableCards.cards.size == 0) {
            game.tableCards = game.tableCards.with(game.cardStack.drawCards(3))
        } else if(game.tableCards.cards.size < 5) {
            game.tableCards = game.tableCards.with(game.cardStack.drawCards(1))
        }
    }

    fun nextPlayer() {
        val playerOnMove = game.currentPlayerOnMove
        val nextPlayerOnMove = game.nextPlayerOnMove

        playerOnMove.isOnMove = false
        nextPlayerOnMove.isOnMove = true
    }

    fun finishRound() {
        val ranks = handComparator.evalPlayers(game.players, game.tableCards)
        val betsSum = game.players.sumBy { it.currentBet }

        // TODO sidepot
        ranks[0].player.chips += betsSum
        game.players.forEach { it.currentBet = 0 }

        game.players.filter { it.chips == 0 }.forEach { it.isFinished = true }

        val currentDealer = game.currentDealer
        val nextPlayer = game.nextDealer

        currentDealer.isDealer = false
        nextPlayer.isDealer = true

        startNewRound()

        gameStateUpdated(game)
    }

    fun showCards(playerUuid: String) =
        applyOnPlayer(playerUuid) {
            it.showCards = true
        }

    fun changeName(playerUuid: String, name: String) =
        applyOnPlayer(playerUuid) {
            it.name = name
        }

    fun applyOnPlayer(uuid: String, action: (Player) -> Unit) {
        game.players.firstOrNull { it.uuid == uuid }?.let {
            action(it)
        } ?: throw IllegalArgumentException("Invalid player UUID")
    }

}