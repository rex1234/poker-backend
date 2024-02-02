package io.pokr.config

import io.pokr.*
import io.github.cdimascio.dotenv.*
import java.io.*

object PokrioConfig {

    fun exists() =
        File(".env").exists()

    val isDebug
        get() = dotenv()["ENV"] == null || dotenv()["ENV"] == "debug"

    val version
        get() = BuildConfig.LAST_COMMIT

    val webDir
        get() = dotenv()["WEB_DIR"]!!

    val webUrl
        get() = dotenv()["WEB_URL"]!!

    val socketUrl
        get() = dotenv()["SOCKET_URL"] ?: webUrl

    val webPort
        get() = dotenv()["WEB_PORT"]!!.toInt()

    val keyStorePath
        get() = dotenv()["KEYSTORE_PATH"]

    val keyStoreAlias
        get() = dotenv()["KEYSTORE_ALIAS"]!!

    val keyStorePassword
        get() = dotenv()["KEYSTORE_PASSWORD"]!!

    val socketsPort
        get() = dotenv()["SOCKETS_PORT"]!!.toInt()

    val socketsPortOutside
        get() = dotenv()["SOCKETS_PORT_OUTSIDE"] ?: socketsPort

    val adminPassword
        get() = dotenv()["ADMIN_PW"]
}