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

    private val _isVideoCallActive = MutableStateFlow(false)
    val isVideoCallActive = _isVideoCallActive.asStateFlow()

    init {
        _currentUser.value = repository.getCurrentUser() ?: ""

        viewModelScope.launch {
            repository.incomingMessages.collect { newMsg ->
                addOrUpdateMessage(newMsg)
                if (newMsg.sender != _currentUser.value) {
                    newMsg.id?.let { repository.sendReadAck(it) }
                }
            }
        }

        viewModelScope.launch {
            repository.incomingAcks.collect { ack ->
                _messages.update { list ->
                    list.map { msg -> if (msg.id == ack.messageId) msg.copy(status = ack.status) else msg }
                }
            }
        }

        // 3. Listen for WebRTC Signals
        viewModelScope.launch {
            repository.incomingSignals.collect { signal ->
                // NOTE: We do NOT set isVideoCallActive = true here automatically anymore.
                // The WebRtcManager will trigger the callback in ChatScreen to show the popup.
                webRtcManager.handleSignal(signal, _currentUser.value)
            }
        }
    }

    // --- Video Actions ---

    fun startVideoCall(targetUser: String) {
        val myName = _currentUser.value
        if (myName.isEmpty()) return

        _isVideoCallActive.value = true // Sender sees UI immediately
        webRtcManager.startCall(targetUser, myName, isVideo = true)
    }

    // ✅ NEW: User clicked "Accept" in the popup
    fun acceptCall() {
        _isVideoCallActive.value = true // Show Video Screen
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
        val msg = ChatMessage(content = content, sender = sender, receiver = receiver, timestamp = java.time.Instant.now().toString(), frontId = System.currentTimeMillis().toString(), status = MessageStatus.SENT)
        addOrUpdateMessage(msg)
        viewModelScope.launch(Dispatchers.IO) { repository.sendMessage(msg) }
    }

    private fun addOrUpdateMessage(msg: ChatMessage) {
        _messages.update { currentList ->
            val existingIndex = currentList.indexOfFirst { (it.id != null && it.id == msg.id) || (it.frontId != null && it.frontId == msg.frontId) }
            if (existingIndex != -1) { val m = currentList.toMutableList(); m[existingIndex] = msg; m } else { currentList + msg }
        }
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