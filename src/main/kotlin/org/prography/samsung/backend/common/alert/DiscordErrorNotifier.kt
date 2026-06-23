package org.prography.samsung.backend.common.alert

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

@Component
class DiscordErrorNotifier(@param:Value("\${alert.discord.webhook-url:}") private val webhookUrl: String) {
    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("discord-error-notifier"),
    )
    private val restClient: RestClient = RestClient.builder().build()
    private val log = LoggerFactory.getLogger(DiscordErrorNotifier::class.java)

    fun notifyError(
        throwable: Throwable,
        uid: String? = null,
        path: String? = null,
        method: String? = null,
        errorCode: String? = null,
    ) {
        if (webhookUrl.isBlank()) {
            return
        }

        scope.launch {
            runCatching {
                val payload = buildPayload(throwable, uid, path, method, errorCode)
                send(payload)
            }.onFailure { e ->
                log.warn("Discord error alert failed: {}", e.message)
            }
        }
    }

    private fun buildPayload(
        throwable: Throwable,
        uid: String?,
        path: String?,
        method: String?,
        errorCode: String?,
    ): Map<String, Any> {
        val stackTrace = formatStackTrace(throwable)
        val timestamp = Instant.now().toString()

        val fields = mutableListOf<Map<String, Any>>()

        val codeValue = errorCode ?: throwable::class.simpleName ?: "Exception"
        fields += mapOf(
            "name" to "에러 코드",
            "value" to codeValue,
            "inline" to true,
        )

        if (!uid.isNullOrBlank()) {
            fields += mapOf(
                "name" to "UID",
                "value" to uid,
                "inline" to true,
            )
        }

        if (!method.isNullOrBlank() || !path.isNullOrBlank()) {
            val endpoint = buildString {
                if (!method.isNullOrBlank()) append(method)
                if (!path.isNullOrBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(path)
                }
            }
            fields += mapOf(
                "name" to "엔드포인트",
                "value" to endpoint,
                "inline" to false,
            )
        }

        fields += mapOf(
            "name" to "메시지",
            "value" to truncate(throwable.message ?: "No message", 1000),
            "inline" to false,
        )

        val description = buildString {
            append("**스택 트레이스**\n")
            append("```\n")
            append(truncate(stackTrace, 3200))
            append("\n```")
        }

        val embed = mutableMapOf<String, Any>(
            "title" to "🚨 서버 에러 발생",
            "color" to 0xED4245,
            "fields" to fields,
            "description" to description,
            "timestamp" to timestamp,
        )

        return mapOf(
            "embeds" to listOf(embed),
        )
    }

    private fun formatStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        PrintWriter(sw).use { pw ->
            throwable.printStackTrace(pw)
        }
        return sw.toString()
    }

    private fun truncate(text: String, max: Int): String =
        if (text.length <= max) text else text.take(max) + "\n... (truncated)"

    private fun send(payload: Map<String, Any>) {
        try {
            restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientResponseException) {
            log.warn("Discord webhook responded with status {}: {}", e.statusCode, e.responseBodyAsString.take(200))
            throw e
        }
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }
}
