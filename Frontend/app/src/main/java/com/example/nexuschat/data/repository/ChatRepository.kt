package com.example.nexuschat.data.repository

import com.example.nexuschat.data.model.ChatMessage
import com.example.nexuschat.data.model.UserSummary
import com.example.nexuschat.data.remote.ApiService
import com.example.nexuschat.data.remote.WebSocketClient
import com.example.nexuschat.util.TokenManager
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val api: ApiService,
    private val webSocketClient: WebSocketClient,
    private val tokenManager: TokenManager
) {
    // 1. Expose flows so ViewModel can listen
    val incomingMessages = webSocketClient.incomingMessages
    val incomingAcks = webSocketClient.incomingAcks

    fun connectSocket() {
        tokenManager.getToken()?.let { webSocketClient.connect(it) }
    }

    fun disconnectSocket() = webSocketClient.disconnect()

    suspend fun getFriends(): Result<List<UserSummary>> = runCatching {
        val token = "Bearer ${tokenManager.getToken()}"
        api.getFriends(token)
    }

    // Renamed to match ViewModel call
    suspend fun getChatHistory(user1: String, user2: String): Result<List<ChatMessage>> = runCatching {
        val token = "Bearer ${tokenManager.getToken()}"
        api.getChatHistory(token, user1, user2)
    }

    // Updated to accept the full object (Fixes the ViewModel error)
    fun sendMessage(msg: ChatMessage) {
        webSocketClient.sendMessage(msg)
    }

    fun sendReadAck(msgId: String) {
        webSocketClient.sendAck(msgId, "READ")
    }

    fun getCurrentUser() = tokenManager.getUsername()
}