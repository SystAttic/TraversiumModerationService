package traversium.moderation.client

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

    fun analyze(text: String): AzureModerationResponse =
        client.post()
            .uri("/contentsafety/text:analyze?api-version=${props.apiVersion}")
            .bodyValue(mapOf("text" to text, "categories" to categories, "outputType" to outputType))
            .retrieve()
            .bodyToMono(AzureModerationResponse::class.java)
            .block()!!
}
