package traversium.moderation.client

data class AzureModerationResponse(
    val categoriesAnalysis: List<CategoryAnalysis> = emptyList(),
    val blocklistsMatch: List<BlocklistMatch> = emptyList()
)

data class CategoryAnalysis(
    val category: String,
    val severity: Int
)

data class BlocklistMatch(
    val blocklistName: String,
    val blocklistItemText: String
)
