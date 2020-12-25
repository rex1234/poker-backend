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
        ABANDONED,
    }

    var gameState: State = State.CREATED

    /**
     * All game players. Do not add players directly, use `addPlayer` to maintain their order.
     */
    var allPlayers = mutableListOf<Player>()

    var targetBet = 0
    var lastFullRaiseDiff = 0
    var previousStreetTargetBet = 0
    var minRaiseTo = 0

    var smallBlind = 0
    var bigBlind = 0
    var nextSmallBlind = 0
    var nextBlindsChangeAt = 0L

    var round = 0
    var gameStart = 0L
    var roundState = RoundState.ACTIVE

    var pauseStart: Long? = null
    var totalPauseTime = 0L

    val gameTime
        get() = if(gameStart == 0L) 0 else System.currentTimeMillis() - gameStart - totalPauseTime

    val isLateRegistrationPossible
        get() = gameTime < config.rebuyTime * 1000

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

    /**
     * The player who is currently small blind. It is possible for that to be no one.
     */
    val currentSmallBlindPlayer
        get() = allPlayers.firstOrNull { it.isSmallBlind }

    val currentBigBlindPlayer
        get() = allPlayers.first { it.isBigBlind }

    val currentPlayerOnMove
        get() = allPlayers.first { it.isOnMove }

    val nextPlayerOnMove
        get() = nextActivePlayerFrom(currentPlayerOnMove)

    /**
     * Adds a player and sorts them by index.
     */
    fun addPlayer(player: Player) {
        allPlayers.add(player.apply {
            // assign random table index for a player
            index = ((1..9).toList() - allPlayers.map { it.index }).shuffled().first()
        })
        allPlayers.sortBy { it.index }
    }

    /**
     * Finds the closest active player that is next (clockwise) to the given player.
     * @param includeRebuyingPlayers If present, the function will consider players who are about to rebuy
     * as active.
     */
    fun nextActivePlayerFrom(player: Player, includeRebuyingPlayers: Boolean = false) =
        (allPlayers + allPlayers).run {
            for (i in indexOf(player) + 1 until size) {
                if (get(i) in activePlayers || (includeRebuyingPlayers && get(i).isRebuyNextRound)) {
                    return@run get(i)
                }
            }
            return@run get(0)
        }

    /**
     * Finds the closest active player that is previous (counter-clockwise) to the given player.
     */
    fun previousActivePlayerFrom(player: Player) =
        (allPlayers + allPlayers).reversed().run {
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
            val pauseTime = System.currentTimeMillis() - (pauseStart ?: 0L)
            totalPauseTime += pauseTime
            nextBlindsChangeAt += pauseTime

            pauseStart = null
        }
    }
}