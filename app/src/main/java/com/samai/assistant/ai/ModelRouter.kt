package com.samai.assistant.ai

import com.samai.assistant.ai.providers.AnthropicProvider
import com.samai.assistant.ai.providers.GeminiProvider
import com.samai.assistant.ai.providers.OpenAIProvider
import com.samai.assistant.profile.SAMIdentity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRouter @Inject constructor(
    private val openAI: OpenAIProvider,
    private val gemini: GeminiProvider,
    private val anthropic: AnthropicProvider,
    private val promptManager: PromptManager
) {
    private val providers: Map<String, AIProvider> = mapOf(
        "openai" to openAI,
        "gemini" to gemini,
        "anthropic" to anthropic
    )

    private var defaultProviderId: String = "openai"
    private var apiKeyStore: MutableMap<String, String> = mutableMapOf()
    private val chatHistory = mutableListOf<ChatMessage>()

    fun setDefaultProvider(providerId: String) {
        defaultProviderId = providerId
    }

    fun getProvider(providerId: String = defaultProviderId): AIProvider? {
        return providers[providerId]
    }

    fun configureApiKey(providerId: String, key: String) {
        apiKeyStore[providerId] = key
    }

    fun getAvailableProviders(): List<AIProviderType> {
        return providers.keys.map { id ->
            when (id) {
                "openai" -> AIProviderType.OpenAI
                "gemini" -> AIProviderType.Gemini
                "anthropic" -> AIProviderType.Anthropic
                else -> AIProviderType.Custom(id)
            }
        }
    }

    /**
     * Simple text-to-text command processing for Chat.
     * Returns SAM's response as a String.
     */
    suspend fun processCommand(userInput: String): String {
        val provider = getProvider() ?: return "No AI provider configured. Please set up in Settings."

        val systemPrompt = promptManager.buildChatPrompt(SAMIdentity.NAME)
        val messages = chatHistory.toMutableList().apply {
            add(ChatMessage(MessageRole.USER, userInput))
        }

        val request = AIRequest(messages = messages, systemPrompt = systemPrompt)
        val response = provider.generate(request)

        // Maintain history
        chatHistory.add(ChatMessage(MessageRole.USER, userInput))
        chatHistory.add(ChatMessage(MessageRole.ASSISTANT, response.content))
        if (chatHistory.size > 20) chatHistory.removeAt(0)

        return response.content
    }

    /**
     * Plan a multi-step task via AI.
     */
    suspend fun planTask(userInput: String): TaskPlanResult {
        val provider = getProvider() ?: return TaskPlanResult(emptyList(), userInput)
        val systemPrompt = promptManager.buildTaskPlannerPrompt()
        val messages = listOf(ChatMessage(MessageRole.USER, userInput))
        val request = AIRequest(messages = messages, systemPrompt = systemPrompt)
        return provider.generateTaskPlan(request)
    }

    suspend fun processCommandWithHistory(userInput: String, systemPrompt: String, history: List<ChatMessage>): AIResponse {
        val provider = getProvider() ?: return AIResponse(
            content = "No AI provider configured.",
            modelUsed = "none", isError = true
        )
        val messages = history.toMutableList().apply {
            add(ChatMessage(MessageRole.USER, userInput))
        }
        val request = AIRequest(messages = messages, systemPrompt = systemPrompt)
        return provider.generate(request)
    }

    fun processCommandStream(userInput: String, systemPrompt: String, history: List<ChatMessage>): Flow<String> {
        val provider = getProvider() ?: return kotlinx.coroutines.flow.flow { emit("No provider configured.") }
        val messages = history.toMutableList().apply {
            add(ChatMessage(MessageRole.USER, userInput))
        }
        val request = AIRequest(messages = messages, systemPrompt = systemPrompt)
        return provider.generateStream(request)
    }

    suspend fun testProvider(providerId: String, apiKey: String, model: String): Result<String> {
        val provider = getProvider(providerId) ?: return Result.failure(Exception("Provider not found"))
        return provider.testConnection(apiKey, model)
    }
}
package com.samai.assistant.ai

import com.samai.assistant.ai.providers.AnthropicProvider
import com.samai.assistant.ai.providers.GeminiProvider
import com.samai.assistant.ai.providers.OpenAIProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRouter @Inject constructor(
    private val openAI: OpenAIProvider,
    private val gemini: GeminiProvider,
    private val anthropic: AnthropicProvider
) {
    private val providers: Map<String, AIProvider> = mapOf(
        "openai" to openAI,
        "gemini" to gemini,
        "anthropic" to anthropic
    )

    private var defaultProviderId: String = "openai"
    private var apiKeyStore: MutableMap<String, String> = mutableMapOf()

    fun setDefaultProvider(providerId: String) {
        defaultProviderId = providerId
    }

    fun getProvider(providerId: String = defaultProviderId): AIProvider? {
        return providers[providerId]
    }

    fun configureApiKey(providerId: String, key: String) {
        apiKeyStore[providerId] = key
    }

    fun getAvailableProviders(): List<AIProviderType> {
        return providers.keys.map { id ->
            when (id) {
                "openai" -> AIProviderType.OpenAI
                "gemini" -> AIProviderType.Gemini
                "anthropic" -> AIProviderType.Anthropic
                else -> AIProviderType.Custom(id)
            }
        }
    }

    suspend fun processCommand(userInput: String, systemPrompt: String, history: List<ChatMessage>): AIResponse {
        val provider = getProvider() ?: return AIResponse(
            content = "No AI provider configured. Please set up in Settings.",
            modelUsed = "none", isError = true
        )

        val messages = history.toMutableList().apply {
            add(ChatMessage(MessageRole.USER, userInput))
        }

        val request = AIRequest(messages = messages, systemPrompt = systemPrompt)
        return provider.generate(request)
    }

    suspend fun planTask(userInput: String, systemPrompt: String): TaskPlanResult {
        val provider = getProvider() ?: return TaskPlanResult(emptyList(), userInput)
        val messages = listOf(ChatMessage(MessageRole.USER, userInput))
        val request = AIRequest(messages = messages, systemPrompt = systemPrompt)
        return provider.generateTaskPlan(request)
    }

    fun processCommandStream(userInput: String, systemPrompt: String, history: List<ChatMessage>): Flow<String> {
        val provider = getProvider() ?: return kotlinx.coroutines.flow.flow { emit("No provider configured.") }
        val messages = history.toMutableList().apply {
            add(ChatMessage(MessageRole.USER, userInput))
        }
        val request = AIRequest(messages = messages, systemPrompt = systemPrompt)
        return provider.generateStream(request)
    }

    suspend fun testProvider(providerId: String, apiKey: String, model: String): Result<String> {
        val provider = getProvider(providerId) ?: return Result.failure(Exception("Provider not found"))
        return provider.testConnection(apiKey, model)
    }
}
