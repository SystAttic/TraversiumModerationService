package traversium.moderation.security

import io.grpc.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import traversium.moderation.config.ModerationSecurityProperties

@Component
class GrpcAuthInterceptor(
    private val props: ModerationSecurityProperties
) : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {

        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null || !authentication.isAuthenticated) {
            return unauthenticated(call, "No authentication found in security context")
        }

        val jwt = authentication.principal as? Jwt
            ?: return unauthenticated(call, "Invalid principal: Expected JWT")

        val realmAccess = jwt.getClaimAsMap("realm_access")
        val roles = realmAccess?.get("roles") as? Collection<*>
            ?: emptyList<String>()

        if (!roles.contains(props.requiredRole)) {
            return forbidden(call, "Caller (microservice) lacks required role to access this resource.")
        }

        return next.startCall(call, headers)
    }

    // Helper to close the call with UNAUTHENTICATED status
    private fun <ReqT, RespT> unauthenticated(call: ServerCall<ReqT, RespT>, message: String): ServerCall.Listener<ReqT> {
        call.close(Status.UNAUTHENTICATED.withDescription(message), Metadata())
        return object : ServerCall.Listener<ReqT>() {}
    }

    // Helper to close the call with PERMISSION_DENIED status
    private fun <ReqT, RespT> forbidden(call: ServerCall<ReqT, RespT>, message: String): ServerCall.Listener<ReqT> {
        call.close(Status.PERMISSION_DENIED.withDescription(message), Metadata())
        return object : ServerCall.Listener<ReqT>() {}
    }
}