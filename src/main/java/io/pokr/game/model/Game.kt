package io.pokr.game.model

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
        PAUSED,
        FINISHED
    }

    var gameState: State = State.CREATED
    var players = mutableListOf<Player>()
    var targetBet = 0
    var smallBlind = 0
    var bigBlind = 0
    var nextBlinds = 0L

    var round = 0
    var gameStart: Long = 0
    var roundState = RoundState.ACTIVE

    var pauseStart = 0L
    var totalPauseTime = 0L

    val isLateRegistrationEnabled
        get() = System.currentTimeMillis() - gameStart < config.rebuyTime * 1000

    val gameTime
        get() = System.currentTimeMillis() - gameStart - totalPauseTime

    lateinit var config: GameConfig
    lateinit var cardStack: CardStack
    lateinit var tableCards: CardList

    var winningCards: CardList? = null

    val activePlayers
        get() = players.filter { !it.isFinished && it.action != PlayerAction.Action.FOLD && it.chips - it.currentBet > 0 }

    val currentDealer
        get() = players.first { it.isDealer }

    val nextDealer
        get() = (activePlayers + activePlayers).run { get(indexOf(currentDealer) + 1) }

    val currentPlayerOnMove
        get() = players.first { it.isOnMove }

    val nextPlayerOnMove
        get() = (activePlayers + activePlayers).run { get(indexOf(currentPlayerOnMove) + 1) }

    fun pause(pause: Boolean) {
        if(pause) {
            pauseStart = System.currentTimeMillis()
        } else {
            val pauseTime = System.currentTimeMillis() - pauseStart
            totalPauseTime += pauseTime
            nextBlinds += pauseTime
        }
    }
}