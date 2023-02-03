package de.tiupe.ktorclient

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking


fun main() {
        runBlocking{
            callTiupe()
            println("tiupe called")
        }
    println("Der Call wurde erfolgreich ausgeführt...")
}

suspend fun callTiupe(){
    // Der Aufruf hier erfolgt vollständig snychron, nur weil ich das immer wieder gerne
    // vergesse.
    val myKtorClient = HttpClient(Apache)
    val response: HttpResponse = myKtorClient.get("http://www.tiupe.de")
    println(response.bodyAsText())
    println("Status-Code der Antwort ist: ${response.status}")
}