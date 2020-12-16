package io.pokr.database

import org.jetbrains.exposed.sql.*

object Games : Table() {
    val id = integer("id").autoIncrement()
    val uuid = varchar("uuid", 8)
    val state = varchar("state", 255).default("created")

    val playerCount = integer("player_count").default(1)
    val gameLength = long("game_length").default(0L)
    val pauseLength = long("pause_length").default(0L)
    val totalRebuys = integer("rebuys_count").default(0)

    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Players : Table() {
    val id = integer("id").autoIncrement()
    val gameId = integer("game_id") references Games.id
    val name = varchar("name", 255)

    val rebuyCount = integer("rebuy_count")
    val left = integer("left")

    val rank = integer("rank")

    override val primaryKey = PrimaryKey(id)
}