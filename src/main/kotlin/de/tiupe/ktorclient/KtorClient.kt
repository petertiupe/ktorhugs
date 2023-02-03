package de.tiupe.ktorclient

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking


fun main() {
    // Es gibt keinen Grund, den Client immer wieder neu zu erzeugen, daher
    // wird er hier als Objekt erzeugt.
    val ktorClient = HttpClient() {
        // Dies ist Beispielcode dafür, wie man den HTTP-Client konfigurieren kann.
        // Der Client bricht ab, wenn der Status - Code >= 300 ist
        expectSuccess = true
        followRedirects
    }
        runBlocking {
            callTiupe(ktorClient)
            println("tiupe called")
        }
    println("Der Call wurde erfolgreich ausgeführt...")
}

suspend fun callTiupe(ktorClient: HttpClient){
    // Der Aufruf hier erfolgt vollständig snychron, nur weil ich das immer wieder gerne
    // vergesse.
    // Wenn man den Network - Client (hier Apache) weglässt, versucht der ktor-client
    // diesen automatisch anhand der Abhängigkeiten zu setzen.

    val response: HttpResponse = ktorClient.get("http://www.tiupe.de")
    println(response.bodyAsText())
    println("Status-Code der Antwort ist: ${response.status}")
}