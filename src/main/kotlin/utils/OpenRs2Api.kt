package utils

import com.fasterxml.jackson.databind.ObjectMapper
import models.openrs2.OpenRs2Cache
import models.openrs2.OpenRs2CacheKeys
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenRs2Api {

    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build()

    fun fetchCaches(): Array<OpenRs2Cache> {
        val request = buildHttpRequest()
            .uri(URI.create("$OPENRS2_URL/caches.json"))
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.checkStatusCode()
        return ObjectMapper().readValue(response.body(), Array<OpenRs2Cache>::class.java) ?: emptyArray()
    }

    fun fetchCacheKeysById(scope: String, id: String): Array<OpenRs2CacheKeys> {
        val request = buildHttpRequest()
            .uri(URI.create("$OPENRS2_URL/caches/$scope/$id/keys.json"))
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.checkStatusCode()
        return ObjectMapper().readValue(response.body(), Array<OpenRs2CacheKeys>::class.java) ?: emptyArray()
    }

    fun fetchCacheZipById(scope: String, id: String): InputStream {
        val request = buildHttpRequest()
            .uri(URI.create("$OPENRS2_URL/caches/$scope/$id/disk.zip"))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.checkStatusCode()
        return response.body()
    }

    private fun buildHttpRequest(): HttpRequest.Builder =
        HttpRequest.newBuilder()
            .header("user-agent", "OSRS-Environment-Exporter")

    companion object {
        private const val OPENRS2_URL = "https://archive.openrs2.org"

        private fun <T> HttpResponse<T>.checkStatusCode() {
            if (statusCode() !in 200..299) {
                throw Exception("HTTP request to ${uri()} failed with status code: ${statusCode()}")
            }
        }
    }
}
