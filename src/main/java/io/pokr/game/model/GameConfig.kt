package io.pokr.game.model

import java.beans.ConstructorProperties

class GameConfig @ConstructorProperties("startingChips", "startingBlinds", "blindIncreaseTime", "playerMoveTime") constructor(
    val startingChips: Int,
    val startingBlinds: Int,
    val blindIncreaseTime: Int,
    val playerMoveTime: Int,
    val rebuyTime: Int
)