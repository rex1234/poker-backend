package io.pokr.network.model

data class PlayerSession(
    var sessionId: String,
    var uuid: String,
    val created: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?) =
        other is PlayerSession && other.uuid == uuid

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}