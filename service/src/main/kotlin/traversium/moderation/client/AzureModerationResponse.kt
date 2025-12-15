package traversium.moderation.client

data class AzureModerationResponse(
    val categoriesAnalysis: List<CategoryAnalysis> = emptyList(),
    val blocklistsMatch: List<BlocklistMatch> = emptyList()
) {
    companion object {
        fun safeFallback() = AzureModerationResponse(
            categoriesAnalysis = emptyList(),
            blocklistsMatch = emptyList()
        )
    }
}

data class CategoryAnalysis(
    val category: String,
    val severity: Int
)

data class BlocklistMatch(
    val blocklistName: String,
    val blocklistItemText: String
)
