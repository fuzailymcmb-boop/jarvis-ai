package com.jarvis.assistant.ai

import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.data.AiTurn
import com.jarvis.assistant.security.BackendEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

/** Only general chat is sent to the user's configured server. */
class AiClient {
    suspend fun ask(endpoint: String, authToken: String, message: String, history: List<AiTurn>): String =
        withContext(Dispatchers.IO) {
            val url = BackendEndpoint.parse(endpoint, BuildConfig.DEBUG)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                // Never forward the bearer token to a redirect destination.
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $authToken")
            }
            try {
                val historyArray = JSONArray()
                history.takeLast(12).forEach {
                    historyArray.put(JSONObject().put("role", it.role).put("content", it.content.take(1500)))
                }
                val payload = JSONObject()
                    .put("message", message.take(2000))
                    .put("history", historyArray)
                    .toString().toByteArray(Charsets.UTF_8)
                connection.outputStream.use { it.write(payload) }
                val status = connection.responseCode
                val body = if (status in 200..299) connection.inputStream.bufferedReader().use { it.readText() }
                else connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (status !in 200..299) {
                    val reason = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                    error("AI service returned $status${if (reason.isBlank()) "" else ": $reason"}")
                }
                JSONObject(body).optString("reply").takeIf { it.isNotBlank() }
                    ?: error("AI service returned no text.")
            } finally { connection.disconnect() }
        }
}
