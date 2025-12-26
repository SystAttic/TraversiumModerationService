package traversium.moderation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import traversium.moderation.client.*
import traversium.moderation.config.ModerationPolicyProperties
import traversium.moderation.dto.ModerateTextRequestDto
import traversium.moderation.service.TextModerationService

@ExtendWith(MockitoExtension::class)
class ModerationServiceTest {

    @Mock
    lateinit var azureClient: AzureContentSafetyClient

    private lateinit var policy: ModerationPolicyProperties

    private lateinit var service: TextModerationService

    @BeforeEach
    fun setup() {
        policy = ModerationPolicyProperties(
            blockSeverity = 4
        )
        service = TextModerationService(azureClient, policy)
    }

    private fun azureResponse(
        severities: List<Int>,
        blocklistHits: Boolean = false
    ): AzureModerationResponse =
        AzureModerationResponse(
            categoriesAnalysis = severities.mapIndexed { i, s ->
                CategoryAnalysis("Category$i", s)
            },
            blocklistsMatch = if (blocklistHits)
                listOf(BlocklistMatch("profanity", "badword"))
            else emptyList()
        )

    @Test
    fun `allows text when severity below threshold`() {
        `when`(azureClient.analyze(anyString()))
            .thenReturn(azureResponse(listOf(1, 2, 3)))

        val result = service.moderate(
            ModerateTextRequestDto("hello world")
        )

        assertTrue(result.allowed)
        assertEquals(3, result.maxSeverity)
        assertNull(result.decisionReason)
    }

    @Test
    fun `blocks text when severity exceeds threshold`() {
        `when`(azureClient.analyze(anyString()))
            .thenReturn(azureResponse(listOf(2, 5)))

        val result = service.moderate(
            ModerateTextRequestDto("toxic text")
        )

        assertFalse(result.allowed)
        assertEquals(5, result.maxSeverity)
        assertEquals("SEVERITY_THRESHOLD_EXCEEDED", result.decisionReason)
    }

    @Test
    fun `blocks text when blocklist hit exists`() {
        `when`(azureClient.analyze(anyString()))
            .thenReturn(azureResponse(listOf(1), blocklistHits = true))

        val result = service.moderate(
            ModerateTextRequestDto("badword")
        )

        assertFalse(result.allowed)
        assertEquals(1, result.maxSeverity)
        assertEquals(1, result.blocklistHits.size)
    }

    @Test
    fun `throws when Azure client throws`() {
        `when`(azureClient.analyze(anyString()))
            .thenThrow(RuntimeException("Azure down"))

        val ex = assertThrows<RuntimeException> {
            service.moderate(ModerateTextRequestDto("any text"))
        }

        assertEquals("Azure down", ex.message)
    }

    @Test
    fun `blocks text when severity equals threshold`() {
        `when`(azureClient.analyze(anyString()))
            .thenReturn(azureResponse(listOf(4)))

        val result = service.moderate(
            ModerateTextRequestDto("borderline text")
        )

        assertFalse(result.allowed)
        assertEquals(4, result.maxSeverity)
        assertEquals("SEVERITY_THRESHOLD_EXCEEDED", result.decisionReason)
    }

    @Test
    fun `allows text when no categories returned`() {
        `when`(azureClient.analyze(anyString()))
            .thenReturn(
                AzureModerationResponse(
                    categoriesAnalysis = emptyList(),
                    blocklistsMatch = emptyList()
                )
            )

        val result = service.moderate(
            ModerateTextRequestDto("neutral text")
        )

        assertTrue(result.allowed)
        assertEquals(0, result.maxSeverity)
        assertNull(result.decisionReason)
    }

    @Test
    fun `blocklist hit blocks even when severity is zero`() {
        `when`(azureClient.analyze(anyString()))
            .thenReturn(azureResponse(listOf(0), blocklistHits = true))

        val result = service.moderate(
            ModerateTextRequestDto("blocked text")
        )

        assertFalse(result.allowed)
        assertEquals("SEVERITY_THRESHOLD_EXCEEDED", result.decisionReason)
    }

    @Test
    fun `changing policy severity affects decision`() {
        policy.blockSeverity = 2

        `when`(azureClient.analyze(anyString()))
            .thenReturn(azureResponse(listOf(3)))

        val result = service.moderate(
            ModerateTextRequestDto("policy sensitive")
        )

        assertFalse(result.allowed)
        assertEquals(3, result.maxSeverity)
    }

    @Test
    fun `uses highest category severity`() {
        `when`(azureClient.analyze(anyString()))
            .thenReturn(azureResponse(listOf(1, 7, 3)))

        val result = service.moderate(
            ModerateTextRequestDto("mixed content")
        )

        assertFalse(result.allowed)
        assertEquals(7, result.maxSeverity)
    }

    @Test
    fun `handles blank input safely`() {
        `when`(azureClient.analyze(""))
            .thenReturn(azureResponse(listOf(0)))

        val result = service.moderate(
            ModerateTextRequestDto("")
        )

        assertTrue(result.allowed)
    }



}