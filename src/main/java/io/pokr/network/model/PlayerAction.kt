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
        NONE("none"),
        CALL("call"),
        CHECK("check"),
        FOLD("fold"),
        RAISE("raise"),

        // player actions
        CHANGE_NAME("changeName"),
        SHOW_CARDS("showCards"),

        // admin actions
        START_GAME("startGame"),
        KICK("kickPlayer"),
        DISCARD_GAME("discardGame")
    }
}