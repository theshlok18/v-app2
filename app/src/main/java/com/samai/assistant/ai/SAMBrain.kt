package com.samai.assistant.ai

import com.samai.assistant.knowledge.KnowledgeManager
import com.samai.assistant.memory.MemoryManager
import com.samai.assistant.profile.DeveloperProfileManager
import com.samai.assistant.profile.UserProfileManager
import javax.inject.Inject
import javax.inject.Singleton

enum class IntentType { TASK, KNOWLEDGE, MEMORY, CODING, DEVELOPER_INFO, CHAT }

data class SAMResponse(val text: String, val intent: IntentType)

@Singleton
class SAMBrain @Inject constructor(
    private val modelRouter: ModelRouter,
    private val developerProfile: DeveloperProfileManager,
    private val knowledgeManager: KnowledgeManager,
    private val memoryManager: MemoryManager,
    private val profileManager: UserProfileManager,
    private val promptManager: PromptManager
) {
    suspend fun process(input: String): SAMResponse {
        // 1. Check developer info queries
        if (developerProfile.isDeveloperQuery(input)) {
            return SAMResponse(developerProfile.getDeveloperResponse(), IntentType.DEVELOPER_INFO)
        }

        // 2. Check knowledge queries (who is, what is, tell me about)
        if (isKnowledgeQuery(input)) {
            val topic = extractTopic(input)
            val result = knowledgeManager.queryWikipedia(topic)
            if (!result.isError) {
                return SAMResponse(result.summary, IntentType.KNOWLEDGE)
            }
        }

        // 3. Check memory commands
        if (isMemoryCommand(input)) {
            // Handle memory store/retrieve
            return SAMResponse("Memory noted.", IntentType.MEMORY)
        }

        // 4. Default: send to AI provider
        return try {
            val response = modelRouter.processCommand(input)
            SAMResponse(response, IntentType.CHAT)
        } catch (e: Exception) {
            val userName = profileManager.getUserName()
            SAMResponse("Sorry $userName, I couldn't process that request.", IntentType.CHAT)
        }
    }

    private fun isKnowledgeQuery(query: String): Boolean {
        val lower = query.lowercase()
        return lower.startsWith("who is") || lower.startsWith("what is") ||
                lower.startsWith("tell me about") || lower.startsWith("define") ||
                lower.contains("wikipedia")
    }

    private fun isMemoryCommand(query: String): Boolean {
        val lower = query.lowercase()
        return lower.startsWith("remember") || lower.startsWith("note that") ||
                lower.startsWith("save this") || lower.startsWith("don't forget")
    }

    private fun extractTopic(query: String): String {
        return query.lowercase()
            .replace("who is", "").replace("what is", "")
            .replace("tell me about", "").replace("define", "")
            .replace("wikipedia", "").trim()
    }
}
package com.samai.assistant.ai

import com.samai.assistant.knowledge.KnowledgeManager
import com.samai.assistant.memory.MemoryManager
import com.samai.assistant.profile.DeveloperProfileManager
import com.samai.assistant.profile.UserProfileManager
import com.samai.assistant.task.TaskPlanner
import com.samai.assistant.utils.SafetyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class IntentType { TASK, QUESTION, CHAT, MEMORY, KNOWLEDGE, DEVELOPER_INFO, CODING }

data class SAMResponse(
    val text: String,
    val intentType: IntentType,
    val taskResult: com.samai.assistant.task.TaskResult? = null,
    val requiresConfirmation: Boolean = false,
    val isSpoken: Boolean = false
)

