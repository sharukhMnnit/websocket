
package com.example.nexuschat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexuschat.data.model.UserSummary
import com.example.nexuschat.data.remote.ApiService
import com.example.nexuschat.util.TokenManager
import com.example.nexuschat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val api: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    // Lists
    private val _friends = MutableStateFlow<List<UserSummary>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserSummary>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<String>>(emptyList())
    val friendRequests = _friendRequests.asStateFlow()

    private val _currentUser = MutableStateFlow("")
    val currentUser = _currentUser.asStateFlow()

    init {
        _currentUser.value = tokenManager.getUsername() ?: ""
        loadAll()
        repository.connectSocket()
    }

    fun loadAll() {
        fetchFriends()
        fetchRequests()
    }

    fun fetchFriends() {
        viewModelScope.launch {
            repository.getFriends().onSuccess { _friends.value = it }
        }
    }

    fun searchUser(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenManager.getToken()}"
                val results = api.searchUsers(token, query)
                _searchResults.value = results.filter { it.username != _currentUser.value }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchRequests() {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenManager.getToken()}"
                _friendRequests.value = api.getFriendRequests(token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendFriendRequest(targetUser: String) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenManager.getToken()}"
                api.sendFriendRequest(token, targetUser)
                // Optionally clear search or show toast
                _searchResults.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun acceptRequest(senderUser: String) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${tokenManager.getToken()}"
                api.acceptFriendRequest(token, senderUser)
                // Refresh lists
                fetchRequests()
                fetchFriends()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        tokenManager.clear()
        // Handle navigation in UI
    }
}
