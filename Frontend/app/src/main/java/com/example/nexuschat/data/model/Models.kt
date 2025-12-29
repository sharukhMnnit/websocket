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
data class UserSummary(
    val username: String,
    val unreadCount: Int = 0 // <--- CRITICAL: Must be here for Badges
)

// --- Chat ---
data class ChatMessage(
    val id: String? = null,
    val content: String,
    val sender: String,
    val receiver: String? = null,
    val type: MessageType = MessageType.CHAT,
    val timestamp: String? = null, // Can be ISO String ("2025-...") or Epoch ("1766...")
    val status: MessageStatus = MessageStatus.SENT,
    val frontId: String? = null
)

enum class MessageType { CHAT, JOIN, LEAVE }
enum class MessageStatus { RECEIVED, DELIVERED, READ, SENT }
data class MessageAck(val messageId: String, val status: MessageStatus)