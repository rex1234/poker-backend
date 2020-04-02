package io.pokr.network.model

data class PlayerAction(
    val action: Action,
    val numericValue: Int? = null,
    val textValue: String? = null
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