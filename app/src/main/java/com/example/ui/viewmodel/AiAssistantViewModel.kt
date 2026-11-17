package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import com.example.ui.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.UnknownHostException

private const val TAG = "AiAssistantViewModel"
private const val PRIMARY_FAST_MODEL = "gemini-3.5-flash-lite"
private const val FALLBACK_MODEL = "gemini-3.8-flash"
private const val AI_REQUEST_TIMEOUT_MS = 45_000L

data class AiChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiAssistantViewModel : ViewModel() {

    private val systemPromptContent by lazy {
        content {
            text(
                """
                Anda adalah Asisten Usaha UMKM yang ramah, praktis, dan cerdas untuk aplikasi pembukuan toko bernama SakuUsaha.
                Jawablah selalu dalam bahasa Indonesia yang jelas, ringkas, terstruktur (gunakan poin bila perlu), dan langsung dapat dipraktikkan oleh pemilik toko/warung/UMKM.
                Jika ringkasan data usaha disertakan dalam pesan, gunakan angka nyata tersebut untuk menganalisis omset, pengeluaran, laba, stok menipis, HPP & margin produk, serta utang/piutang.
                Jangan mengarang angka yang tidak ada di data.
                """.trimIndent()
            )
        }
    }

    // Ultra-fast primary model with minimal thinking latency for responsive UMKM chat
    private val fastModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = PRIMARY_FAST_MODEL,
            generationConfig = generationConfig {
                maxOutputTokens = 1024
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            },
            systemInstruction = systemPromptContent,
            requestOptions = RequestOptions(AI_REQUEST_TIMEOUT_MS)
        )
    }

    // Fallback model with low thinking level for resiliency
    private val fallbackModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = generationConfig {
                maxOutputTokens = 1024
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            systemInstruction = systemPromptContent,
            requestOptions = RequestOptions(AI_REQUEST_TIMEOUT_MS)
        )
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private var lastSubmittedPrompt: String? = null
    private var lastSubmittedContext: String = ""
    private var activeRequestJob: Job? = null

    fun sendMessage(
        userPrompt: String,
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        products: List<ProductEntity>,
        lowStockProducts: List<ProductEntity>,
        activeDebts: List<TransactionEntity>
    ) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Streaming) return

        val historyBeforeTurn = _messages.value
        val userMsg = AiChatMessage(
            id = System.currentTimeMillis(),
            isUser = true,
            text = trimmed
        )
        _messages.value = historyBeforeTurn + userMsg
        lastSubmittedPrompt = trimmed

        activeRequestJob?.cancel()
        activeRequestJob = viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val snapshot = withContext(Dispatchers.Default) {
                buildBusinessContext(
                    profile = profile,
                    transactions = transactions,
                    products = products,
                    lowStockProducts = lowStockProducts,
                    activeDebts = activeDebts
                )
            }
            lastSubmittedContext = snapshot
            executeStreamingTurn(
                prompt = trimmed,
                businessSnapshot = snapshot,
                historyMessages = historyBeforeTurn
            )
        }
    }

    fun retryLastMessage() {
        val prompt = lastSubmittedPrompt ?: return
        if (_uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Streaming) return

        // Keep only completed pairs before the failed turn so history never has consecutive user messages
        val messagesWithoutTrailingAssistant = _messages.value.dropLastWhile { !it.isUser }
        val historyBeforeTurn = messagesWithoutTrailingAssistant.dropLast(1)

        activeRequestJob?.cancel()
        activeRequestJob = viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            executeStreamingTurn(
                prompt = prompt,
                businessSnapshot = lastSubmittedContext,
                historyMessages = historyBeforeTurn
            )
        }
    }

    fun cancelCurrentRequest() {
        activeRequestJob?.cancel()
        activeRequestJob = null
        if (_uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Streaming) {
            _uiState.value = AiUiState.Idle
        }
    }

    fun clearConversation() {
        activeRequestJob?.cancel()
        activeRequestJob = null
        _messages.value = emptyList()
        _uiState.value = AiUiState.Idle
        lastSubmittedPrompt = null
        lastSubmittedContext = ""
    }

    private suspend fun executeStreamingTurn(
        prompt: String,
        businessSnapshot: String,
        historyMessages: List<AiChatMessage>
    ) {
        try {
            val chatHistory = buildSanitizedHistory(historyMessages)
            val fullPrompt = if (businessSnapshot.isNotBlank()) {
                """
                [Ringkasan Data Usaha Saat Ini]
                $businessSnapshot

                [Pertanyaan Pemilik Usaha]
                $prompt
                """.trimIndent()
            } else {
                prompt
            }

            val replyText = try {
                streamWithModel(
                    targetModel = fastModel,
                    chatHistory = chatHistory,
                    fullPrompt = fullPrompt
                )
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    Log.w(TAG, "Primary model $PRIMARY_FAST_MODEL timed out, retrying with $FALLBACK_MODEL", e)
                    fallbackUnaryOrStream(chatHistory, fullPrompt)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                if (isNonRetryableAuthOrAppCheckError(e)) {
                    throw e
                }
                Log.w(TAG, "Primary model $PRIMARY_FAST_MODEL failed (${e.message}), retrying with $FALLBACK_MODEL", e)
                fallbackUnaryOrStream(chatHistory, fullPrompt)
            }

            if (replyText.isNotEmpty()) {
                _messages.value = _messages.value + AiChatMessage(
                    id = System.currentTimeMillis() + 1,
                    isUser = false,
                    text = replyText
                )
                _uiState.value = AiUiState.Success(replyText)
            } else {
                _uiState.value = AiUiState.Error("AI tidak memberikan jawaban. Silakan coba lagi.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Assistant request timed out", e)
            _uiState.value = AiUiState.Error(
                "Permintaan melebihi batas waktu. Periksa koneksi internet Anda lalu tekan Coba Lagi."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            _uiState.value = AiUiState.Error(formatUserFriendlyAiError(e))
        }
    }

    private suspend fun streamWithModel(
        targetModel: GenerativeModel,
        chatHistory: List<Content>,
        fullPrompt: String
    ): String {
        val accumulatedText = StringBuilder()
        val chat = targetModel.startChat(history = chatHistory)
        withTimeout(AI_REQUEST_TIMEOUT_MS) {
            chat.sendMessageStream(fullPrompt)
                .flowOn(Dispatchers.IO)
                .collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulatedText.append(chunkText)
                        _uiState.value = AiUiState.Streaming(accumulatedText.toString())
                    }
                }
        }
        return accumulatedText.toString().trim()
    }

    private suspend fun fallbackUnaryOrStream(
        chatHistory: List<Content>,
        fullPrompt: String
    ): String {
        val streamed = try {
            streamWithModel(
                targetModel = fallbackModel,
                chatHistory = chatHistory,
                fullPrompt = fullPrompt
            )
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) "" else throw e
        } catch (e: Exception) {
            if (isNonRetryableAuthOrAppCheckError(e)) throw e
            Log.w(TAG, "Fallback streaming failed, trying unary sendMessage", e)
            ""
        }

        if (streamed.isNotEmpty()) return streamed

        val chat = fallbackModel.startChat(history = chatHistory)
        val response = withTimeout(AI_REQUEST_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                chat.sendMessage(fullPrompt)
            }
        }
        return response.text.orEmpty().trim()
    }

    private fun isNonRetryableAuthOrAppCheckError(e: Throwable): Boolean {
        val msg = e.message.orEmpty().lowercase()
        return msg.contains("403") ||
            msg.contains("app check") ||
            msg.contains("appcheck") ||
            msg.contains("attestation") ||
            msg.contains("api key") ||
            msg.contains("permission_denied")
    }

    private fun formatUserFriendlyAiError(e: Throwable): String {
        val rawMessage = e.localizedMessage ?: e.message.orEmpty()
        val lower = rawMessage.lowercase()
        return when {
            e is UnknownHostException || e is IOException || lower.contains("unable to resolve host") || lower.contains("network") ->
                "Koneksi internet bermasalah. Pastikan perangkat terhubung ke internet lalu coba lagi.\n($rawMessage)"
            lower.contains("403") || lower.contains("app check") || lower.contains("appcheck") || lower.contains("attestation") ->
                "Verifikasi keamanan Firebase App Check gagal. Coba tutup lalu buka ulang aplikasi dari launcher.\n($rawMessage)"
            lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") ->
                "Batas permintaan AI sementara tercapai. Tunggu beberapa saat lalu tekan Coba Lagi.\n($rawMessage)"
            rawMessage.isNotBlank() ->
                "Gagal menghubungi AI Asistan: $rawMessage"
            else ->
                "Gagal menghubungi AI Asistan. Periksa koneksi internet lalu tekan Coba Lagi."
        }
    }

    internal fun buildSanitizedHistory(historyMessages: List<AiChatMessage>): List<Content> {
        val cleanHistory = mutableListOf<Content>()
        var i = 0
        while (i + 1 < historyMessages.size) {
            val first = historyMessages[i]
            val second = historyMessages[i + 1]
            if (first.isUser && !second.isUser && first.text.isNotBlank() && second.text.isNotBlank()) {
                cleanHistory.add(content(role = "user") { text(first.text) })
                cleanHistory.add(content(role = "model") { text(second.text) })
                i += 2
            } else {
                i += 1
            }
        }
        return cleanHistory
    }

    private fun buildBusinessContext(
        profile: BusinessProfileEntity?,
        transactions: List<TransactionEntity>,
        products: List<ProductEntity>,
        lowStockProducts: List<ProductEntity>,
        activeDebts: List<TransactionEntity>
    ): String {
        val totalPenjualan = transactions.filter { it.type in listOf("JUAL", "PENJUALAN") }.sumOf { it.amount }
        val totalBelanja = transactions.filter { it.type in listOf("BELI", "BIAYA") }.sumOf { it.amount }
        val labaBersih = totalPenjualan - totalBelanja

        val produkList = products.filter { it.stockType == "PRODUK" }
        val bahanCount = products.count { it.stockType == "BAHAN" }
        val inventarisCount = products.count { it.stockType == "INVENTARIS" }

        val topProductsText = produkList.take(8).joinToString("; ") { p ->
            val margin = p.sellPrice - p.costPrice
            "${p.name} (Jual ${Formatters.rupiah(p.sellPrice)}, HPP ${Formatters.rupiah(p.costPrice)}, Margin ${Formatters.rupiah(margin)}, Stok ${p.stock} ${p.unit})"
        }.ifEmpty { "Belum ada produk" }

        val lowStockText = lowStockProducts.take(8).joinToString("; ") { item ->
            "${item.name} [${item.stockType}] sisa ${item.stock} ${item.unit} (min ${item.minStockAlert})"
        }.ifEmpty { "Semua stok aman" }

        val debtsSummary = if (activeDebts.isEmpty()) {
            "Tidak ada utang/piutang aktif"
        } else {
            val totalHutang = activeDebts.filter { it.direction == "KELUAR" }.sumOf { it.remainingDebt }
            val totalPiutang = activeDebts.filter { it.direction == "MASUK" }.sumOf { it.remainingDebt }
            "${activeDebts.size} tagihan aktif (Utang ke supplier: ${Formatters.rupiah(totalHutang)}, Piutang pelanggan: ${Formatters.rupiah(totalPiutang)})"
        }

        return """
            Nama Usaha: ${profile?.businessName ?: "Toko Suka Maju"} (${profile?.businessType ?: "Toko Kelontong"})
            Pemilik: ${profile?.ownerName ?: "Pemilik"}
            Total Penjualan: ${Formatters.rupiah(totalPenjualan)} | Pengeluaran & Belanja: ${Formatters.rupiah(totalBelanja)} | Selisih Kas: ${Formatters.rupiah(labaBersih)}
            Jumlah Stok: ${produkList.size} produk, $bahanCount bahan, $inventarisCount inventaris
            Produk & Margin: $topProductsText
            Stok Menipis: $lowStockText
            Utang/Piutang: $debtsSummary
        """.trimIndent()
    }
}
