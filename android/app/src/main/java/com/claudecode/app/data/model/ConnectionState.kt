package com.claudecode.app.data.model

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val localPort: Int) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
