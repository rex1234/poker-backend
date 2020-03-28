package io.pokr.game.model

/**
 * Class containing player data (uuid, name) and his state in the game (cards, chips, etc.)
 */
class Player(
    val uuid: String,
    val name: String
) {
    var cards = CardList()
}