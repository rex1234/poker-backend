package io.pokr.game.model

data class PlayerAction(
    val action: Action,
    val numericValue: Int? = null,
    val textValue: String? = null,
) {

    enum class Action(
        val key: String,
    ) {

        // game actions
        NONE("none"),
        CALL("call"),
        CHECK("check"),
        FOLD("fold"),
        RAISE("raise"),

        // player actions
        CHANGE_NAME("changeName"),
        LEAVE("leave"),
        REBUY("rebuy"),
        SHOW_CARDS("showCards"),

        // admin actions
        KICK("kickPlayer"),
        PAUSE("pause"),
        START_GAME("startGame"),
    }
}