package com.claudecode.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.claudecode.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val defaultModel by viewModel.defaultModel.collectAsState()
    val lastWorkingDir by viewModel.lastWorkingDir.collectAsState()
    val serverCommand by viewModel.serverCommand.collectAsState()
    val authToken by viewModel.authToken.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Authentication", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = authToken,
                onValueChange = { viewModel.updateAuthToken(it) },
                label = { Text("Auth Token") },
                placeholder = { Text("Bearer token (leave empty if auth disabled)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Token for server API authentication. Leave empty if server has no tokens configured.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Session Defaults", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = defaultModel,
                onValueChange = { viewModel.updateDefaultModel(it) },
                label = { Text("Default Model") },
                placeholder = { Text("opus, sonnet, haiku...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Passed as --model to Claude Code when creating sessions",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = lastWorkingDir,
                onValueChange = { viewModel.updateLastWorkingDir(it) },
                label = { Text("Default Working Directory") },
                placeholder = { Text("/home/user/projects") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Pre-filled when creating new sessions",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Server", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = serverCommand,
                onValueChange = { viewModel.updateServerCommand(it) },
                label = { Text("Server Command") },
                placeholder = { Text("claude-code-server") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Command used to start the server on the remote machine",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("About", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Claude Code Android v1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                "Connects to a Claude Code server via SSH tunnel",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
