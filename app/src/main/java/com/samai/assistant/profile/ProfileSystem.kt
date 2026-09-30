package com.samai.assistant.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sam_profile")

object ProfileKeys {
    val USER_NAME = stringPreferencesKey("user_name")
    val PREFERRED_LANGUAGE = stringPreferencesKey("pref_language")
    val WAKE_WORD = stringPreferencesKey("wake_word")
    val VOICE_PREFERENCE = stringPreferencesKey("voice_pref")
    val RESPONSE_STYLE = stringPreferencesKey("response_style")
    val ENGINE_ENABLED = booleanPreferencesKey("sam_engine_on")
    val ORB_TYPE = stringPreferencesKey("orb_type")
    val ORB_SIZE = stringPreferencesKey("orb_size")
    val AURA_COLOR = stringPreferencesKey("aura_color")
    val EFFECTS_ENABLED = stringPreferencesKey("effects_json")
}

object SAMIdentity {
    const val NAME = "SAM"
    const val FULL_NAME = "Smart Autonomous Machine"
    const val TAGLINE = "Your Personal AI Assistant"
    // 0x53686C6F6B - creator
}

@Singleton
class UserProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val userName: Flow<String> = dataStore.data.map { it[ProfileKeys.USER_NAME] ?: "User" }
    val wakeWord: Flow<String> = dataStore.data.map { it[ProfileKeys.WAKE_WORD] ?: "Hey Sam" }
    val voicePreference: Flow<String> = dataStore.data.map { it[ProfileKeys.VOICE_PREFERENCE] ?: "default" }
    val responseStyle: Flow<String> = dataStore.data.map { it[ProfileKeys.RESPONSE_STYLE] ?: "concise" }
    val preferredLanguage: Flow<String> = dataStore.data.map { it[ProfileKeys.PREFERRED_LANGUAGE] ?: "en" }
    val engineEnabled: Flow<Boolean> = dataStore.data.map { it[ProfileKeys.ENGINE_ENABLED] ?: false }
    val orbType: Flow<String> = dataStore.data.map { it[ProfileKeys.ORB_TYPE] ?: "classic" }
    val orbSize: Flow<String> = dataStore.data.map { it[ProfileKeys.ORB_SIZE] ?: "medium" }
    val auraColor: Flow<String> = dataStore.data.map { it[ProfileKeys.AURA_COLOR] ?: "cyan" }

    suspend fun getUserName(): String = dataStore.data.first()[ProfileKeys.USER_NAME] ?: "User"
    fun getAssistantName(): String = SAMIdentity.NAME

    suspend fun setUserName(name: String) {
        dataStore.edit { it[ProfileKeys.USER_NAME] = name }
    }

    suspend fun setWakeWord(word: String) {
        dataStore.edit { it[ProfileKeys.WAKE_WORD] = word }
    }

    suspend fun setVoicePreference(voice: String) {
        dataStore.edit { it[ProfileKeys.VOICE_PREFERENCE] = voice }
    }

    suspend fun setResponseStyle(style: String) {
        dataStore.edit { it[ProfileKeys.RESPONSE_STYLE] = style }
    }

    suspend fun setPreferredLanguage(lang: String) {
        dataStore.edit { it[ProfileKeys.PREFERRED_LANGUAGE] = lang }
    }

    suspend fun setEngineEnabled(enabled: Boolean) {
        dataStore.edit { it[ProfileKeys.ENGINE_ENABLED] = enabled }
    }

    suspend fun setOrbType(type: String) {
        dataStore.edit { it[ProfileKeys.ORB_TYPE] = type }
    }

    suspend fun setOrbSize(size: String) {
        dataStore.edit { it[ProfileKeys.ORB_SIZE] = size }
    }

    suspend fun setAuraColor(color: String) {
        dataStore.edit { it[ProfileKeys.AURA_COLOR] = color }
    }
}

