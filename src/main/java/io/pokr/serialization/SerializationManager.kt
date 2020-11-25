package io.pokr.serialization

import com.google.gson.*
import io.pokr.network.*
import org.slf4j.*
import java.io.*

class SerializationManager {

    private val logger = LoggerFactory.getLogger(SerializationManager::class.java)

    private val stateFile = "state/pokrio.json"

    fun storeState(gamePool: GamePool) {
        val serializedState = Gson().toJson(GamePoolState(
            gamePool.gameSessions.values.filter { it.gameEngine.gameRestorePoint != null }.map {
                GamePoolState.GameSessionState(
                    uuid = it.uuid,
                    gameRestorePoint = it.gameEngine.gameRestorePoint!!,
                    playerSessions = it.playerSessions,
                    created = it.created
                )
            }
        ))

        File(stateFile).also {
            File(it.parent).mkdirs()
            it.writeText(serializedState)
        }

        logger.info("Game state serialized into file $stateFile")
        logger.debug(serializedState)
    }

    fun restoreState(gamePool: GamePool) {
        val stateFile = File(stateFile)

        if (stateFile.exists()) {
            val serializedState = stateFile.readText()
            try {
                val deserializedState = Gson().fromJson(serializedState, GamePoolState::class.java)

                gamePool.createGameSessionWithGameData(deserializedState)

                logger.info("Game state restored from file $stateFile")
            } catch (e: JsonSyntaxException) {
                logger.error("Invalid state file ($stateFile) contents", e)
            }
        } else {
            logger.info("No state file found. Restoring no games.")
        }
    }

}