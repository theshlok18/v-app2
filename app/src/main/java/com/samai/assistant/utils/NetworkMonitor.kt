package com.samai.assistant.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionState { ONLINE, OFFLINE, WEAK }

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _connectionState = MutableStateFlow(ConnectionState.ONLINE)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    fun checkConnection(): ConnectionState {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return ConnectionState.OFFLINE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionState.OFFLINE

        return when {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> ConnectionState.ONLINE
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> ConnectionState.WEAK
            else -> ConnectionState.OFFLINE
        }.also { _connectionState.value = it }
    }

    fun isOnline(): Boolean = _connectionState.value != ConnectionState.OFFLINE
}
