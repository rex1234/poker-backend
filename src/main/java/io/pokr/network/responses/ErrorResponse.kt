package io.pokr.network.responses

data class ErrorResponse(
    val code: Int,
    val message: String,
)