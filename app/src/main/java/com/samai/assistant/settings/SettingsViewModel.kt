package com.samai.assistant.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samai.assistant.ai.ModelRouter
import com.samai.assistant.profile.UserProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIProviderUiState(
    val providerName: String,
    val providerId: String,
    val isConfigured: Boolean,
    val isEnabled: Boolean,
    val isDefault: Boolean,
    val models: List<String> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userProfile: UserProfileManager,
    private val modelRouter: ModelRouter,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val userName: StateFlow<String> = userProfile.userName.stateIn(viewModelScope, SharingStarted.Lazily, "User")
    val wakeWord: StateFlow<String> = userProfile.wakeWord.stateIn(viewModelScope, SharingStarted.Lazily, "Hey Sam")

    private val _providers = MutableStateFlow<List<AIProviderUiState>>(emptyList())
    val providers: StateFlow<List<AIProviderUiState>> = _providers.asStateFlow()

    fun updateUserName(name: String) {
        viewModelScope.launch { userProfile.setUserName(name) }
    }

    fun updateWakeWord(word: String) {
        viewModelScope.launch { userProfile.setWakeWord(word) }
    }

    fun updateResponseStyle(style: String) {
        viewModelScope.launch { userProfile.setResponseStyle(style) }
    }

    fun testProviderConnection(providerId: String, apiKey: String, model: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = modelRouter.testProvider(providerId, apiKey, model)
            onResult(result)
        }
    }

    fun saveProviderConfig(providerId: String, apiKey: String, model: String) {
        viewModelScope.launch {
            settingsRepo.saveProviderConfig(providerId, apiKey, model)
        }
    }
}
