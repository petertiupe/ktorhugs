package de.tiupe.ktorclient

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking


fun main() {
    // Es gibt keinen Grund, den Client immer wieder neu zu erzeugen, daher
    // wird er hier als Objekt erzeugt.
    val ktorClient = HttpClient() {
        // Dies ist Beispielcode dafür, wie man den HTTP-Client konfigurieren kann.
        // Der Client bricht ab, wenn der Status - Code >= 300 ist
        expectSuccess = true
        followRedirects
        // Der nächste Teil dient dazu, die Engine hinter dem HTTP-Client zu konfigurieren.
        engine {
            // Dies ist nur ein Beispiel für einen Konfigurationseintrag
            // Der Default ist 4, daher kann man damit nicht viel kaputt machen ;-)
            this.threadsCount = 5
        }
        // Dies ist wie beim Server ein Beispiel dafür, wie man den Client mit neuer
        // Funktionalität ausstattet. In diesem Fall wird der Client um ein Logging erweitert
        // Man sieht die Möglichkeiten zur Erweiterung sehr gut, wenn man die
        // install-Fkt mit Codeergänzung aufruft.
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
            filter { request ->
                // so kann man die Logs filtern
                request.url.host.contains("tiupe")
            }
        }

        install(UserAgent) {
            // der Wert ist der Default-Wert, möchte nur zeigen, was hier geht.
            this.agent = "Ktor http-client"
        }
    }
    runBlocking {
        // Auch diese beiden Calls laufen komplett synchron ab.
        callTodosWithDigestAuth()
        // callTiupe(ktorClient)
        // callPeter(ktorClient)
        // Nach der Verwendung des Clients sollte man diesen schließen oder
        // nur mit "use" arbeiten, dann wird er automatisch geschlossen.
        // Dann kann man den Client aber nur einmalig verwenden und bekommt ansonten
        // die Fehlermeldung:
        //      "Parent job is Completed;"
        // Hier funktioniert dies nur, weil nur der zweite Aufruf mit "use" erfolgt
        // Das Erzeugen eines Clients ist teuer, man sollte daher immer besser die Instanz
        // erhalten, wenn man mehrere davon braucht, als sie jedes Mal neu zu erzeugen.
        ktorClient.close()

    }
    println("Die Calls wurde erfolgreich ausgeführt...")
}

suspend fun callTiupe(ktorClient: HttpClient) {
    // Der Aufruf hier erfolgt vollständig synchron, nur weil ich das immer wieder gerne
    // vergesse.
    // Wenn man den Network - Client (hier Apache) weglässt, versucht der ktor-client
    // diesen automatisch anhand der Abhängigkeiten zu setzen.
    val response = ktorClient.get("http://www.tiupe.de")
    println(response.bodyAsText())
    println("Status-Code der Antwort ist: ${response.status}")

}

suspend fun callPeter(ktorClient: HttpClient) {
    ktorClient.use {
        val responsePeter: HttpResponse = it.get("http://www.tiupe.de/peter")
        // println(responsePeter.bodyAsText())
        println("Status-Code der Antwort Peter ist: ${responsePeter.status}")
    }
}

suspend fun callTodosWithDigestAuth() {
    // Die Erklärung für den Flow ist auf der Seite https://ktor.io/docs/digest-client.html nachzulesen
    val client = HttpClient() {
        install(Auth) {
            digest {
                credentials {
                    DigestAuthCredentials(username = "peter", password = "pw1")
                }
                realm = "/"
            }
        }
    }

    val urlToCall = "http://localhost:8081/todos"
    val response = client.get {
        accept(ContentType.Application.Json)
        charset("UTF-8")
        url(urlToCall)
    }
    println(response.bodyAsText())

}

