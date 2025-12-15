package traversium.moderation.grpc

import org.springframework.grpc.server.service.GrpcService
import traversium.moderation.textmoderation.*
import traversium.moderation.service.TextModerationService
import traversium.moderation.dto.ModerateTextRequestDto

@GrpcService
class TextModerationGrpcService(
    private val moderationService: TextModerationService
) : TextModerationServiceGrpc.TextModerationServiceImplBase() {

    override fun moderateText(
        request: ModerateTextRequest,
        responseObserver: io.grpc.stub.StreamObserver<ModerateTextResponse>
    ) {
        val dto = ModerateTextRequestDto(
            text = request.text,
        )

        val result = moderationService.moderate(dto)

        val response = ModerateTextResponse.newBuilder()
            .setAllowed(result.allowed)
            .setMaxSeverity(result.maxSeverity)
            .setDecisionReason(result.decisionReason ?: "")
            .addAllCategories(
                result.categories.map {
                    CategoryResult.newBuilder()
                        .setCategory(it.category)
                        .setSeverity(it.severity)
                        .build()
                }
            )
            .addAllBlocklistHits(
                result.blocklistHits.map {
                    BlocklistHit.newBuilder()
                        .setBlocklistName(it.blocklistName)
                        .setMatchedText(it.matchedText)
                        .build()
                }
            )
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
