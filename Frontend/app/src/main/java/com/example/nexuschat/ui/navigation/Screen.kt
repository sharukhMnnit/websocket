package com.example.nexuschat.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Chat : Screen("chat/{username}") {
        fun createRoute(username: String) = "chat/$username"
    }
}