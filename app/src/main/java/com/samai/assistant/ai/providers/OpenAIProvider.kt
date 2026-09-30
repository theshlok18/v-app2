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
class OpenAIProvider @Inject constructor(
    private val client: OkHttpClient
) : AIProvider {

    override val type = AIProviderType.OpenAI
    override val isAvailable: Boolean get() = true

    private val baseUrl = "https://api.openai.com/v1"

    override suspend fun testConnection(apiKey: String, model: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Connection successful. Models available.")
            } else {
                Result.failure(Exception("API Error: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        return try {
            val jsonBody = buildRequestBody(request)
            val httpRequest = Request.Builder()
                .url("$baseUrl/chat/completions")
                .header("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), jsonBody))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                val tokens = json.getJSONObject("usage").getInt("total_tokens")
                AIResponse(content = content, modelUsed = request.modelOverride ?: "gpt-4o", tokensUsed = tokens)
            } else {
                AIResponse(content = "", modelUsed = "", isError = true, errorMessage = "API Error $response")
            }
        } catch (e: Exception) {
            AIResponse(content = "", modelUsed = "", isError = true, errorMessage = e.message)
        }
    }

    override fun generateStream(request: AIRequest): Flow<String> = flow {
        val jsonBody = buildRequestBody(request, streaming = true)
        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaType(), jsonBody))
            .build()

        val response = client.newCall(httpRequest).execute()
        val source = okio.Okio.buffer(response.body?.source() ?: return@flow)

        while (true) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                try {
                    val json = JSONObject(data)
                    val delta = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("delta").optString("content", "")
                    if (delta.isNotEmpty()) emit(delta)
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun generateTaskPlan(request: AIRequest): TaskPlanResult {
        val response = generate(request)
        return try {
            val json = JSONObject(response.content)
            val stepsArray = json.getJSONArray("steps")
            val steps = (0 until stepsArray.length()).map { i ->
                val step = stepsArray.getJSONObject(i)
                TaskStep(
                    id = step.getInt("id"),
                    action = step.getString("action"),
                    target = step.getString("target"),
                    description = step.getString("description")
                )
            }
            TaskPlanResult(steps = steps, originalIntent = request.messages.last().content)
        } catch (_: Exception) {
            TaskPlanResult(steps = emptyList(), originalIntent = request.messages.last().content)
        }
    }

    private fun buildRequestBody(request: AIRequest, streaming: Boolean = false): String {
        val messages = JSONArray()
        if (request.systemPrompt.isNotEmpty()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", request.systemPrompt)
            })
        }
        request.messages.forEach { msg ->
            messages.put(JSONObject().apply {
                put("role", msg.role.name.lowercase())
                put("content", msg.content)
            })
        }
        return JSONObject().apply {
            put("model", request.modelOverride ?: "gpt-4o")
            put("messages", messages)
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)
            if (streaming) put("stream", true)
        }.toString()
    }
}
