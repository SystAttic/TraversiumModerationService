package traversium.moderation.security

import io.grpc.ServerInterceptor
import org.springframework.grpc.server.GlobalServerInterceptor

@GlobalServerInterceptor
class GlobalGrpcInterceptor(
    private val authInterceptor: GrpcAuthInterceptor
) : ServerInterceptor by authInterceptor
