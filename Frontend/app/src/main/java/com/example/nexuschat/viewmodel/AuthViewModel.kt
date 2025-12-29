package com.example.nexuschat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexuschat.data.model.LoginRequest
import com.example.nexuschat.data.model.SignupRequest
import com.example.nexuschat.data.remote.ApiService
import com.example.nexuschat.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun login(username: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.login(LoginRequest(username, pass))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Save Token & User Info
                    tokenManager.saveAuthDetails(body.token, body.username, body.id)
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Login Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Connection Error: ${e.localizedMessage}")
            }
        }
    }

    fun signup(username: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.signup(SignupRequest(username, pass))
                if (response.isSuccessful) {
                    _authState.value = AuthState.SignupSuccess
                } else {
                    _authState.value = AuthState.Error("Signup Failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error: ${e.localizedMessage}")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object SignupSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}