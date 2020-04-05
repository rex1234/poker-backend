package io.pokr.game.model

import io.pokr.network.model.PlayerAction

class Game constructor(
    val uuid: String
) {

    enum class RoundState {
        ACTIVE,
        FINISHED
    }

    enum class State {
        CREATED,
        ACTIVE,
        FINISHED
    }

    var gameState: State = State.CREATED
    var players = mutableListOf<Player>()
    var targetBet = 0
    var smallBlind = 20
    var bigBlind = 40
    var nextBlinds = 0L

    var round = 0
    var gameStart: Long = 0
    var roundState = RoundState.ACTIVE

    var lateRegistrationEnabled = true

    lateinit var config: GameConfig
    lateinit var cardStack: CardStack
    lateinit var tableCards: CardList

    val activePlayers
        get() = players.filter { !it.isFinished && it.action != PlayerAction.Action.FOLD && it.chips > 0 }

    val currentDealer
        get() = players.first { it.isDealer }

    val nextDealer
        get() = (activePlayers + activePlayers).run { get(indexOf(currentDealer) + 1) }

    val currentPlayerOnMove
        get() = players.first { it.isOnMove }

    val nextPlayerOnMove
        get() = (activePlayers + activePlayers).run { get(indexOf(currentPlayerOnMove) + 1) }
}