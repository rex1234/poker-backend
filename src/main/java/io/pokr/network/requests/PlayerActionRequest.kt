package io.pokr.network.requests

import java.beans.*

class PlayerActionRequest @ConstructorProperties("action", "textValue", "numericValue") constructor(
    val action: String,
    val textValue: String?,
    val numericValue: Int?
)