@Singleton
class SAMBrain @Inject constructor(
    private val modelRouter: ModelRouter,
    private val promptManager: PromptManager,
    private val taskPlanner: TaskPlanner,
    private val memoryManager: MemoryManager,
    private val knowledgeManager: KnowledgeManager,
    private val userProfileManager: UserProfileManager,
    private val developerProfileManager: DeveloperProfileManager,
    private val safetyManager: SafetyManager
) {
    private val _lastResponse = MutableStateFlow<SAMResponse?>(null)
    val lastResponse: StateFlow<SAMResponse?> = _lastResponse

    private val chatHistory = mutableListOf<ChatMessage>()

    suspend fun process(userInput: String): SAMResponse {
        val userName = userProfileManager.getUserName()
        val assistantName = userProfileManager.getAssistantName()
        val responseStyle = userProfileManager.responseStyle.toString()

        // Check developer query first
        if (developerProfileManager.isDeveloperQuery(userInput)) {
            val response = developerProfileManager.getDeveloperResponse()
            return SAMResponse(text = response, intentType = IntentType.DEVELOPER_INFO).also {
                _lastResponse.value = it
                chatHistory.add(ChatMessage(MessageRole.USER, userInput))
                chatHistory.add(ChatMessage(MessageRole.ASSISTANT, response))
            }
        }

        // Determine intent
        val intent = classifyIntent(userInput)

        return when (intent) {
            IntentType.TASK -> handleTask(userInput, userName, assistantName)
            IntentType.KNOWLEDGE -> handleKnowledge(userInput, assistantName, userName)
            IntentType.MEMORY -> handleMemory(userInput)
            IntentType.CODING -> handleCoding(userInput, assistantName, userName)
            else -> handleChat(userInput, userName, assistantName, responseStyle)
        }
    }

    private suspend fun handleTask(userInput: String, userName: String, assistantName: String): SAMResponse {
        val result = taskPlanner.planAndExecute(userInput)
        return SAMResponse(
            text = result.message,
            intentType = IntentType.TASK,
            taskResult = result
        ).also { _lastResponse.value = it }
    }

    private suspend fun handleKnowledge(userInput: String, assistantName: String, userName: String): SAMResponse {
        val topic = extractTopic(userInput)
        val result = knowledgeManager.queryWikipedia(topic)
        val text = if (!result.isError) {
            "Here's what I found, $userName: ${result.summary}"
        } else {
            "Sorry $userName, I couldn't retrieve information on that topic."
        }
        return SAMResponse(text = text, intentType = IntentType.KNOWLEDGE).also { _lastResponse.value = it }
    }

    private suspend fun handleMemory(userInput: String): SAMResponse {
        return SAMResponse(text = "Memory operation requested.", intentType = IntentType.MEMORY).also { _lastResponse.value = it }
    }

    private suspend fun handleCoding(userInput: String, assistantName: String, userName: String): SAMResponse {
        val systemPrompt = promptManager.buildSystemPrompt(assistantName, userName, "technical")
        val response = modelRouter.processCommand(userInput, systemPrompt, chatHistory)
        return SAMResponse(text = response.content, intentType = IntentType.CODING).also { _lastResponse.value = it }
    }

    private suspend fun handleChat(userInput: String, userName: String, assistantName: String, style: String): SAMResponse {
        val systemPrompt = promptManager.buildSystemPrompt(assistantName, userName, style)
        val response = modelRouter.processCommand(userInput, systemPrompt, chatHistory)

        chatHistory.add(ChatMessage(MessageRole.USER, userInput))
        chatHistory.add(ChatMessage(MessageRole.ASSISTANT, response.content))

        if (chatHistory.size > 20) chatHistory.take(20)

        return SAMResponse(text = response.content, intentType = IntentType.CHAT).also { _lastResponse.value = it }
    }

    private fun classifyIntent(input: String): IntentType {
        val lower = input.lowercase()
        return when {
            lower.contains("open ") || lower.contains("launch ") || lower.contains("go to ") ||
            lower.contains("tap ") || lower.contains("scroll") || lower.contains("type ") ||
            lower.contains("search for ") && !lower.contains("wikipedia") -> IntentType.TASK
            lower.contains("wikipedia") || lower.contains("what is ") || lower.contains("explain ") ||
            lower.contains("who is ") || lower.contains("tell me about ") -> IntentType.KNOWLEDGE
            lower.contains("remember ") || lower.contains("save this ") || lower.contains("note ") -> IntentType.MEMORY
            lower.contains("code ") || lower.contains("create a ") && (lower.contains("python") || lower.contains("html")) ||
            lower.contains("fix this code") || lower.contains("debug") -> IntentType.CODING
            else -> IntentType.CHAT
        }
    }

    private fun extractTopic(input: String): String {
        return input.replace(Regex("(?i)(what is|explain|search wikipedia for|tell me about|who is)\\s*"), "").trim()
    }
}
