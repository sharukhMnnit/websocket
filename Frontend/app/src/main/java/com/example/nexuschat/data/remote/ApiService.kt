package com.example.nexuschat.data.remote

import com.example.nexuschat.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // --- Auth & Chat ---
    @POST("api/auth/signin")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<Any>

    @GET("api/users/users")
    suspend fun getFriends(@Header("Authorization") token: String): List<UserSummary>

    @GET("api/messages/{user1}/{user2}")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("user1") user1: String,
        @Path("user2") user2: String
    ): List<ChatMessage>

    // --- NEW: Search & Friend Requests ---

    // Search for users by username (e.g., "sha")
    @GET("api/users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): List<UserSummary>

    // Get list of pending friend requests (usernames)
    @GET("api/friends/requests")
    suspend fun getFriendRequests(
        @Header("Authorization") token: String
    ): List<String>

    // Send a friend request
    @POST("api/friends/add/{username}")
    suspend fun sendFriendRequest(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): Response<Any>

    // Accept a friend request
    @POST("api/friends/accept/{username}")
    suspend fun acceptFriendRequest(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): Response<Any>
}