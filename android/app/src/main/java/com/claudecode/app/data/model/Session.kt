package com.claudecode.app.data.model

data class Session(
    val id: String,
    val status: SessionStatus,
    val createdAt: Double,
    val lastActiveAt: Double? = null,
    val workingDirectory: String,
    val pid: Int? = null
)

enum class SessionStatus {
    Starting,
    Ready,
    Busy,
    Dead,
    Destroyed;

    companion object {
        fun fromString(value: String): SessionStatus = when (value) {
            "starting" -> Starting
            "ready" -> Ready
            "busy" -> Busy
            "dead" -> Dead
            "destroyed" -> Destroyed
            else -> Dead
        }
    }
}
