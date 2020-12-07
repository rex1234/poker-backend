package io.pokr.game.model

import java.beans.*

data class GameConfig @ConstructorProperties(
    "startingChips",
    "startingBlinds",
    "blindIncreaseTime",
    "playerMoveTime",
    "rebuyTime",
    "maxRebuys"
) constructor(
    val startingChips: Int,
    val startingBlinds: Int,
    val blindIncreaseTime: Int,
    val playerMoveTime: Int,
    val rebuyTime: Int,
    val maxRebuys: Int,
)