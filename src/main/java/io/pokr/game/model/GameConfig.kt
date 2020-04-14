package io.pokr.game.model

import java.beans.*

class GameConfig @ConstructorProperties("startingChips", "startingBlinds", "blindIncreaseTime", "playerMoveTime", "rebuyTime") constructor(
    val startingChips: Int,
    val startingBlinds: Int,
    val blindIncreaseTime: Int,
    val playerMoveTime: Int,
    val rebuyTime: Int
)