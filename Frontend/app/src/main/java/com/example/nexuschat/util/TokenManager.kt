package com.example.nexuschat.util

import android.content.Context

class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)

    fun saveAuthDetails(token: String, username: String, userId: String) {
        prefs.edit()
            .putString("jwt", token)
            .putString("username", username)
            .putString("userid", userId)
            .apply()
    }

    fun getToken(): String? = prefs.getString("jwt", null)
    fun getUsername(): String? = prefs.getString("username", null)
    fun getUserId(): String? = prefs.getString("userid", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}