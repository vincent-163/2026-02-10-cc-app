package com.claudecode.app.data.model

import java.util.UUID

sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    data class UserMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val content: String
    ) : ChatMessage()

    data class AssistantMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val content: List<ContentBlock>,
        val isStreaming: Boolean = false
    ) : ChatMessage()

    data class ResultMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val resultText: String,
        val totalCostUsd: Double? = null,
        val usage: Map<String, Any>? = null
    ) : ChatMessage()

    data class SystemMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val subtype: String,
        val sessionId: String? = null,
        val model: String? = null,
        val tools: List<String>? = null
    ) : ChatMessage()

    data class StatusMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val status: String
    ) : ChatMessage()

    data class ErrorMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val content: String
    ) : ChatMessage()

    data class ExitMessage(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val exitCode: Int
    ) : ChatMessage()

    data class ControlRequest(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Long = System.currentTimeMillis(),
        val requestId: String,
        val toolName: String,
        val toolInput: Map<String, Any>,
        val permissionSuggestions: List<Any> = emptyList(),
        val blockedPath: String? = null,
        val approved: Boolean? = null
    ) : ChatMessage()
}

sealed class ContentBlock {
    data class Text(val text: String) : ContentBlock()
    data class ToolUse(
        val id: String,
        val name: String,
        val input: Map<String, Any>
    ) : ContentBlock()
}
