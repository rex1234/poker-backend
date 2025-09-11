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
        get() = "1.1"

    val webDir
        get() = "web"

    val webUrl
        get() = dotenv()["WEB_URL"]!!

    val socketUrl
        get() = dotenv()["SOCKET_URL"] ?: webUrl

    val webPort
        get() = dotenv()["WEB_PORT"]!!.toInt()

    val socketsPort
        get() = dotenv()["SOCKETS_PORT"]!!.toInt()

    val socketsPortOutside
        get() = dotenv()["SOCKETS_PORT_OUTSIDE"] ?: socketsPort
}