package com.example.nexuschat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nexuschat.ui.navigation.Screen
import com.example.nexuschat.ui.screens.ChatScreen
import com.example.nexuschat.ui.screens.HomeScreen
import com.example.nexuschat.ui.screens.LoginScreen
import com.example.nexuschat.ui.theme.NexuschatTheme // Make sure this matches your theme folder name
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexuschatTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        // 1. Login Screen
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // 2. Home Screen (User List)
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        // 3. Chat Screen (Takes "username" as argument)
        composable(
            route = "chat/{username}",
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            ChatScreen(
                navController = navController,
                otherUser = username
            )
        }
    }
}