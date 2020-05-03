package io.pokr.game.model

import java.beans.*

data class GameConfig @ConstructorProperties("startingChips", "startingBlinds", "blindIncreaseTime", "playerMoveTime", "rebuyTime", "maxRebuys") constructor(
    val startingChips: Int,
    val startingBlinds: Int,
    val blindIncreaseTime: Int,
    val playerMoveTime: Int,
    val rebuyTime: Int,
    val maxRebuys: Int
) {
    val isValid
        get() = startingChips > 100 && startingChips < 1_000_000 &&
                startingBlinds > 10 && startingBlinds < 1_000_000 &&
                blindIncreaseTime > 1 * 60 && blindIncreaseTime < Integer.MAX_VALUE &&
                playerMoveTime > 5 && playerMoveTime < 60 * 60 &&
                rebuyTime >= 0 && rebuyTime < Integer.MAX_VALUE &&
                maxRebuys >=0 && maxRebuys < Integer.MAX_VALUE

}