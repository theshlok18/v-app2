package com.samai.assistant.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sam_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    suspend fun saveProviderConfig(providerId: String, apiKey: String, model: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("key_$providerId")] = apiKey
            prefs[stringPreferencesKey("model_$providerId")] = model
            prefs[stringPreferencesKey("enabled_$providerId")] = "true"
        }
    }

    suspend fun getProviderApiKey(providerId: String): String {
        return dataStore.data.first()[stringPreferencesKey("key_$providerId")] ?: ""
    }

    suspend fun getProviderModel(providerId: String): String {
        return dataStore.data.first()[stringPreferencesKey("model_$providerId")] ?: ""
    }

    suspend fun isProviderEnabled(providerId: String): Boolean {
        return dataStore.data.first()[stringPreferencesKey("enabled_$providerId")] == "true"
    }

    suspend fun setDefaultProvider(providerId: String) {
        dataStore.edit { it[stringPreferencesKey("default_provider")] = providerId }
    }

    suspend fun getDefaultProvider(): String {
        return dataStore.data.first()[stringPreferencesKey("default_provider")] ?: "openai"
    }

    suspend fun clearProviderConfig(providerId: String) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("key_$providerId"))
            prefs.remove(stringPreferencesKey("model_$providerId"))
            prefs.remove(stringPreferencesKey("enabled_$providerId"))
        }
    }
}
