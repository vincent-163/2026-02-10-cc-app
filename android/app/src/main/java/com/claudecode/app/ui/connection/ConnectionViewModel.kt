package com.claudecode.app.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudecode.app.data.SettingsRepository
import com.claudecode.app.data.model.ConnectionMode
import com.claudecode.app.data.model.ConnectionState
import com.claudecode.app.data.model.SshConfig
import com.claudecode.app.network.ApiClient
import com.claudecode.app.ssh.SshManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val sshManager: SshManager,
    private val apiClient: ApiClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = sshManager.connectionState

    private val _directConnected = MutableStateFlow(false)
    val directConnected: StateFlow<Boolean> = _directConnected.asStateFlow()

    private val _config = MutableStateFlow(SshConfig())
    val config: StateFlow<SshConfig> = _config.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settingsRepository.sshConfig.first()
            if (saved.host.isNotBlank() || saved.directApiUrl.isNotBlank()) {
                _config.value = saved
            }
            val token = settingsRepository.authToken.first()
            if (token.isNotBlank()) {
                apiClient.authToken = token
            }
        }
    }

    fun updateConfig(config: SshConfig) {
        _config.value = config
    }

    fun connect() {
        val cfg = _config.value
        if (cfg.connectionMode == ConnectionMode.DirectAPI) {
            connectDirect()
        } else {
            connectSsh()
        }
    }

    private fun connectDirect() {
        viewModelScope.launch {
            _statusMessage.value = "Connecting to API server..."
            settingsRepository.saveSshConfig(_config.value)

            val url = _config.value.directApiUrl.trimEnd('/')
            apiClient.updateBaseUrlDirect(url)

            val healthResult = apiClient.healthCheck()
            healthResult.fold(
                onSuccess = { health ->
                    _directConnected.value = true
                    _statusMessage.value = "Connected (${health.sessionsActive} active sessions)"
                },
                onFailure = { e ->
                    _directConnected.value = false
                    _statusMessage.value = "Connection failed: ${e.message}"
                }
            )
        }
    }

    private fun connectSsh() {
        viewModelScope.launch {
            _statusMessage.value = "Connecting via SSH..."
            settingsRepository.saveSshConfig(_config.value)

            val result = sshManager.connect(_config.value)
            result.fold(
                onSuccess = { localPort ->
                    _statusMessage.value = "SSH connected. Starting server..."
                    apiClient.updateBaseUrl(localPort)

                    val serverResult = sshManager.startRemoteServer(_config.value)
                    serverResult.fold(
                        onSuccess = { status ->
                            _statusMessage.value = "Checking server health..."
                            kotlinx.coroutines.delay(1000)
                            val healthResult = apiClient.healthCheck()
                            healthResult.fold(
                                onSuccess = { health ->
                                    _statusMessage.value = "Connected (${health.sessionsActive} active sessions)"
                                },
                                onFailure = {
                                    _statusMessage.value = when {
                                        status.contains("ALREADY_RUNNING") -> "Connected (server was already running)"
                                        status.contains("STARTED") -> "Connected (server started)"
                                        else -> "Connected"
                                    }
                                }
                            )
                        },
                        onFailure = { e ->
                            _statusMessage.value = "Connected (server start uncertain: ${e.message})"
                        }
                    )
                },
                onFailure = { e ->
                    _statusMessage.value = "Connection failed: ${e.message}"
                }
            )
        }
    }

    fun disconnect() {
        if (_config.value.connectionMode == ConnectionMode.DirectAPI) {
            _directConnected.value = false
        } else {
            sshManager.disconnect()
        }
        _statusMessage.value = ""
    }

    val isConnected: Boolean
        get() = if (_config.value.connectionMode == ConnectionMode.DirectAPI) {
            _directConnected.value
        } else {
            connectionState.value is ConnectionState.Connected
        }

    override fun onCleared() {
        super.onCleared()
        sshManager.disconnect()
    }
}
