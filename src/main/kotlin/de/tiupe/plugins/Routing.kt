package de.tiupe.plugins

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.resources.*
import io.ktor.server.resources.Resources
import kotlinx.serialization.Serializable
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import kotlinx.html.*

fun Application.configureRouting() {

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(Resources)
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
        get<Articles> { article ->
            // Get all articles ...
            call.respond("List of articles sorted starting from ${article.sort}")
        }

        get("/peterWars"){
            call.respondText { "Peter ist der Beste" }
        }

        get("/xxx") {
            call.response.cookies.append("petersCookie", "Peterwars")
            call.respond("Cookie gesetzt")
        }

        // gibt einfach den Inhalt des Payloads zurück...
        post("/yyy") {
            val data = call.receive<String>()
            call.respondText("received data :  $data", ContentType.Text.Plain)

        }
        get("/html-dsl") {
            val name = "Ktor"
            call.respondHtml(HttpStatusCode.OK) {
                body {
                    h1 {
                        +"Hello from $name!"
                    }
                        a("http://www.tiupe.de"){
                            + "hier gehts zu Tiupe"
                        }
                    }

            }
        }

    }
}

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
