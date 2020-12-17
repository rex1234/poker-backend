package io.pokr.game.model

import kotlin.math.*

import io.pokr.game.tools.*

/**
 * Class containing player data (uuid, name) and his state in the game (cards, chips, etc.)
 */
data class Player(
    val uuid: String,
    var index: Int = 0, // player's position on the table
    var name: String = "Nameless",
    var isAdmin: Boolean = false,
    var isConnected: Boolean = true,
    var isFinished: Boolean = false, // whether the player already lost
    var isKicked: Boolean = false, // like isFinished but permanent (cannot rebuy)
    var isDealer: Boolean = false,
    var isOnMove: Boolean = false,
    var moveStart: Long = 0L, // time when the player's action began
    var action: PlayerAction.Action = PlayerAction.Action.NONE,
    var pendingAction: Boolean = false,
    var cards: CardList = CardList(),
    var hand: HandComparator.Hand? = null,
    var currentBet: Int = 0,
    var canRaise: Boolean = true,
    var chips: Int = 0,
    var finalRank: Int = 0, // final game rank

    var isRebuyNextRound: Boolean = false,
    var didRebuyThisRound: Boolean = false,
    var rebuyCount: Int = 0,

    var connectedToRound: Int = 0, // to which round the player has connected

    var isLeaveNextRound: Boolean = false,

    // only for FE
    var bestCards: CardList? = null,
    var showCards: Boolean = false,
    var lastWin: Int = 0,
    var isWinner: Boolean = false,

    // helper vars
    var chipsAtStartOfTheRound: Int = 0
) {

    fun startMove() {
        moveStart = System.currentTimeMillis()
        isOnMove = true
    }

    fun postBlind(amount: Int) {
        currentBet = min(chips, amount)
        if (isAllIn) {
            pendingAction = false
        }
    }

    val isAllIn
        get() = chips == currentBet

    override fun equals(other: Any?): Boolean {
        return other is Player && uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}