package com.example.nexuschat.viewmodel

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
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    val webRtcManager: WebRtcManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentUser = MutableStateFlow("")
    val currentUser = _currentUser.asStateFlow()

    // Controls the Video Overlay
    private val _isVideoCallActive = MutableStateFlow(false)
    val isVideoCallActive = _isVideoCallActive.asStateFlow()

    init {
        _currentUser.value = repository.getCurrentUser() ?: ""

        // 1. Listen for Chat Messages
        viewModelScope.launch {
            repository.incomingMessages.collect { newMsg ->
                addOrUpdateMessage(newMsg)
                if (newMsg.sender != _currentUser.value) {
                    newMsg.id?.let { repository.sendReadAck(it) }
                }
            }
        }

        // 2. Listen for Blue Ticks (Acks)
        viewModelScope.launch {
            repository.incomingAcks.collect { ack ->
                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == ack.messageId) msg.copy(status = ack.status) else msg
                    }
                }
            }
        }

        // 3. Listen for Video Call Signals
        viewModelScope.launch {
            repository.incomingSignals.collect { signal ->
                // If we receive an OFFER, the other person is calling.
                // Since we are in "Auto-Answer" mode, we just show the screen immediately.
                if (signal.type == "offer") {
                    _isVideoCallActive.value = true
                }

                // Let the Manager handle the technical WebRTC handshake
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

    fun endCall() {
        webRtcManager.endCall()
        _isVideoCallActive.value = false
    }

    fun sendFile(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            webRtcManager.sendFile(uri)
        }
    }

    // --- Chat Actions ---
    fun sendMessage(content: String, receiver: String) {
        val sender = _currentUser.value
        if (sender.isEmpty()) return

        val msg = ChatMessage(
            content = content,
            sender = sender,
            receiver = receiver,
            timestamp = java.time.Instant.now().toString(),
            frontId = System.currentTimeMillis().toString(),
            status = MessageStatus.SENT
        )

        addOrUpdateMessage(msg)

        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(msg)
        }
    }

    private fun addOrUpdateMessage(msg: ChatMessage) {
        _messages.update { currentList ->
            val existingIndex = currentList.indexOfFirst {
                (it.id != null && it.id == msg.id) ||
                        (it.frontId != null && it.frontId == msg.frontId) ||
                        (it.id == null && it.sender == msg.sender && it.content == msg.content)
            }

            if (existingIndex != -1) {
                val mutableList = currentList.toMutableList()
                mutableList[existingIndex] = msg
                mutableList
            } else {
                currentList + msg
            }
        }
    }

    fun loadHistory(otherUser: String) {
        val myName = _currentUser.value
        if (myName.isEmpty()) return

        viewModelScope.launch {
            repository.getChatHistory(myName, otherUser).onSuccess { history ->
                _messages.value = history
                history.forEach { msg ->
                    if (msg.sender == otherUser && msg.status != MessageStatus.READ) {
                        msg.id?.let { repository.sendReadAck(it) }
                    }
                }
            }
        }
    }
}