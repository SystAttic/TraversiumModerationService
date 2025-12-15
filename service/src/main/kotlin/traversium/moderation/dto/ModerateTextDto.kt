package traversium.moderation.dto

data class ModerateTextRequestDto(
    val text: String,
)

data class CategoryResultDto(
    val category: String,
    val severity: Int
)

data class BlocklistHitDto(
    val blocklistName: String,
    val matchedText: String
)

data class ModerateTextResponseDto(
    val allowed: Boolean,
    val maxSeverity: Int,
    val categories: List<CategoryResultDto>,
    val blocklistHits: List<BlocklistHitDto>,
    val decisionReason: String?
)
