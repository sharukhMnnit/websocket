package com.example.nexuschat.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexuschat.data.model.ChatMessage
import com.example.nexuschat.data.model.MessageStatus
import com.example.nexuschat.data.repository.ChatRepository
import com.example.nexuschat.util.WebRtcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    val webRtcManager: WebRtcManager,
    savedStateHandle: SavedStateHandle // ✅ 1. Get the current chat user immediately
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentUser = MutableStateFlow("")
    val currentUser = _currentUser.asStateFlow()

    private val _isVideoCallActive = MutableStateFlow(false)
    val isVideoCallActive = _isVideoCallActive.asStateFlow()

    // The user we are currently chatting with (from Navigation arguments)
    private val currentChatUser: String? = savedStateHandle["username"]

    // Buffer for ACKs that arrive before the Echo
    private val pendingAcks = ConcurrentHashMap<String, MessageStatus>()

    init {
        _currentUser.value = repository.getCurrentUser() ?: ""

        // 1. Listen for Incoming Messages
        viewModelScope.launch {
            repository.incomingMessages.collect { newMsg ->

                // ✅ 2. FILTER: Only process messages for THIS chat
                // (Prevents ghost messages from other users appearing here)
                val isRelevant = currentChatUser != null &&
                        (newMsg.sender == currentChatUser ||
                                (newMsg.sender == _currentUser.value && newMsg.receiver == currentChatUser))

                if (isRelevant) {
                    addOrUpdateMessage(newMsg)

                    // ✅ 3. ACK: Only send Blue Ticks if it's from the person we are looking at
                    if (newMsg.sender == currentChatUser) {
                        newMsg.id?.let { repository.sendReadAck(it) }
                    }
                }
            }
        }

        // 2. Listen for ACKs (Blue Ticks)
        viewModelScope.launch {
            repository.incomingAcks.collect { ack ->
                Log.d("ChatViewModel", "ACK Received: ${ack.status} for ${ack.messageId}")

                var wasUpdated = false
                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == ack.messageId) {
                            wasUpdated = true
                            val mergedStatus = resolveStatus(msg.status, ack.status)
                            msg.copy(status = mergedStatus)
                        } else {
                            msg
                        }
                    }
                }

                if (!wasUpdated) {
                    Log.d("ChatViewModel", "Buffering ACK for unknown ID: ${ack.messageId}")
                    pendingAcks[ack.messageId] = ack.status
                }
            }
        }

        // 3. Listen for WebRTC Signals
        viewModelScope.launch {
            repository.incomingSignals.collect { signal ->
                webRtcManager.handleSignal(signal, _currentUser.value)
            }
        }
    }

    // --- Video Actions ---
    fun startVideoCall(targetUser: String) {
        val myName = _currentUser.value
        if (myName.isEmpty()) return
        _isVideoCallActive.value = true
        webRtcManager.startCall(targetUser, myName, isVideo = true)
    }

    fun acceptCall() {
        _isVideoCallActive.value = true
        webRtcManager.acceptCall(_currentUser.value)
    }

    fun setVideoCallActive(active: Boolean) {
        _isVideoCallActive.value = active
    }

    fun endCall(targetUser: String) {
        webRtcManager.endCall(targetUser, _currentUser.value)
        _isVideoCallActive.value = false
    }

    fun sendFile(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) { webRtcManager.sendFile(uri) }
    }

    // --- Chat Logic ---
    fun sendMessage(content: String, receiver: String) {
        val sender = _currentUser.value
        if (sender.isEmpty()) return

        // Create Optimistic Message
        val msg = ChatMessage(
            content = content,
            sender = sender,
            receiver = receiver,
            timestamp = java.time.Instant.now().toString(),
            frontId = System.currentTimeMillis().toString(),
            status = MessageStatus.SENT
        )

        addOrUpdateMessage(msg)
        viewModelScope.launch(Dispatchers.IO) { repository.sendMessage(msg) }
    }

    private fun addOrUpdateMessage(msg: ChatMessage) {
        _messages.update { currentList ->
            // Find existing message by Real ID OR Front ID
            val existingIndex = currentList.indexOfFirst {
                (it.id != null && it.id == msg.id) ||
                        (it.frontId != null && it.frontId == msg.frontId)
            }

            val pendingStatus = msg.id?.let { pendingAcks.remove(it) }
            val statusFromMsg = pendingStatus?.let { resolveStatus(msg.status, it) } ?: msg.status

            if (existingIndex != -1) {
                val currentMsg = currentList[existingIndex]
                val statusStep1 = resolveStatus(currentMsg.status, statusFromMsg)
                val finalId = msg.id ?: currentMsg.id
                val mergedMsg = msg.copy(id = finalId, status = statusStep1)

                val m = currentList.toMutableList()
                m[existingIndex] = mergedMsg
                m
            } else {
                val finalMsg = if (pendingStatus != null) msg.copy(status = statusFromMsg) else msg
                currentList + finalMsg
            }
        }
    }

    private fun resolveStatus(current: MessageStatus, incoming: MessageStatus): MessageStatus {
        if (current == MessageStatus.READ) return MessageStatus.READ
        if (incoming == MessageStatus.READ) return MessageStatus.READ
        if (current == MessageStatus.DELIVERED && incoming == MessageStatus.SENT) return MessageStatus.DELIVERED
        return incoming
    }

    fun loadHistory(otherUser: String) {
        val myName = _currentUser.value
        if (myName.isEmpty()) return
        viewModelScope.launch {
            repository.getChatHistory(myName, otherUser).onSuccess { history ->
                _messages.value = history
            }
        }
    }
}