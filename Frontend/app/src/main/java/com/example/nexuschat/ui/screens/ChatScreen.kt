package com.example.nexuschat.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
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
    val isVideoCallActive by viewModel.isVideoCallActive.collectAsState()

    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // 🔔 State for Incoming Call Dialog
    var incomingCallSender by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.startVideoCall(otherUser)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.sendFile(it) } }

    // --- INITIALIZE & LISTENERS ---
    LaunchedEffect(Unit) {
        viewModel.webRtcManager.initialize(context)
        viewModel.webRtcManager.context = context
        viewModel.loadHistory(otherUser)

        // 1. Listen for Incoming Call
        viewModel.webRtcManager.onIncomingCall = { sender ->
            // Only show if not already in a call
            if (!viewModel.isVideoCallActive.value) {
                incomingCallSender = sender
            }
        }

        // 2. Listen for Call End (Remote Hangup)
        viewModel.webRtcManager.onCallEnded = {
            viewModel.setVideoCallActive(false)
            incomingCallSender = null
        }
    }

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
                        if (android.os.Build.VERSION.SDK_INT >= 31) perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                        permissionLauncher.launch(perms.toTypedArray())
                    }) { Icon(Icons.Default.Videocam, "Video") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF0F2F5))) {

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    items(messages) { msg -> MessageBubble(msg, isMe = msg.sender == currentUser) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp))
                    FloatingActionButton(onClick = { if(text.isNotBlank()) { viewModel.sendMessage(text, otherUser); text = "" } }) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                    }
                }
            }

            // --- VIDEO OVERLAY ---
            if (isVideoCallActive) {
                VideoCallOverlay(
                    webRtcManager = viewModel.webRtcManager,
                    onEndCall = {
                        // Send Bye signal when ending
                        viewModel.endCall(otherUser)
                    }
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
                                viewModel.endCall(incomingCallSender!!) // Decline sends a "Bye" or just closes
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
        // 1. REMOTE VIDEO (Full Screen)
        AndroidView(
            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachRemoteView(this) } },
            // 👇 CRITICAL: Removed background(Color.Black).
            // It was covering the video. Now it is transparent, so video shows through.
            modifier = Modifier.fillMaxSize()
        )

        // 2. LOCAL VIDEO (Bottom Right)
        AndroidView(
            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachLocalView(this) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(100.dp, 150.dp).background(Color.DarkGray)
        )

        FloatingActionButton(onClick = onEndCall, containerColor = Color.Red, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Icon(Icons.Default.CallEnd, "End Call", tint = Color.White)
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(modifier = Modifier.widthIn(max = 300.dp).background(if (isMe) Color(0xFFDCF8C6) else Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
            Text(text = msg.content, fontSize = 16.sp, color = Color.Black)
        }
    }
}