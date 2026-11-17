package com.example

import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiAssistantViewModelTest {

    @Test
    fun `buildSanitizedHistory keeps only paired user-model turns and drops failed user-only entries`() {
        val vm = AiAssistantViewModel()
        val messages = listOf(
            AiChatMessage(id = 1, isUser = true, text = "Pertanyaan 1"),
            AiChatMessage(id = 2, isUser = false, text = "Jawaban 1"),
            AiChatMessage(id = 3, isUser = true, text = "Pertanyaan gagal"),
            AiChatMessage(id = 4, isUser = true, text = "Pertanyaan 2"),
            AiChatMessage(id = 5, isUser = false, text = "Jawaban 2"),
            AiChatMessage(id = 6, isUser = true, text = "Pertanyaan baru belum terjawab")
        )

        val sanitized = vm.buildSanitizedHistory(messages)
        assertEquals(4, sanitized.size)
        assertEquals("user", sanitized[0].role)
        assertEquals("model", sanitized[1].role)
        assertEquals("user", sanitized[2].role)
        assertEquals("model", sanitized[3].role)
    }

    @Test
    fun `cancelCurrentRequest and clearConversation reset state cleanly`() {
        val vm = AiAssistantViewModel()
        vm.cancelCurrentRequest()
        assertTrue(vm.uiState.value is AiUiState.Idle)

        vm.clearConversation()
        assertTrue(vm.uiState.value is AiUiState.Idle)
        assertTrue(vm.messages.value.isEmpty())
    }
}
