package de.tiupe.ktorclient

import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking


fun main() {
    runBlocking {
        val myKtorClient = HttpClient(Apache)
        val response: HttpResponse = myKtorClient.get("http://www.tiupe.de")
        println(response.bodyAsText())
    }
    println("Der Call wurde erfolgreich ausgeführt...")
}