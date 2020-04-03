package io.pokr.network.model

data class PlayerAction(
    val action: Action,
    val numericValue: Int? = null,
    val textValue: String? = null
) {
    enum class Action(
        val key: String
    ) {

        // game actions
        CALL("call"),
        CHECK("check"),
        FOLD("fold"),
        RAISE("rais"),

        // player actions
        CHANGE_NAME("changeName"),
        SHOW_CARDS("showCards"),

        // admin actions
        START_GAME("startGame"),
        KICK("kickPlayer"),
        DISCARD_GAME("discardGame")
    }
}