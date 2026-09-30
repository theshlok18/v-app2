package com.samai.assistant.knowledge

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class KnowledgeResult(
    val title: String,
    val summary: String,
    val content: String,
    val source: String,
    val url: String = "",
    val isError: Boolean = false
)

interface KnowledgeProvider {
    val name: String
    suspend fun search(query: String): KnowledgeResult
    suspend fun getSummary(topic: String): KnowledgeResult
}

@Singleton
class WikipediaProvider @Inject constructor(
    private val client: OkHttpClient
) : KnowledgeProvider {

    override val name = "Wikipedia"

    private val apiBase = "https://en.wikipedia.org/api/rest_v1"

    override suspend fun search(query: String): KnowledgeResult {
        return try {
            val url = "$apiBase/page/summary/${query.replace(" ", "_")}"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                KnowledgeResult(
                    title = json.optString("title", query),
                    summary = json.optString("extract", "No summary available."),
                    content = json.optString("extract", ""),
                    source = "Wikipedia",
                    url = json.getJSONObject("content_urls").getJSONObject("desktop").getString("page")
                )
            } else {
                KnowledgeResult(title = query, summary = "", content = "", source = "Wikipedia", isError = true)
            }
        } catch (e: Exception) {
            KnowledgeResult(title = query, summary = "Failed to fetch: ${e.message}", content = "", source = "Wikipedia", isError = true)
        }
    }

    override suspend fun getSummary(topic: String): KnowledgeResult = search(topic)

    suspend fun searchMultiple(query: String): List<KnowledgeResult> {
        return try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&srlimit=5"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val results = json.getJSONObject("query").getJSONArray("search")
                (0 until results.length()).map { i ->
                    val item = results.getJSONObject(i)
                    KnowledgeResult(
                        title = item.getString("title"),
                        summary = item.getString("snippet").replace(Regex("<[^>]*>"), ""),
                        content = "",
                        source = "Wikipedia",
                        url = "https://en.wikipedia.org/wiki/${item.getString("title").replace(" ", "_")}"
                    )
                }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Singleton
class KnowledgeManager @Inject constructor(
    private val wikipedia: WikipediaProvider
) {
    suspend fun queryWikipedia(topic: String): KnowledgeResult {
        return wikipedia.search(topic)
    }

    suspend fun searchWikipedia(query: String): List<KnowledgeResult> {
        return wikipedia.searchMultiple(query)
    }
}
