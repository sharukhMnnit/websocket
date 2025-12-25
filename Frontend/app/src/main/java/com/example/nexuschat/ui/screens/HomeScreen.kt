package com.example.nexuschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nexuschat.data.model.UserSummary
import com.example.nexuschat.ui.navigation.Screen
import com.example.nexuschat.viewmodel.HomeViewModel
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Check

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val requests by viewModel.friendRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    // 0 = Chats, 1 = Requests
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Nexus Chat") },
                    actions = {
                        IconButton(onClick = {
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0)
                            }
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF008069), titleContentColor = Color.White, actionIconContentColor = Color.White)
                )
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchUser(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    placeholder = { Text("Search Users...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    singleLine = true
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Chats") },
                    icon = { Text("💬") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Requests (${requests.size})") },
                    icon = { Text("👋") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {

            // If Searching, Show Search Results overlay
            if (searchQuery.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    item { Text("Search Results", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }
                    items(searchResults) { user ->
                        SearchUserItem(user) {
                            viewModel.sendFriendRequest(user.username)
                            searchQuery = "" // Clear search after sending
                        }
                    }
                }
            } else {
                // Main Content
                if (selectedTab == 0) {
                    // Chat List
                    LazyColumn {
                        items(friends) { friend ->
                            FriendItem(friend) {
                                navController.navigate(Screen.Chat.createRoute(friend.username))
                            }
                        }
                    }
                } else {
                    // Requests List
                    LazyColumn {
                        items(requests) { sender ->
                            RequestItem(sender) {
                                viewModel.acceptRequest(sender)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(user: UserSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(user.username)
            Spacer(Modifier.width(16.dp))
            Text(text = user.username, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RequestItem(username: String, onAccept: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(username)
            Spacer(Modifier.width(16.dp))
            Text(text = username, modifier = Modifier.weight(1f), fontSize = 18.sp)
            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) {
                Text("Accept")
            }
        }
    }
}

@Composable
fun SearchUserItem(user: UserSummary, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(user.username)
            Spacer(Modifier.width(16.dp))
            Text(text = user.username, modifier = Modifier.weight(1f), fontSize = 18.sp)
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = Color.Blue)
            }
        }
    }
}

@Composable
fun Avatar(name: String) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}