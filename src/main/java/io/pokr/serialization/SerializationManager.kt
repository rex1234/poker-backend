package io.pokr.serialization

import com.google.gson.*
import com.google.gson.stream.*
import io.pokr.game.model.*
import io.pokr.network.*
import org.slf4j.*
import java.io.*
import java.lang.Exception

class SerializationManager {

    private val logger = LoggerFactory.getLogger(SerializationManager::class.java)

    private val stateFile = "state/pokrio.json"

    private val gson: Gson

    init {
        gson = GsonBuilder()
            .registerTypeAdapter(CardList::class.java, object: TypeAdapter<CardList>() {
                override fun write(json: JsonWriter, value: CardList) {
                    json.value(value.toString())
                }

                override fun read(json: JsonReader) =
                    CardList.parse(json.nextString())
            }).create()
    }

    fun storeState(gamePool: GamePool) {
        val serializedState = gson.toJson(GamePoolState(
            gamePool.gameSessions.values.filter { it.gameEngine.gameRestorePoint != null }.map {
                GamePoolState.GameSessionState(
                    uuid = it.uuid,
                    gameRestorePoint = it.gameEngine.gameRestorePoint!!,
                    playerSessions = it.playerSessions.map { it.copy(sessionId = "") },
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
                val deserializedState = gson.fromJson(serializedState, GamePoolState::class.java)

                // try whether valid GameData can be constructed (NPE is thrown if something is missing)
                deserializedState.gameSessions.forEach {
                    it.gameRestorePoint.toGameData()
                }

                gamePool.createGameSessionWithGameData(deserializedState)

                logger.info("Game state restored from file $stateFile")
            } catch (e: Exception) {
                logger.error("Invalid state file ($stateFile) contents", e)
            }
        } else {
            logger.info("No state file found. Restoring no games.")
        }
    }

}