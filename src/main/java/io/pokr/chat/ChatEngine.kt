package io.pokr.chat

import io.pokr.chat.model.*
import java.io.*

object ChatEngine {

    val validReactions by lazy {
        File("web/img/reacts/").list().map {
            it.replace(".svg", "")
        }
    }

    fun isValidReaction(reaction: String) =
        reaction in validReactions
}