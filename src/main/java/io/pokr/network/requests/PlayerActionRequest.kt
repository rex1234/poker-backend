package io.pokr.network.requests

import io.pokr.network.model.PlayerAction
import java.beans.ConstructorProperties

class PlayerActionRequest @ConstructorProperties("action", "textValue", "numericValue") constructor(
    val action: String,
    val textValue: String?,
    val numericValue: Int?
)