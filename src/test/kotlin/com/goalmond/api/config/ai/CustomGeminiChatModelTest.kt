package com.goalmond.api.config.ai

import com.goalmond.api.service.ai.GeminiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.messages.UserMessage

/**
 * CustomGeminiChatModel 단위 테스트 (GeminiClient 모킹).
 */
class CustomGeminiChatModelTest {

    @Test
    fun `call은 Prompt에서 메시지를 추출하여 GeminiClient 호출 후 ChatResponse 반환`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient> {
            on { generateContent("안녕", 0.5, 0.9) }.thenReturn("안녕하세요")
        }
        val chatModel = CustomGeminiChatModel(geminiClient)

        val prompt = Prompt(UserMessage("안녕"))
        val response = chatModel.call(prompt)

        assertThat(response).isNotNull
        assertThat(response.results).hasSize(1)
        assertThat(response.result.output.content).isEqualTo("안녕하세요")
    }

    @Test
    fun `빈 Prompt 시 IllegalArgumentException`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient>()
        val chatModel = CustomGeminiChatModel(geminiClient)

        val prompt = Prompt(emptyList())

        assertThrows<IllegalArgumentException> {
            chatModel.call(prompt)
        }
    }

    @Test
    fun `stream 호출 시 UnsupportedOperationException`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient>()
        val chatModel = CustomGeminiChatModel(geminiClient)

        assertThrows<UnsupportedOperationException> {
            chatModel.stream(Prompt(UserMessage("test")))
        }
    }

    @Test
    fun `getDefaultOptions는 null이 아닌 ChatOptions 반환`() {
        val geminiClient = org.mockito.kotlin.mock<GeminiClient>()
        val chatModel = CustomGeminiChatModel(geminiClient)

        val options = chatModel.defaultOptions

        assertThat(options).isNotNull
        assertThat(options.model).isNull()
        assertThat(options.temperature).isNull()
    }
}
