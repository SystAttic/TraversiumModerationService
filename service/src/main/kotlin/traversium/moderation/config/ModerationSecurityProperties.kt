package traversium.moderation.config


import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Configuration

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.moderation")
data class ModerationSecurityProperties(
    var requiredRole: String = ""
)