package traversium.moderation.dto

data class ModerateTextRequestDto(val text: String, val language: String? = null)
data class ModerateTextResponseDto(val labels: Map<String, Double>, val blocked: Boolean, val reason: String?)
