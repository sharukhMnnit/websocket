package com.example.nexuschat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nexuschat.ui.navigation.Screen
import com.example.nexuschat.viewmodel.AuthState
import com.example.nexuschat.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Observe Authentication State
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                // Navigate to Home and clear Login from backstack
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is AuthState.SignupSuccess -> {
                Toast.makeText(context, "Signup Successful! Please Login.", Toast.LENGTH_SHORT).show()
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Nexus Chat", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(32.dp))

        // Username Field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        if (authState is AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            // Login Button
            Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }

            Spacer(Modifier.height(8.dp))

            // Signup Button
            TextButton(
                onClick = { viewModel.signup(username, password) }
            ) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}