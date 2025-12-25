package com.example.nexuschat.data.model

import com.google.gson.annotations.SerializedName

// --- Auth ---
data class LoginRequest(val username: String, val password: String)
data class SignupRequest(val username: String, val password: String)

data class AuthResponse(
    @SerializedName("accessToken") val token: String,
    val username: String,
    val id: String
)

// --- User ---
data class UserSummary(val username: String)

// --- Chat ---
data class ChatMessage(
    val id: String? = null,
    val content: String,
    val sender: String,
    val receiver: String? = null,
    val type: MessageType = MessageType.CHAT,
    val timestamp: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val frontId: String? = null // For UI optimism
)

enum class MessageType { CHAT, JOIN, LEAVE }

enum class MessageStatus { RECEIVED, DELIVERED, READ, SENT }

// --- Ack ---
data class MessageAck(val messageId: String, val status: MessageStatus)