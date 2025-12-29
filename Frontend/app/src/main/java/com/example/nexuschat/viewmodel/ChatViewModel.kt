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

        // 1. Incoming Messages
        viewModelScope.launch {
            repository.incomingMessages.collect { newMsg ->
                // IMPORTANT: Swaps Temp ID for Real ID so Blue Ticks work
                addOrUpdateMessage(newMsg)

                if (newMsg.sender != _currentUser.value) {
                    newMsg.id?.let { repository.sendReadAck(it) }
                }
            }
        }

        // 2. Incoming Blue Ticks
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

    // FIX FOR BLUE TICKS: Find the matching message and UPDATE it with the Server ID
    private fun addOrUpdateMessage(msg: ChatMessage) {
        _messages.update { currentList ->
            val existingIndex = currentList.indexOfFirst {
                (it.id != null && it.id == msg.id) ||
                        (it.frontId != null && it.frontId == msg.frontId) ||
                        // Fallback: If frontId is missing, match by content & sender
                        (it.id == null && it.sender == msg.sender && it.content == msg.content)
            }

            if (existingIndex != -1) {
                val mutableList = currentList.toMutableList()
                mutableList[existingIndex] = msg // Replace with Real Server Message
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

    fun sendMessage(content: String, receiver: String) {
        val sender = _currentUser.value
        if (sender.isEmpty()) return

        val msg = ChatMessage(
            content = content,
            sender = sender,
            receiver = receiver,
            // FIX FOR MISSING TIME: Send ISO String (Matches Web)
            timestamp = java.time.Instant.now().toString(),
            frontId = System.currentTimeMillis().toString(),
            status = MessageStatus.SENT
        )

        addOrUpdateMessage(msg) // Show instantly

        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(msg)
        }
    }
}