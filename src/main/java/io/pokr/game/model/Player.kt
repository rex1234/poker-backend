package io.pokr.game.model

/**
 * Class containing player data (uuid, name) and his state in the game (cards, chips, etc.)
 */
class Player(
    val uuid: String
) {
    var name: String = "Player " + System.currentTimeMillis()
    var connected = true
    var finished = false
    var isOnMove = false
    var cards = CardList()
    var currentBet = 0
    var chips = 0

    var showCards = false
}