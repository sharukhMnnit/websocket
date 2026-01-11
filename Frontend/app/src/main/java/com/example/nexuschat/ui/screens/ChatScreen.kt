package com.example.nexuschat.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nexuschat.data.model.ChatMessage
import com.example.nexuschat.data.model.MessageStatus
import com.example.nexuschat.util.WebRtcManager
import com.example.nexuschat.viewmodel.ChatViewModel
import org.webrtc.SurfaceViewRenderer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    otherUser: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isVideoCallActive by viewModel.isVideoCallActive.collectAsState()

    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var incomingCallSender by remember { mutableStateOf<String?>(null) }

    // Permissions & Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camera = permissions[Manifest.permission.CAMERA] ?: false
        val audio = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (camera && audio) viewModel.startVideoCall(otherUser)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.sendFile(it) } }

    // Initialize & Listeners
    LaunchedEffect(Unit) {
        viewModel.webRtcManager.initialize(context.applicationContext)
        viewModel.loadHistory(otherUser)

        viewModel.webRtcManager.onIncomingCall = { sender ->
            if (!viewModel.isVideoCallActive.value) incomingCallSender = sender
        }
        viewModel.webRtcManager.onCallEnded = {
            viewModel.setVideoCallActive(false)
            incomingCallSender = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        // We handle top bars automatically
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0072FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(otherUser.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = otherUser,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, "Attach", tint = Color.Gray)
                    }
                    IconButton(onClick = {
                        val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= 31) perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                        permissionLauncher.launch(perms.toTypedArray())
                    }) {
                        Icon(Icons.Default.Videocam, "Video", tint = Color(0xFF0072FF))
                    }
                }
            }
        },
        // ✅ FIX: Use 'bottomBar' for Input Area.
        // Scaffold automatically places this above the keyboard if 'adjustResize' is on.
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp, // Higher elevation to separate from list
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        // Only pad for the gesture bar (at the very bottom).
                        // Do NOT add imePadding() here because the window already resizes.
                        .navigationBarsPadding()
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp),
                        shape = RoundedCornerShape(24.dp),
                        placeholder = { Text("Type a message...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0072FF),
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                        containerColor = Color(0xFF0072FF),
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding) // Scaffold calculates space for topBar and bottomBar automatically
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(), // Fill remaining space
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        MessageBubble(msg, isMe = msg.sender == currentUser)
                    }
                }
            }

            if (isVideoCallActive) {
                VideoCallOverlay(
                    webRtcManager = viewModel.webRtcManager,
                    onEndCall = { viewModel.endCall(otherUser) }
                )
            }

            if (incomingCallSender != null) {
                AlertDialog(
                    onDismissRequest = { incomingCallSender = null },
                    title = { Text("Incoming Call") },
                    text = { Text("$incomingCallSender is video calling you...") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.acceptCall(); incomingCallSender = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) { Text("Accept") }
                    },
                    dismissButton = {
                        Button(
                            onClick = { viewModel.endCall(incomingCallSender!!); incomingCallSender = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Decline") }
                    }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
    val align = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val myBrush = Brush.horizontalGradient(colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
    val otherColor = MaterialTheme.colorScheme.surface

    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = align
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(2.dp, shape)
                .background(if (isMe) Color.Transparent else otherColor, shape)
                .background(if (isMe) myBrush else androidx.compose.ui.graphics.SolidColor(Color.Transparent), shape)
                .padding(12.dp)
        ) {
            Text(
                text = msg.content,
                fontSize = 16.sp,
                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(msg.timestamp),
                    fontSize = 10.sp,
                    color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                )

                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val (icon, tint) = when (msg.status) {
                        MessageStatus.SENT -> Icons.Default.Done to Color.White.copy(alpha = 0.7f)
                        MessageStatus.DELIVERED -> Icons.Default.DoneAll to Color.White.copy(alpha = 0.7f)
                        MessageStatus.RECEIVED -> Icons.Default.DoneAll to Color.White.copy(alpha = 0.7f)
                        MessageStatus.READ -> Icons.Default.DoneAll to Color(0xFFB3E5FC)
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

fun formatTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(timestamp)
        DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(instant)
    } catch (e: Exception) {
        try {
            val localDateTime = LocalDateTime.parse(timestamp)
            localDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e2: Exception) { "" }
    }
}

@Composable
fun VideoCallOverlay(webRtcManager: WebRtcManager, onEndCall: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachRemoteView(this) } },
            modifier = Modifier.fillMaxSize()
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp, 80.dp).size(120.dp, 160.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            AndroidView(
                factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachLocalView(this) } },
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(
            onClick = onEndCall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).size(60.dp).background(Color.Red, CircleShape)
        ) {
            Icon(Icons.Default.CallEnd, "End", tint = Color.White)
        }
    }
}