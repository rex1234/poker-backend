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
    val rounds = integer("rounds").default(0)

    val startingChips = integer("starting_chips").default(0)
    val startingBlinds = integer("starting_blinds").default(0)
    val blindIncreaseTime = integer("blind_inc_time").default(0)
    val playerMoveTime = integer("player_move_time").default(0)
    val rebuyTime = integer("rebuy_time").default(0)
    val maxRebuys = integer("max_rebuys").default(0)

    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Players : Table() {
    val id = integer("id").autoIncrement()
    val gameId = integer("game_id") references Games.id
    val name = varchar("name", 255)

    val rebuyCount = integer("rebuy_count")
    val hasLeft = integer("has_left")
    val isAdmin = integer("is_admin")
    val roundConnected = integer("round_connected")

    val rank = integer("rank")

    override val primaryKey = PrimaryKey(id)
}