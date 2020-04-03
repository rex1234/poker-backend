package io.pokr.game

import io.pokr.game.model.*

class GameEngine(
    val gameUuid: String
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
        game.players[0].isOnMove = true

        startNewRound()

        gameStateUpdated(game)
    }

    fun startNewRound() {
        game.round++
        game.cardStack = CardStack.create()

        game.players.forEach {
            it.showCards = false
            it.cards = it.cards.with(game.cardStack.drawCards(2))
        }

        game.cards = CardList()
        game.cards = game.cards.with(game.cardStack.drawCards(3))
    }

    fun onNextPlayer() {

    }

    fun evaluateRound() {
        val ranks = handComparator.evalPlayers(game.players, game.cards)
        val betsSum = game.players.sumBy { it.currentBet }

        // TODO sidepot
        ranks[0].player.chips += betsSum
        game.players.forEach { it.currentBet = 0 }

        game.players.filter { it.chips == 0 }.forEach { it.finished = true }

        val currentPlayer = game.playerOnMove
        val nextPlayer = game.nextPlayer

        currentPlayer.isOnMove = false
        nextPlayer.isOnMove = true

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