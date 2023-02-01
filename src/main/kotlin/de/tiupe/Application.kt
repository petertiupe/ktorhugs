package de.tiupe

import de.tiupe.plugins.configureRouting
import de.tiupe.plugins.configureSecurity
import de.tiupe.todo.configureToDoRouting
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import kotlinx.serialization.json.Json
import org.slf4j.event.*


fun main(args: Array<String>) {
    EngineMain.main(args)
    /*embeddedServer(Netty) {
        module()
    }
        .start(wait = true)
    */
}

@Suppress("unused")
fun Application.module(testing: Boolean = false) {
    // Auf die hier gezeigte Art und Weise kann man in ktor Features installieren.
    // ===========================================================================

    // Feature um den Server per URL zu stoppen
    install(ShutDownUrl.ApplicationCallPlugin) {
        // The URL that will be intercepted (you can also use the application.conf's ktor.deployment.shutdown.url key)
        shutDownUrl = "/ktor/application/shutdown"
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = {
            println("Peter Wars und Tschüüüüüüüüsssss")
            0
        } // ApplicationCall.() -> Int
    }

    // Feature um Header zu setzen
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
        header("MyPeter", "Tor")
    }

    // Feature um alle Requests zu loggen
    // wirft bei Posts dann einen Fehler, weil der Request bereits empfangen wurde...
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    // Json-Serialisierung via kotlinx-serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            encodeDefaults = true
        })
    }

    install(Resources)

    install(DoubleReceive) {

    }

    configureSecurity()
    configureRouting()
    configureToDoRouting()
}

