package io.pokr.database

import io.pokr.game.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.io.*

object DatabaseManager {

    init {
        Class.forName("org.sqlite.JDBC")
        File("db").mkdir()
        Database.connect("jdbc:sqlite:db/pokrio.db")

        transaction {
            SchemaUtils.create(Games, Players)
        }
    }

    fun updateGame(gameData: GameData) {
        transaction {
            if(Games.select { Games.uuid eq gameData.uuid }.count() == 0L) {
                Games.insert {
                    it[uuid] = gameData.uuid
                    it[state] = gameData.gameState.name
                    it[createdAt] = System.currentTimeMillis()
                }
            } else {
                Games.update({ Games.uuid eq gameData.uuid }) {
                    it[state] = gameData.gameState.name
                    it[playerCount] = gameData.allPlayers.size
                    it[gameLength] = gameData.gameTime / 1000
                    it[pauseLength] = gameData.totalPauseTime / 1000
                    it[totalRebuys] = gameData.allPlayers.sumBy { it.rebuyCount }
                }

                if(gameData.gameState in listOf(GameData.State.FINISHED, GameData.State.DISCARDED)) {
                    val gameId = Games
                        .select { Games.uuid eq gameData.uuid}
                        .last()[Games.id]

                    gameData.allPlayers.forEach {
                        insertPlayer(it, gameId)
                    }
                }

                Unit
            }
        }
    }

    private fun insertPlayer(player: Player, gameId: Int) {
        Players.insert {
            it[name] = player.name
            it[rank] = player.finalRank
            it[rebuyCount] = player.rebuyCount
            it[left] = if(player.isKicked) 1 else 0
            it[Players.gameId] = gameId
        }
    }
}