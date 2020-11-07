package io.pokr.config

import io.pokr.*
import io.github.cdimascio.dotenv.*
import java.io.*

object PokrioConfig {

    fun exists() =
        File(".env").exists()

    val version
        get() = BuildConfig.LAST_COMMIT

    val webUrl
        get() = dotenv()["WEB_URL"]!!

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

    val adminPassword
        get() = dotenv()["ADMIN_PW"]
}