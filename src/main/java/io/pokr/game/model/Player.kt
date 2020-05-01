package io.pokr.game.model

import io.pokr.game.tools.*

/**
 * Class containing player data (uuid, name) and his state in the game (cards, chips, etc.)
 */
class Player(
    val uuid: String
) {
    var index: Int = 0 // player's position on the table
    var name: String = "Nameless"
    var isAdmin = false
    var isConnected = true
    var isFinished = false // whether the player already lost
    var isKicked = false // like isFinished but permanent (cannot rebuy)
    var isDealer = false
    var isOnMove = false
    var moveStart = 0L // time when the player's action began
    var action = PlayerAction.Action.NONE
    var cards = CardList()
    var hand: HandComparator.Hand? = null
    var currentBet = 0
    var chips = 0
    var rebuyCount = 0
    var finalRank = 0 // final game rank

    var isLeaveNextRound = false
    var isRebuyNextRound = false

    // only for FE
    var bestCards: CardList? = null
    var showCards = false
    var lastWin = 0
    var isWinner = false

    fun startMove() {
        moveStart = System.currentTimeMillis()
        isOnMove = true
    }

    val isAllIn
        get() = chips - currentBet == 0

    override fun equals(other: Any?): Boolean {
        return other is Player && uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}