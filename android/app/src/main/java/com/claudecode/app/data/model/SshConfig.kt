package com.claudecode.app.data.model

data class SshConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val connectionMode: ConnectionMode = ConnectionMode.SSH,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: AuthMethod = AuthMethod.Password,
    val password: String = "",
    val privateKeyPath: String = "",
    val privateKeyContent: String = "",
    val privateKeyPassphrase: String = "",
    val remotePort: Int = 8080,
    val localPort: Int = 8080,
    val serverCommand: String = "claude-code-server",
    val directApiUrl: String = ""
) {
    val needsStoragePermission: Boolean
        get() = connectionMode == ConnectionMode.SSH &&
                authMethod == AuthMethod.PrivateKey &&
                privateKeyContent.isBlank() &&
                privateKeyPath.startsWith("/storage/")
}

enum class ConnectionMode {
    SSH,
    DirectAPI
}

enum class AuthMethod {
    Password,
    PrivateKey
}
