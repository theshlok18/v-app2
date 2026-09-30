package com.samai.assistant.androidcontrol

import javax.inject.Inject
import javax.inject.Singleton

data class AppAdapter(
    val packageName: String,
    val displayName: String,
    val searchButtonId: String? = null,
    val searchFieldId: String? = null,
    val navigationPatterns: Map<String, String> = emptyMap()
)

@Singleton
class AppActionRegistry @Inject constructor() {
    private val adapters = mutableMapOf<String, AppAdapter>()

    init {
        registerDefaults()
    }

    private fun registerDefaults() {
        adapters["com.instagram.android"] = AppAdapter(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            searchButtonId = "com.instagram.android:id/search_text",
            navigationPatterns = mapOf("reels" to "com.instagram.android:id/reels_tab")
        )
        adapters["com.whatsapp"] = AppAdapter(
            packageName = "com.whatsapp",
            displayName = "WhatsApp",
            searchButtonId = "com.whatsapp:id/search",
            navigationPatterns = mapOf("chat" to "com.whatsapp:id/row_primary_conversation")
        )
        adapters["com.google.android.youtube"] = AppAdapter(
            packageName = "com.google.android.youtube",
            displayName = "YouTube",
            searchButtonId = "com.google.android.youtube:id/search_edit_frame",
            navigationPatterns = mapOf("home" to "com.google.android.youtube:id/home_page_root")
        )
        adapters["com.android.chrome"] = AppAdapter(
            packageName = "com.android.chrome",
            displayName = "Chrome",
            searchFieldId = "com.android.chrome:id/search_box_text"
        )
        adapters["com.google.android.apps.maps"] = AppAdapter(
            packageName = "com.google.android.apps.maps",
            displayName = "Google Maps",
            searchFieldId = "com.google.android.apps.maps:id/search_omnibox_text_view"
        )
        adapters["com.google.android.gm"] = AppAdapter(
            packageName = "com.google.android.gm",
            displayName = "Gmail",
            navigationPatterns = mapOf("compose" to "com.google.android.gm:id/compose_button")
        )
    }

    fun getAdapter(packageName: String): AppAdapter? = adapters[packageName]

    fun findByDisplayName(name: String): AppAdapter? {
        return adapters.values.find { it.displayName.equals(name, ignoreCase = true) }
    }

    fun registerAdapter(adapter: AppAdapter) {
        adapters[adapter.packageName] = adapter
    }

    fun getAllAdapters(): List<AppAdapter> = adapters.values.toList()
}
