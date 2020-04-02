package io.pokr.game

import io.pokr.game.model.Game
import io.pokr.game.model.GameConfig
import io.pokr.game.model.Player
import java.util.*

class GameEngine {

    val handComparator = HandComparator()
    val players = mutableListOf<Player>()

    var gameStateUpdated: (Game) -> Unit = {}

    val game = Game.withConfig(GameConfig(
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
        nextAction()

        gameStateUpdated(game)
    }

    fun nextAction() {
        if(game.round == 1) {
            players.forEach {
                it.cards = it.cards.with(game.cardStack.takeCards(2))
            }
            game.midCards = game.midCards.with(game.cardStack.takeCards(3))
        }

        gameStateUpdated(game)
    }

    fun evaluateRound() {
        val ranks = handComparator.evalPlayers(game.players, game.midCards)
        val betsSum = players.sumBy { it.currentBet }

        ranks[0].player.chips = 0
        players.forEach { it.currentBet = 0 }

        gameStateUpdated(game)
    }

    fun changePlayerName(uuid: String, name: String) =
        game.players.firstOrNull { it.uuid == uuid }?.let {
            it.name = name
        } ?: throw IllegalArgumentException("Invalid player UUID")


}