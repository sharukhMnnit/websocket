package com.example.nexuschat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nexuschat.ui.navigation.Screen
import com.example.nexuschat.ui.screens.ChatScreen
import com.example.nexuschat.ui.screens.HomeScreen
import com.example.nexuschat.ui.screens.LoginScreen
import com.example.nexuschat.ui.theme.NexuschatTheme
import com.example.nexuschat.util.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager // ✅ 1. Inject TokenManager for Auto-Login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 2. Fix "Merged Screen": Enable Edge-to-Edge properly
        enableEdgeToEdge()

        // Check if user is already logged in
        val startRoute = if (tokenManager.getToken() != null) Screen.Home.route else Screen.Login.route

        setContent {
            NexuschatTheme {
                // ✅ 3. Fix "Light Theme in Dark Mode": Use Surface with correct background
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startRoute)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String) { // ✅ Updated to accept startDestination
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

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