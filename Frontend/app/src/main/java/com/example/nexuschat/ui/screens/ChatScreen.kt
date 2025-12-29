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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nexuschat.data.model.ChatMessage
import com.example.nexuschat.data.model.MessageStatus
import com.example.nexuschat.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    LaunchedEffect(otherUser) { viewModel.loadHistory(otherUser) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF0F2F5))) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg, isMe = msg.sender == currentUser)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
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
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            viewModel.sendMessage(text, otherUser)
                            text = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
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
            Text(text = msg.content, fontSize = 16.sp, color = Color.Black)

            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                // Use the new Robust Time Logic
                val timeString = remember(msg.timestamp) {
                    formatMessageTime(msg.timestamp)
                }

                Text(text = timeString, fontSize = 11.sp, color = Color.Gray)

                if (isMe) {
                    Spacer(Modifier.width(4.dp))
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

// --- UNIVERSAL DATE PARSER (Works on Old & New Android) ---
fun formatMessageTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""

    // 1. Try parsing as Number (Epoch Millis: "1766...")
    try {
        val millis = timestamp.toLong()
        val date = Date(millis)
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        // Not a number, continue to step 2...
    }

    // 2. Try parsing as ISO 8601 (Web Format: "2025-12-25T12:00:00.123Z")
    // This uses SimpleDateFormat which works on ALL Android versions (no crash on API < 26)
    try {
        // Pattern matches the standard web ISO string
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoFormat.timeZone = TimeZone.getTimeZone("UTC") // Web sends UTC

        // We substring to remove the ".123Z" or "Z" parts to make parsing simpler/universal
        // Takes first 19 chars: "2025-12-25T12:00:00"
        val cleanTimestamp = if (timestamp.length >= 19) timestamp.substring(0, 19) else timestamp

        val date = isoFormat.parse(cleanTimestamp)
        if (date != null) {
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        // Ignore
    }

    // 3. Fallback: If EVERYTHING fails, show the raw text so we know it exists
    // (We shorten it to prevent it from breaking the layout)
    return if (timestamp.length > 5 && timestamp.contains(":")) {
        timestamp.substring(timestamp.indexOf(":") - 2, timestamp.indexOf(":") + 3) // Try to grab "12:00"
    } else {
        "Now"
    }
}