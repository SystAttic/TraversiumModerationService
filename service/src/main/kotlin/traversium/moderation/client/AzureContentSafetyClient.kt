package traversium.moderation.client

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.apache.logging.log4j.kotlin.logger
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import traversium.moderation.config.AzureContentSafetyProperties

@Component
class AzureContentSafetyClient(
    private val props: AzureContentSafetyProperties,
    private val categories: List<String> = listOf("Hate", "Sexual", "SelfHarm", "Violence"),
    private val outputType: String = "EightSeverityLevels",
) {

    private val client = WebClient.builder()
        .baseUrl(props.endpoint)
        .defaultHeader("Ocp-Apim-Subscription-Key", props.apiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    @CircuitBreaker(name = "azureModeration", fallbackMethod = "fallback")
    @Retry(name = "azureModeration")
    fun analyze(text: String): AzureModerationResponse {
        logger.info("Calling Azure Content Safety")
        return client.post()
            .uri("/contentsafety/text:analyze?api-version=${props.apiVersion}")
            .bodyValue(mapOf("text" to text, "categories" to categories, "outputType" to outputType))
            .retrieve()
            .bodyToMono(AzureModerationResponse::class.java)
            .block()!!
    }

    private fun fallback(text: String, ex: Throwable): AzureModerationResponse {
        logger.error("Azure moderation failed: ${ex.message}")
        return AzureModerationResponse.safeFallback()
    }
}