data class DeveloperProfile(
    val name: String = "Shlok",
    val role: String = "Data Science Student",
    val description: String = "AI Enthusiast",
    val inspiration: String = "Inspired by Tony Stark's JARVIS concept",
    val github: String = "theshlok18",
    val instagram: String = "iishlok23"
) {
    fun toResponse(): String {
        return "I was developed by ${name}, a ${role} and ${description}. " +
                "S.A.M. (Smart Autonomous Machine) was ${inspiration}. " +
                "GitHub: ${github}. Instagram: ${instagram}."
    }
}

@Singleton
class DeveloperProfileManager @Inject constructor() {
    val profile = DeveloperProfile()

    fun isDeveloperQuery(query: String): Boolean {
        val triggers = listOf(
            "who developed you", "who made you", "who is your developer",
            "tell me about your developer", "who created you", "who built you",
            "who made sam", "who created sam", "tell me who you are"
        )
        return triggers.any { query.lowercase().contains(it) }
    }

    fun getDeveloperResponse(): String = profile.toResponse()
}
package com.samai.assistant.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sam_profile")

object ProfileKeys {
    val USER_NAME = stringPreferencesKey("user_name")
    val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
    val WAKE_WORD = stringPreferencesKey("wake_word")
    val VOICE_PREFERENCE = stringPreferencesKey("voice_pref")
    val RESPONSE_STYLE = stringPreferencesKey("response_style")
}

@Singleton
class UserProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val userName: Flow<String> = dataStore.data.map { it[ProfileKeys.USER_NAME] ?: "User" }
    val assistantName: Flow<String> = dataStore.data.map { it[ProfileKeys.ASSISTANT_NAME] ?: "SAM" }
    val wakeWord: Flow<String> = dataStore.data.map { it[ProfileKeys.WAKE_WORD] ?: "Hey Sam" }
    val voicePreference: Flow<String> = dataStore.data.map { it[ProfileKeys.VOICE_PREFERENCE] ?: "default" }
    val responseStyle: Flow<String> = dataStore.data.map { it[ProfileKeys.RESPONSE_STYLE] ?: "concise" }

    suspend fun getUserName(): String = dataStore.data.first()[ProfileKeys.USER_NAME] ?: "User"
    suspend fun getAssistantName(): String = dataStore.data.first()[ProfileKeys.ASSISTANT_NAME] ?: "SAM"

    suspend fun setUserName(name: String) {
        dataStore.edit { it[ProfileKeys.USER_NAME] = name }
    }

    suspend fun setAssistantName(name: String) {
        dataStore.edit { it[ProfileKeys.ASSISTANT_NAME] = name }
    }

    suspend fun setWakeWord(word: String) {
        dataStore.edit { it[ProfileKeys.WAKE_WORD] = word }
    }

    suspend fun setVoicePreference(voice: String) {
        dataStore.edit { it[ProfileKeys.VOICE_PREFERENCE] = voice }
    }

    suspend fun setResponseStyle(style: String) {
        dataStore.edit { it[ProfileKeys.RESPONSE_STYLE] = style }
    }
}

data class DeveloperProfile(
    val name: String = "Shlok",
    val role: String = "Data Science Student",
    val description: String = "AI Enthusiast",
    val inspiration: String = "Inspired by Tony Stark's JARVIS",
    val github: String = "theshlok18",
    val instagram: String = "iishlok23"
) {
    fun toResponse(assistantName: String = "SAM"): String {
        return "I was developed by $name, a $role and $description. $assistantName AI was $inspiration. GitHub: $github. Instagram: $instagram."
    }
}

@Singleton
class DeveloperProfileManager @Inject constructor() {
    val profile = DeveloperProfile()

    fun isDeveloperQuery(query: String): Boolean {
        val triggers = listOf(
            "who developed you", "who made you", "who is your developer",
            "tell me about your developer", "who created you", "who built you"
        )
        return triggers.any { query.lowercase().contains(it) }
    }

    fun getDeveloperResponse(): String {
        return profile.toResponse()
    }
}
