package io.pokr.game.model

class GameData constructor(
    val uuid: String,
) {

    enum class RoundState {
        ACTIVE,
        FINISHED,
    }

    enum class State {
        CREATED,
        ACTIVE,
        PAUSED,
        FINISHED,
    }

    var gameState: State = State.CREATED
    var allPlayers = mutableListOf<Player>()
    var targetBet = 0
    var previousStreetTargetBet = 0
    var minRaiseTo = 0
    var smallBlind = 0
    var bigBlind = 0
    var nextBlinds = 0L

    var round = 0
    var gameStart: Long = 0
    var roundState = RoundState.ACTIVE

    var pauseStart = 0L
    var totalPauseTime = 0L

    val isLateRegistrationEnabled
        get() = System.currentTimeMillis() - gameStart - totalPauseTime < config.rebuyTime * 1000

    val gameTime
        get() = System.currentTimeMillis() - gameStart - totalPauseTime

    lateinit var config: GameConfig
    lateinit var cardStack: CardStack
    lateinit var tableCards: CardList

    var bestCards: CardList? = null

    // players active in the current game
    val players
        get() = allPlayers.filter { !it.isFinished && !it.isKicked }

    // players active in the current round (players that can still play in the street)
    val activePlayers
        get() = players.filter { it.action != PlayerAction.Action.FOLD && !it.isAllIn }

    val currentDealer
        get() = allPlayers.first { it.isDealer }

    val nextDealer
        get() = (players + players).run { get(indexOf(currentDealer) + 1) }

    val currentPlayerOnMove
        get() = allPlayers.first { it.isOnMove }

    val nextPlayerOnMove
        get() = nextActivePlayerFrom(currentPlayerOnMove)

    fun nextActivePlayerFrom(player: Player) =
        (players + players).run {
            for (i in indexOf(player) + 1 until size) {
                if (get(i) in activePlayers) {
                    return@run get(i)
                }
            }
            return@run get(0)
        }

    fun pause(pause: Boolean) {
        if (pause) {
            gameState = State.PAUSED
            pauseStart = System.currentTimeMillis()
        } else {
            gameState = State.ACTIVE
            val pauseTime = System.currentTimeMillis() - pauseStart
            totalPauseTime += pauseTime
            nextBlinds += pauseTime
        }
    }
}