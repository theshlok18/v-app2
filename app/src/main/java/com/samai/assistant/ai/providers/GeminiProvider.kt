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
class GeminiProvider @Inject constructor(
    private val client: OkHttpClient
) : AIProvider {

    override val type = AIProviderType.Gemini
    override val isAvailable: Boolean get() = true

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    override suspend fun testConnection(apiKey: String, model: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/models?key=$apiKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Gemini connection successful.")
            } else {
                Result.failure(Exception("API Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generate(request: AIRequest): AIResponse {
        return try {
            val modelName = request.modelOverride ?: "gemini-pro"
            val url = "$baseUrl/models/$modelName:generateContent?key=${request.messages.firstOrNull()?.content?.let { "" } ?: ""}"

            val content = buildGeminiContent(request)
            val httpRequest = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), content))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                AIResponse(content = text, modelUsed = modelName)
            } else {
                AIResponse(content = "", modelUsed = modelName, isError = true, errorMessage = "Gemini API Error")
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
            val stepsArray = json.getJSONArray("steps")
            TaskPlanResult(
                steps = (0 until stepsArray.length()).map { i ->
                    val step = stepsArray.getJSONObject(i)
                    TaskStep(step.getInt("id"), step.getString("action"), step.getString("target"), description = step.getString("description"))
                },
                originalIntent = request.messages.last().content
            )
        } catch (_: Exception) {
            TaskPlanResult(steps = emptyList(), originalIntent = request.messages.last().content)
        }
    }

    private fun buildGeminiContent(request: AIRequest): String {
        val contents = JSONArray()
        request.messages.forEach { msg ->
            val role = if (msg.role == MessageRole.USER) "user" else "model"
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
            })
        }
        return JSONObject().apply {
            put("contents", contents)
            if (request.systemPrompt.isNotEmpty()) {
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", request.systemPrompt))))
            }
        }.toString()
    }
}
