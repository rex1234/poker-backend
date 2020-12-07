package io.pokr.game.tools

import io.pokr.game.model.*

object InputValidator {

    const val PLAYER_NAME_PATTERN =
        "[.:%!?@#^$&*(),+|'_=\\-0-9a-zA-Z\\u00C0-\\u024F\\u1E00-\\u1EFF ]{1,10}"

    fun validatePlayerName(str: String) =
        str.isNotBlank() && PLAYER_NAME_PATTERN.toRegex().matches(str)

    // @formatter:off

    fun validateGameConfig(gameConfig: GameConfig) =
        gameConfig.run {
            startingChips     in 100    .. 1_000_000 &&
            startingBlinds    in 1      .. 1_000_000 &&
            blindIncreaseTime in 1 * 60 .. 24 * 60 * 60 &&
            playerMoveTime    in 5      .. 60 * 60 &&
            rebuyTime         in 0      .. 24 * 60 * 60 &&
            maxRebuys         in 0      .. 100
        }

    // @formatter:on
}