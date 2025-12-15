package traversium.moderation.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "azure.contentsafety")
data class AzureContentSafetyProperties(
    var endpoint: String = "",
    var apiKey: String = "",
    var apiVersion: String = ""
)

@Configuration
@ConfigurationProperties(prefix = "moderation.policy")
data class ModerationPolicyProperties(
    var blockSeverity: Int = 0
)