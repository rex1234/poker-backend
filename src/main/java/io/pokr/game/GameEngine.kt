package io.pokr.game

import io.pokr.game.model.CardStack
import io.pokr.game.model.Game
import io.pokr.game.model.Player

class GameEngine {

    val players = mutableListOf<Player>()
    val game = Game()

    fun init() {
        game.cardStack = CardStack.create()
        game.players = mutableListOf()
    }

    fun addPlayer(player: Player) {
        game.players.add(player)
    }

    fun nextAction() {
        if(game.round == 1) {
            players.forEach {
                it.cards = it.cards.with(game.cardStack.takeCards(2))
            }
            game.midCards = game.midCards.with(game.cardStack.takeCards(3))
        }
    }

}