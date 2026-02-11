package com.claudecode.app

import android.app.Application
import com.claudecode.app.data.SettingsRepository
import com.claudecode.app.network.ApiClient
import com.claudecode.app.ssh.SshManager

class ClaudeCodeApp : Application() {

    lateinit var sshManager: SshManager
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sshManager = SshManager()
        apiClient = ApiClient()
        settingsRepository = SettingsRepository(this)
    }
}
