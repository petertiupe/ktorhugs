package de.tiupe.plugins

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    // hier passiert dasselbe noch einmal wie vorn es werden Plugins installiert, hier sind es die
    // Status-Pages
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }


    // Die von der App zu erledigenden Anfragen werden von einer Pipeline verarbeitet. Auf diesem
    // Pipeline-Objekt sind die Routing-Funktionen wie get, post... definiert.

    // Das ApplicationCall-Objekt übernimmt die Daten für den Request, also für die get, post...
    // Funktionen...

    // Das Request-Objekt bündelt die Daten in einem Objekt.


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
