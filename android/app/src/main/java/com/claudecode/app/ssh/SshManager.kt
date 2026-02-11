package com.claudecode.app.ssh

import com.claudecode.app.data.model.AuthMethod
import com.claudecode.app.data.model.ConnectionState
import com.claudecode.app.data.model.SshConfig
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties

class SshManager {

    private var session: Session? = null
    private var tempKeyFile: File? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    suspend fun connect(config: SshConfig): Result<Int> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting
            disconnect()

            val jsch = JSch()

            if (config.authMethod == AuthMethod.PrivateKey) {
                val keyPath = if (config.privateKeyContent.isNotBlank()) {
                    // Write pasted key to temp file for JSch
                    val tmp = File.createTempFile("ssh_key_", null)
                    tmp.writeText(config.privateKeyContent)
                    tmp.setReadable(false, false)
                    tmp.setReadable(true, true)
                    tempKeyFile = tmp
                    tmp.absolutePath
                } else {
                    config.privateKeyPath
                }

                if (keyPath.isNotBlank()) {
                    if (config.privateKeyPassphrase.isNotBlank()) {
                        jsch.addIdentity(keyPath, config.privateKeyPassphrase)
                    } else {
                        jsch.addIdentity(keyPath)
                    }
                }
            }

            val sshSession = jsch.getSession(config.username, config.host, config.port)

            if (config.authMethod == AuthMethod.Password) {
                sshSession.setPassword(config.password)
            }

            val props = Properties()
            props["StrictHostKeyChecking"] = "no"
            sshSession.setConfig(props)
            sshSession.timeout = 15_000
            sshSession.setServerAliveInterval(30_000)
            sshSession.setServerAliveCountMax(3)

            sshSession.connect()

            val assignedPort = sshSession.setPortForwardingL(
                config.localPort,
                "127.0.0.1",
                config.remotePort
            )

            session = sshSession
            _connectionState.value = ConnectionState.Connected(assignedPort)
            Result.success(assignedPort)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "SSH connection failed")
            Result.failure(e)
        }
    }

    suspend fun startRemoteServer(config: SshConfig): Result<String> = withContext(Dispatchers.IO) {
        val sshSession = session ?: return@withContext Result.failure(
            IllegalStateException("Not connected")
        )
        try {
            val channel = sshSession.openChannel("exec") as ChannelExec
            // Check if server is already running, if not start it
            channel.setCommand(
                "pgrep -f '${config.serverCommand}' > /dev/null 2>&1 && echo 'ALREADY_RUNNING' || " +
                "(nohup ${config.serverCommand} --port ${config.remotePort} > /tmp/claude-code-server.log 2>&1 & echo 'STARTED')"
            )
            val output = ByteArrayOutputStream()
            channel.outputStream = output
            channel.connect(10_000)

            // Wait for command to finish
            while (!channel.isClosed) {
                Thread.sleep(100)
            }
            channel.disconnect()

            val result = output.toString().trim()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        val sshSession = session ?: return@withContext Result.failure(
            IllegalStateException("Not connected")
        )
        try {
            val channel = sshSession.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            val output = ByteArrayOutputStream()
            val errOutput = ByteArrayOutputStream()
            channel.outputStream = output
            channel.setErrStream(errOutput)
            channel.connect(10_000)

            while (!channel.isClosed) {
                Thread.sleep(100)
            }
            val exitStatus = channel.exitStatus
            channel.disconnect()

            if (exitStatus == 0) {
                Result.success(output.toString().trim())
            } else {
                Result.failure(RuntimeException(errOutput.toString().trim()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean = session?.isConnected == true

    fun disconnect() {
        session?.let {
            if (it.isConnected) {
                it.disconnect()
            }
        }
        session = null
        tempKeyFile?.delete()
        tempKeyFile = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
