package com.claudecode.app.ui.connection

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.claudecode.app.data.model.AuthMethod
import com.claudecode.app.data.model.ConnectionMode
import com.claudecode.app.data.model.ConnectionState
import com.claudecode.app.ui.theme.TextSecondary

private enum class KeyInputMode { FilePath, Paste }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onConnected: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val directConnected by viewModel.directConnected.collectAsState()
    val config by viewModel.config.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current

    var keyInputMode by remember(config.privateKeyContent) {
        mutableStateOf(
            if (config.privateKeyContent.isNotBlank()) KeyInputMode.Paste
            else KeyInputMode.FilePath
        )
    }

    // Storage permission state
    var hasStoragePermission by remember { mutableStateOf(false) }
    var pendingConnect by remember { mutableStateOf(false) }

    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(Unit) {
        hasStoragePermission = checkStoragePermission()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted && pendingConnect) {
            pendingConnect = false
            viewModel.connect()
        }
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else false
        hasStoragePermission = granted
        if (granted && pendingConnect) {
            pendingConnect = false
            viewModel.connect()
        }
    }

    fun requestStoragePermissionAndConnect() {
        pendingConnect = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            manageStorageLauncher.launch(intent)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun handleConnect() {
        if (config.needsStoragePermission && !checkStoragePermission()) {
            requestStoragePermissionAndConnect()
        } else {
            viewModel.connect()
        }
    }

    val isConnected = when (config.connectionMode) {
        ConnectionMode.DirectAPI -> directConnected
        ConnectionMode.SSH -> connectionState is ConnectionState.Connected
    }

    LaunchedEffect(isConnected, statusMessage) {
        if (isConnected && statusMessage.startsWith("Connected")) {
            onConnected()
        }
    }

    val isConnecting = when (config.connectionMode) {
        ConnectionMode.DirectAPI -> statusMessage == "Connecting to API server..."
        ConnectionMode.SSH -> connectionState is ConnectionState.Connecting
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claude Code") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Connection",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Connection mode toggle
            Text("Mode", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = config.connectionMode == ConnectionMode.SSH,
                    onClick = { viewModel.updateConfig(config.copy(connectionMode = ConnectionMode.SSH)) },
                    label = { Text("SSH Tunnel") }
                )
                FilterChip(
                    selected = config.connectionMode == ConnectionMode.DirectAPI,
                    onClick = { viewModel.updateConfig(config.copy(connectionMode = ConnectionMode.DirectAPI)) },
                    label = { Text("Direct API") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (config.connectionMode == ConnectionMode.DirectAPI) {
                // Direct API mode
                OutlinedTextField(
                    value = config.directApiUrl,
                    onValueChange = { viewModel.updateConfig(config.copy(directApiUrl = it)) },
                    label = { Text("API Server URL") },
                    placeholder = { Text("http://192.168.1.100:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // SSH mode
                OutlinedTextField(
                    value = config.host,
                    onValueChange = { viewModel.updateConfig(config.copy(host = it)) },
                    label = { Text("Host") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = config.port.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { port ->
                                viewModel.updateConfig(config.copy(port = port))
                            }
                        },
                        label = { Text("SSH Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = config.username,
                        onValueChange = { viewModel.updateConfig(config.copy(username = it)) },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Authentication", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = config.authMethod == AuthMethod.Password,
                        onClick = { viewModel.updateConfig(config.copy(authMethod = AuthMethod.Password)) },
                        label = { Text("Password") }
                    )
                    FilterChip(
                        selected = config.authMethod == AuthMethod.PrivateKey,
                        onClick = { viewModel.updateConfig(config.copy(authMethod = AuthMethod.PrivateKey)) },
                        label = { Text("SSH Key") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (config.authMethod == AuthMethod.Password) {
                    OutlinedTextField(
                        value = config.password,
                        onValueChange = { viewModel.updateConfig(config.copy(password = it)) },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = keyInputMode == KeyInputMode.FilePath,
                            onClick = { keyInputMode = KeyInputMode.FilePath },
                            label = { Text("File Path") }
                        )
                        FilterChip(
                            selected = keyInputMode == KeyInputMode.Paste,
                            onClick = { keyInputMode = KeyInputMode.Paste },
                            label = { Text("Paste Key") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (keyInputMode == KeyInputMode.FilePath) {
                        OutlinedTextField(
                            value = config.privateKeyPath,
                            onValueChange = {
                                viewModel.updateConfig(config.copy(
                                    privateKeyPath = it,
                                    privateKeyContent = ""
                                ))
                            },
                            label = { Text("Private Key Path") },
                            placeholder = { Text("/storage/emulated/0/.ssh/id_ed25519") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = config.privateKeyContent,
                            onValueChange = {
                                viewModel.updateConfig(config.copy(
                                    privateKeyContent = it,
                                    privateKeyPath = ""
                                ))
                            },
                            label = { Text("Private Key Content") },
                            placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----\n...") },
                            singleLine = false,
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = config.privateKeyPassphrase,
                        onValueChange = { viewModel.updateConfig(config.copy(privateKeyPassphrase = it)) },
                        label = { Text("Key Passphrase (optional)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Server", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = config.remotePort.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { port ->
                                viewModel.updateConfig(config.copy(remotePort = port))
                            }
                        },
                        label = { Text("Remote Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = config.localPort.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { port ->
                                viewModel.updateConfig(config.copy(localPort = port))
                            }
                        },
                        label = { Text("Local Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val canConnect = when (config.connectionMode) {
                ConnectionMode.DirectAPI -> config.directApiUrl.isNotBlank()
                ConnectionMode.SSH -> config.host.isNotBlank() && config.username.isNotBlank()
            }

            Button(
                onClick = { handleConnect() },
                enabled = !isConnecting && canConnect,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isConnecting) "Connecting..." else "Connect")
            }

            if (isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disconnect")
                }
            }

            if (statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                val isError = when (config.connectionMode) {
                    ConnectionMode.SSH -> connectionState is ConnectionState.Error
                    ConnectionMode.DirectAPI -> statusMessage.startsWith("Connection failed")
                }
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        TextSecondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
