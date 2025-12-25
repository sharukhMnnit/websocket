package com.example.nexuschat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexuschat.data.model.ChatMessage
import com.example.nexuschat.data.model.MessageStatus
import com.example.nexuschat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentUser = MutableStateFlow("")
    val currentUser = _currentUser.asStateFlow()

    init {
        _currentUser.value = repository.getCurrentUser() ?: ""

        // 1. Real-time Listener (Incoming Messages)
        viewModelScope.launch {
            repository.incomingMessages.collect { newMsg ->
                addMessageToState(newMsg)

                // If message is from someone else, mark it READ automatically
                if (newMsg.sender != _currentUser.value) {
                    newMsg.id?.let { repository.sendReadAck(it) }
                }
            }
        }

        // 2. Real-time Listener (Blue Ticks/Acks)
        viewModelScope.launch {
            repository.incomingAcks.collect { ack ->
                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == ack.messageId) msg.copy(status = ack.status) else msg
                    }
                }
            }
        }
    }

    // Helper to safely add messages (Optimistic + Real-time)
    private fun addMessageToState(msg: ChatMessage) {
        _messages.update { currentList ->
            // 1. Check if we already have this message (by ID or FrontID)
            val existingIndex = currentList.indexOfFirst {
                (it.id != null && it.id == msg.id) ||
                        (it.frontId != null && it.frontId == msg.frontId)
            }

            if (existingIndex != -1) {
                // FOUND IT! We must REPLACE the old temporary message with the new real one
                // This ensures we get the real server ID needed for Blue Ticks
                val mutableList = currentList.toMutableList()
                mutableList[existingIndex] = msg
                mutableList
            } else {
                // New message, just add it
                currentList + msg
            }
        }
    }

    // FIX 1: Updated to match Repo signature (user1, user2)
    fun loadHistory(otherUser: String) {
        val myName = _currentUser.value
        if (myName.isEmpty()) return

        viewModelScope.launch {
            repository.getChatHistory(myName, otherUser).onSuccess { history ->
                _messages.value = history

                // Mark unread messages as read
                history.forEach { msg ->
                    if (msg.sender == otherUser && msg.status != MessageStatus.READ) {
                        msg.id?.let { repository.sendReadAck(it) }
                    }
                }
            }
        }
    }

    // FIX 2: Optimistic Update + Repo Object Passing
    fun sendMessage(content: String, receiver: String) {
        val sender = _currentUser.value
        if (sender.isEmpty()) return

        val msg = ChatMessage(
            content = content,
            sender = sender,
            receiver = receiver, // This connects the message to the specific friend
            timestamp = java.time.Instant.now().toString(),
            frontId = System.currentTimeMillis().toString(), // For duplicate detection
            status = MessageStatus.SENT
        )

        // 1. Show on screen IMMEDIATELY (Optimistic Update)
        addMessageToState(msg)

        // 2. Send to Server via WebSocket
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(msg)
        }
    }
}