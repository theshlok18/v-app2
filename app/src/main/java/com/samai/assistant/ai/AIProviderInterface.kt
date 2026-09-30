package com.samai.assistant.ai

import kotlinx.coroutines.flow.Flow

sealed class AIProviderType(val id: String, val displayName: String) {
    object OpenAI : AIProviderType("openai", "OpenAI")
    object Gemini : AIProviderType("gemini", "Google Gemini")
    object Anthropic : AIProviderType("anthropic", "Anthropic Claude")
    object Local : AIProviderType("local", "Local Model")
    data class Custom(val name: String) : AIProviderType("custom", name)
}

data class AIModelConfig(
    val providerType: AIProviderType,
    val modelName: String,
    val apiKey: String = "",
    val baseUrl: String = "",
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false
)

data class AIRequest(
    val messages: List<ChatMessage>,
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val modelOverride: String? = null
)

data class AIResponse(
    val content: String,
    val modelUsed: String,
    val tokensUsed: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

data class ChatMessage(
    val role: MessageRole,
    val content: String
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }

interface AIProvider {
    val type: AIProviderType
    val isAvailable: Boolean
    suspend fun testConnection(apiKey: String, model: String): Result<String>
    suspend fun generate(request: AIRequest): AIResponse
    fun generateStream(request: AIRequest): Flow<String>
    suspend fun generateTaskPlan(request: AIRequest): TaskPlanResult
}

data class TaskPlanResult(
    val steps: List<TaskStep>,
    val originalIntent: String,
    val requiresConfirmation: Boolean = false
)

data class TaskStep(
    val id: Int,
    val action: String,
    val target: String,
    val parameters: Map<String, String> = emptyMap(),
    val description: String
)
