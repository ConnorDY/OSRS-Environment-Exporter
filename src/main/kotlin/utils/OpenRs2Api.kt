package utils

import com.fasterxml.jackson.databind.ObjectMapper
import models.openrs2.OpenRs2Cache
import models.openrs2.OpenRs2CacheKeys
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenRs2Api {

    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build()

    fun getCaches(): Array<OpenRs2Cache> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$OPENRS2_URL/caches.json"))
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw Exception("HTTP request to $OPENRS2_URL failed with status code: ${response.statusCode()}")
        }

        return ObjectMapper().readValue(response.body(), Array<OpenRs2Cache>::class.java)
    }

    fun getCacheKeysById(id: String): Array<OpenRs2CacheKeys>? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$OPENRS2_URL/caches/runescape/$id/keys.json"))
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw Exception("HTTP request to $OPENRS2_URL failed with status code: ${response.statusCode()}")
        }

        return ObjectMapper().readValue(response.body(), Array<OpenRs2CacheKeys>::class.java)
    }

    companion object {
        private const val OPENRS2_URL = "https://archive.openrs2.org"
    }
}
