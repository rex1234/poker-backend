package io.pokr.database

import io.pokr.game.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*

class DatabaseManager {

    fun init() {
        Class.forName("org.sqlite.JDBC")
        File("db").mkdir()
        Database.connect("jdbc:sqlite:db/pokrio.db")

        transaction {
            SchemaUtils.create(Games, Players)
        }
    }

    fun insertGame() {
        transaction {
            Games.insert {
                it[uuid] = "ABCD"
                it[state] = "discarded"
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }

    fun insertGame(gameData: GameData) {
        Games.insert {
            it[uuid] = gameData.uuid
            it[state] = "discarded"
            it[createdAt] = System.currentTimeMillis()
        }
    }

}