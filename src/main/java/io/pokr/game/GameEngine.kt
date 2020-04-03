package io.pokr.game

import io.pokr.game.model.CardStack
import io.pokr.game.model.Game
import io.pokr.game.model.GameConfig
import io.pokr.game.model.Player

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

    fun init() {
        game.players = mutableListOf()
    }

    fun addPlayer(playerUUID: String) {
        game.players.add(Player(playerUUID))

        gameStateUpdated(game)
    }

    fun startGame() {
        game.gameState = Game.State.ACTIVE

        game.players.forEach {
            it.cards = it.cards.with(game.cardStack.drawCards(2))
        }
        game.midCards = game.midCards.with(game.cardStack.drawCards(3))

        gameStateUpdated(game)
    }

    fun nextAction() {
        if(game.round == 1) {

        }

        gameStateUpdated(game)
    }

    fun evaluateRound() {
        val ranks = handComparator.evalPlayers(game.players, game.midCards)
        val betsSum = game.players.sumBy { it.currentBet }

        ranks[0].player.chips = 0
        game.players.forEach { it.currentBet = 0 }

        gameStateUpdated(game)
    }

    fun changePlayerName(uuid: String, name: String) =
        applyOnPlayer(uuid) {
            it.name = name
        }

    fun applyOnPlayer(uuid: String, action: (Player) -> Unit) {
        game.players.firstOrNull { it.uuid == uuid }?.let {
            action(it)
        } ?: throw IllegalArgumentException("Invalid player UUID")
    }

}