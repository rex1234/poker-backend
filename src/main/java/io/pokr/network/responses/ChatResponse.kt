package io.pokr.network.responses

import io.pokr.chat.model.*
import io.pokr.game.model.*
import org.apache.commons.text.*

class ChatResponse(
    val name: String,
    val index: Int?,
    val time: Long,
    val message: String,
    val flash: Boolean = false,
)

class ChatResponseFactory {

    companion object {

        fun fromChatMessage(player: Player?, chatMessage: ChatMessage) =
            ChatResponse(
                StringEscapeUtils.escapeHtml4(player?.name ?: "System"),
                player?.index ?: -1,
                System.currentTimeMillis(),
                StringEscapeUtils.escapeHtml4(chatMessage.text),
                chatMessage.isFlash
            )
    }
}