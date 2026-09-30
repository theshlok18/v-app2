package com.samai.assistant.ai.providers

import com.samai.assistant.ai.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnthropicProvider @Inject constructor(
    private val client: OkHttpClient
) : AIProvider {

    override val type = AIProviderType.Anthropic
    override val isAvailable: Boolean get() = true

    private val baseUrl = "https://api.anthropic.com/v1"

    override suspend fun testConnection(apiKey: String, model: String): Result<String> {
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "hi")))
            }
            val request = Request.Builder()
                .url("$baseUrl/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), body.toString()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success("Anthropic connection successful.")
            else Result.failure(Exception("API Error: ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        return try {
            val body = buildAnthropicBody(request)
            val httpRequest = Request.Builder()
                .url("$baseUrl/messages")
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), body))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val content = json.getJSONArray("content").getJSONObject(0).getString("text")
                AIResponse(content = content, modelUsed = request.modelOverride ?: "claude-sonnet-4-20250514")
            } else {
                AIResponse(content = "", modelUsed = "", isError = true, errorMessage = "Anthropic API Error")
            }
        } catch (e: Exception) {
            AIResponse(content = "", modelUsed = "", isError = true, errorMessage = e.message)
        }
    }

    override fun generateStream(request: AIRequest): Flow<String> = flow {
        val response = generate(request)
        if (!response.isError) emit(response.content)
    }

    override suspend fun generateTaskPlan(request: AIRequest): TaskPlanResult {
        val response = generate(request)
        return try {
            val json = JSONObject(response.content)
            val steps = json.getJSONArray("steps").let { arr ->
                (0 until arr.length()).map { i ->
                    TaskStep(i + 1, arr.getJSONObject(i).getString("action"), arr.getJSONObject(i).getString("target"), description = arr.getJSONObject(i).getString("description"))
                }
            }
            TaskPlanResult(steps, request.messages.last().content)
        } catch (_: Exception) {
            TaskPlanResult(emptyList(), request.messages.last().content)
        }
    }

    private fun buildAnthropicBody(request: AIRequest): String {
        val messages = JSONArray()
        request.messages.forEach { msg ->
            if (msg.role != MessageRole.SYSTEM) {
                messages.put(JSONObject().apply {
                    put("role", msg.role.name.lowercase())
                    put("content", msg.content)
                })
            }
        }
        return JSONObject().apply {
            put("model", request.modelOverride ?: "claude-sonnet-4-20250514")
            put("max_tokens", request.maxTokens)
            put("messages", messages)
            if (request.systemPrompt.isNotEmpty()) put("system", request.systemPrompt)
        }.toString()
    }
}
