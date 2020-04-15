package io.pokr.network.model

class GameSession(
    val uuid: String,
    val playerSessions: MutableList<PlayerSession> = mutableListOf(),
    val created: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?) =
        other is GameSession && other.uuid == uuid

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}