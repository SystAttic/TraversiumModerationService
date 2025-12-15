package traversium.moderation.service

import org.springframework.stereotype.Service
import traversium.moderation.client.AzureContentSafetyClient
import traversium.moderation.config.ModerationPolicyProperties
import traversium.moderation.dto.*

@Service
class TextModerationService(
    private val azureClient: AzureContentSafetyClient,
    private val policy: ModerationPolicyProperties
) {

    fun moderate(request: ModerateTextRequestDto): ModerateTextResponseDto {

        val result = azureClient.analyze(request.text)
        val maxSeverity = result.categoriesAnalysis.maxOfOrNull { it.severity } ?: 0
        val blocked = maxSeverity >= policy.blockSeverity ||
                result.blocklistsMatch.isNotEmpty()

        return ModerateTextResponseDto(
            allowed = !blocked,
            maxSeverity = maxSeverity,
            categories = result.categoriesAnalysis.map {
                CategoryResultDto(it.category, it.severity)
            },
            blocklistHits = result.blocklistsMatch.map {
                BlocklistHitDto(it.blocklistName, it.blocklistItemText)
            },
            decisionReason = if (blocked) "SEVERITY_THRESHOLD_EXCEEDED" else null
        )
    }
}
