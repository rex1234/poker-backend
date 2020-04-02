package io.pokr.network.model

class PlayerAction(
    val action: Action,
    val numericValue: Int?,
    val textValue: String?
) {
    enum class Action {

        // game actions
        CALL,
        CHECK,
        FOLD,
        RAISE,

        // player actions
        CHANGE_NAME,

        // admin actions
        KICK,
        DISCARD_GAME
    }
}