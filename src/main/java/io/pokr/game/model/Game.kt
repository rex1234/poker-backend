package io.pokr.game.model

class Game {

    lateinit var cardStack: CardStack
    lateinit var players: MutableList<Player>
    lateinit var midCards: CardList

    var round = 1

    enum class State {

    }

}