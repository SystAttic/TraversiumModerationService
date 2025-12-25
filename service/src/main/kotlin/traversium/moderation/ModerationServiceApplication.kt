package traversium.moderation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [KafkaAutoConfiguration::class])
class ModerationServiceApplication

fun main(args: Array<String>) {
	runApplication<ModerationServiceApplication>(*args)
}
