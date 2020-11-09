package io.pokr.chat

import io.pokr.config.*
import java.io.*

object ChatEngine {

    val validReactions by lazy {
        File(PokrioConfig.webDir + "/img/reacts/").list().map {
            it.replace(".svg", "")
        }
    }

    fun isValidReaction(reaction: String) =
        reaction in validReactions
}