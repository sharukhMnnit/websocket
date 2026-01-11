package com.example.nexuschat.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.util.Date
import java.util.Locale

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

    // 🔔 State for Incoming Call Dialog
    var incomingCallSender by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camera = permissions[Manifest.permission.CAMERA] ?: false
        val audio = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (camera && audio) {
            viewModel.startVideoCall(otherUser)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.sendFile(it) } }

    // --- INITIALIZE & LISTENERS ---
    LaunchedEffect(Unit) {
        viewModel.webRtcManager.initialize(context.applicationContext)
        viewModel.loadHistory(otherUser)

        // 1. Listen for Incoming Call
        viewModel.webRtcManager.onIncomingCall = { sender ->
            if (!viewModel.isVideoCallActive.value) {
                incomingCallSender = sender
            }
        }

        // 2. Listen for Call End
        viewModel.webRtcManager.onCallEnded = {
            viewModel.setVideoCallActive(false)
            incomingCallSender = null
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUser) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) { Icon(Icons.Default.AttachFile, "File") }
                    IconButton(onClick = {
                        val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= 31) perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                        permissionLauncher.launch(perms.toTypedArray())
                    }) { Icon(Icons.Default.Videocam, "Video") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF0F2F5))) {

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages) { msg ->
                        MessageBubble(msg, isMe = msg.sender == currentUser)
                    }
                }

                // Input Area
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        placeholder = { Text("Type a message...") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if(text.isNotBlank()) {
                                viewModel.sendMessage(text, otherUser)
                                text = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                    }
                }
            }

            // --- VIDEO OVERLAY ---
            if (isVideoCallActive) {
                VideoCallOverlay(
                    webRtcManager = viewModel.webRtcManager,
                    onEndCall = { viewModel.endCall(otherUser) }
                )
            }

            // --- INCOMING CALL DIALOG ---
            if (incomingCallSender != null) {
                AlertDialog(
                    onDismissRequest = { incomingCallSender = null },
                    title = { Text("Incoming Call") },
                    text = { Text("$incomingCallSender is video calling you...") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.acceptCall()
                                incomingCallSender = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) { Text("Accept") }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.endCall(incomingCallSender!!)
                                incomingCallSender = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Decline") }
                    }
                )
            }
        }
    }
}

@Composable
fun VideoCallOverlay(webRtcManager: WebRtcManager, onEndCall: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. REMOTE VIDEO
        AndroidView(
            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachRemoteView(this) } },
            modifier = Modifier.fillMaxSize(),
            onRelease = { renderer -> renderer.release() }
        )

        // 2. LOCAL VIDEO
        AndroidView(
            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachLocalView(this) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(100.dp, 150.dp).background(Color.DarkGray),
            onRelease = { renderer -> renderer.release() }
        )

        FloatingActionButton(
            onClick = onEndCall,
            containerColor = Color.Red,
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
        ) {
            Icon(Icons.Default.CallEnd, "End Call", tint = Color.White)
        }
    }
}

// ----------------------------------------------------------------
// 👇 UPDATED: FLEXIBLE TIME PARSING
// ----------------------------------------------------------------
@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
    val align = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isMe) Color(0xFFDCF8C6) else Color.White

    // Flexible Time Parsing
    val formattedTime = remember(msg.timestamp) {
        if (msg.timestamp.isNullOrBlank()) {
            ""
        } else {
            try {
                // 1. Try ISO Standard (2025-01-01T12:00:00Z)
                val instant = Instant.parse(msg.timestamp)
                DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(instant)
            } catch (e: Exception) {
                try {
                    // 2. Try Backend Format (2025-01-01T12:00:00) -> The one your friend uses
                    val localDateTime = LocalDateTime.parse(msg.timestamp)
                    val formatter = DateTimeFormatter.ofPattern("h:mm a")
                    localDateTime.format(formatter)
                } catch (e2: Exception) {
                    try {
                        // 3. Try Epoch Millis (1705000000000)
                        val millis = msg.timestamp.toLong()
                        val date = Date(millis)
                        java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                    } catch (e3: Exception) {
                        Log.e("ChatScreen", "Time Error: ${msg.timestamp}")
                        ""
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = align) {
        Column(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 300.dp)
                .background(color, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // 1. Message Content
            Text(
                text = msg.content,
                fontSize = 16.sp,
                color = Color.Black
            )

            // 2. Time & Status Row
            Row(
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TIME
                if (formattedTime.isNotEmpty()) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // TICKS (Only for me)
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val (icon, tint) = when (msg.status) {
                        MessageStatus.SENT -> Pair(Icons.Default.Done, Color.Gray)
                        MessageStatus.DELIVERED -> Pair(Icons.Default.DoneAll, Color.Gray)
                        MessageStatus.RECEIVED -> Pair(Icons.Default.DoneAll, Color.Gray)
                        MessageStatus.READ -> Pair(Icons.Default.DoneAll, Color(0xFF34B7F1)) // Blue
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = "Status",
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}