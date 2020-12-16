package io.pokr.database

import org.jetbrains.exposed.sql.*

object Games : Table() {
    val id = integer("id").autoIncrement()
    val uuid = varchar("uuid", 8)
    val state = varchar("state", 255)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Players : Table() {
    val id = integer("id").autoIncrement()
    val gameId = integer("game_id") references Games.id
    val name = varchar("name", 255)
    val rank = integer("rank")

    override val primaryKey = PrimaryKey(id)
}