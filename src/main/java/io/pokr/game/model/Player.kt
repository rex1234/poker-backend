package io.pokr.game.model

import io.pokr.network.model.PlayerAction

/**
 * Class containing player data (uuid, name) and his state in the game (cards, chips, etc.)
 */
class Player(
    val uuid: String
) {
    var name: String = "Player " + System.currentTimeMillis()
    var isConnected = true
    var isFinished = false
    var isDealer = false
    var isOnMove = false
    var action = PlayerAction.Action.NONE
    var cards = CardList()
    var currentBet = 0
    var chips = 0

    var showCards = false

    override fun equals(other: Any?): Boolean {
        return other is Player && uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}