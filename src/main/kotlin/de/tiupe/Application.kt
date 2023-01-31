package de.tiupe

import io.ktor.server.application.*
import io.ktor.server.netty.*
import de.tiupe.plugins.*


fun main(args: Array<String>) {
    EngineMain.main(args)
    /*embeddedServer(Netty) {
        module()
    }
        .start(wait = true)
    */
}


fun Application.module(testing: Boolean = false) {
    configureSecurity()
    configureRouting()
}
