package com.example.nexuschat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nexuschat.data.model.ChatMessage
import com.example.nexuschat.data.model.MessageStatus
import com.example.nexuschat.viewmodel.ChatViewModel
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    otherUser: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Load history when entering the screen
    LaunchedEffect(otherUser) {
        viewModel.loadHistory(otherUser)
    }

    // Auto-scroll to bottom when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUser) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF0F2F5)) // WhatsApp-like background color
        ) {
            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.sender == currentUser
                    MessageBubble(msg, isMe)
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            viewModel.sendMessage(text, otherUser)
                            text = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    color = if (isMe) Color(0xFFDCF8C6) else Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text = msg.content,
                fontSize = 16.sp,
                color = Color.Black
            )

            // Time & Status Row
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = msg.timestamp?.takeLast(8)?.take(5) ?: "", // Simple time formatting
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (msg.status) {
                            MessageStatus.SENT -> "✓"
                            MessageStatus.DELIVERED -> "✓✓"
                            MessageStatus.READ -> "✓✓"
                            else -> "•"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (msg.status == MessageStatus.READ) Color(0xFF34B7F1) else Color.Gray
                    )
                }
            }
        }
    }
}