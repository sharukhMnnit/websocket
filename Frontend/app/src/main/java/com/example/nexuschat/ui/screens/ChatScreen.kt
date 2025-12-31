//package com.example.nexuschat.ui.screens
//
//import android.Manifest
//import android.net.Uri
//import android.widget.Toast
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.Send
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.AttachFile
//import androidx.compose.material.icons.filled.CallEnd
//import androidx.compose.material.icons.filled.Videocam
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavController
//import com.example.nexuschat.data.model.ChatMessage
//import com.example.nexuschat.data.model.MessageStatus
//import com.example.nexuschat.util.WebRtcManager
//import com.example.nexuschat.viewmodel.ChatViewModel
//import org.webrtc.SurfaceViewRenderer
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.TimeZone
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ChatScreen(
//    navController: NavController,
//    otherUser: String,
//    viewModel: ChatViewModel = hiltViewModel()
//) {
//    val messages by viewModel.messages.collectAsState()
//    val currentUser by viewModel.currentUser.collectAsState()
//    val isVideoCallActive by viewModel.isVideoCallActive.collectAsState()
//
//    var text by remember { mutableStateOf("") }
//    val listState = rememberLazyListState()
//    val context = LocalContext.current
//
//    // permissionLauncher handles the camera permissions
//    val permissionLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
//        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
//
//        if (cameraGranted && audioGranted) {
//            viewModel.startVideoCall(otherUser)
//        } else {
//            Toast.makeText(context, "Permissions required for Video Call", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    val filePickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let {
//            viewModel.sendMessage("Sent a file", otherUser)
//            viewModel.sendFile(it)
//        }
//    }
//
//    // Initialize WebRTC on screen load
//    LaunchedEffect(Unit) {
//        viewModel.webRtcManager.initialize(context)
//        viewModel.webRtcManager.context = context
//        viewModel.loadHistory(otherUser)
//    }
//
//    // Auto-scroll to bottom of chat
//    LaunchedEffect(messages.size) {
//        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(otherUser) },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
//                        Icon(Icons.Default.AttachFile, contentDescription = "Send File")
//                    }
//                    IconButton(onClick = {
//                        val permissions = mutableListOf(
//                            android.Manifest.permission.CAMERA,
//                            android.Manifest.permission.RECORD_AUDIO
//                        )
//                        // Add BLUETOOTH_CONNECT only for Android 12+ (SDK 31+)
//                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//                            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
//                        }
//
//                        permissionLauncher.launch(permissions.toTypedArray())
//                    }) {
//                        Icon(Icons.Default.Videocam, contentDescription = "Video Call")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
//            )
//        }
//    ) { padding ->
//        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF0F2F5))) {
//
//            Column(modifier = Modifier.fillMaxSize()) {
//                LazyColumn(
//                    state = listState,
//                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
//                    verticalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    items(messages) { msg ->
//                        MessageBubble(msg, isMe = msg.sender == currentUser)
//                    }
//                }
//
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(8.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    OutlinedTextField(
//                        value = text,
//                        onValueChange = { text = it },
//                        modifier = Modifier.weight(1f),
//                        placeholder = { Text("Type a message...") },
//                        shape = RoundedCornerShape(24.dp),
//                        colors = TextFieldDefaults.colors(
//                            focusedIndicatorColor = Color.Transparent,
//                            unfocusedIndicatorColor = Color.Transparent
//                        )
//                    )
//                    Spacer(Modifier.width(8.dp))
//                    FloatingActionButton(
//                        onClick = {
//                            if (text.isNotBlank()) {
//                                viewModel.sendMessage(text, otherUser)
//                                text = ""
//                            }
//                        },
//                        containerColor = MaterialTheme.colorScheme.primary
//                    ) {
//                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
//                    }
//                }
//            }
//
//            // Simple Video Overlay - No Dialogs
//            if (isVideoCallActive) {
//                VideoCallOverlay(
//                    webRtcManager = viewModel.webRtcManager,
//                    onEndCall = { viewModel.endCall() }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun VideoCallOverlay(webRtcManager: WebRtcManager, onEndCall: () -> Unit) {
//    Box(modifier = Modifier.fillMaxSize()) {
//        AndroidView(
//            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachRemoteView(this) } },
//            modifier = Modifier.fillMaxSize()
//        )
//        AndroidView(
//            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachLocalView(this) } },
//            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(100.dp, 150.dp).background(Color.DarkGray)
//        )
//        FloatingActionButton(onClick = onEndCall, containerColor = Color.Red, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
//            Icon(Icons.Default.CallEnd, "End Call", tint = Color.White)
//        }
//    }
//}
//
//@Composable
//fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
//    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
//        Column(modifier = Modifier.widthIn(max = 300.dp).background(if (isMe) Color(0xFFDCF8C6) else Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
//            Text(text = msg.content, fontSize = 16.sp, color = Color.Black)
//            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
//                Text(text = formatMessageTime(msg.timestamp), fontSize = 11.sp, color = Color.Gray)
//                if (isMe) {
//                    Spacer(Modifier.width(4.dp))
//                    Text(text = if (msg.status == MessageStatus.READ) "✓✓" else "✓", fontSize = 12.sp, color = if (msg.status == MessageStatus.READ) Color(0xFF34B7F1) else Color.Gray)
//                }
//            }
//        }
//    }
//}
//

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.startVideoCall(otherUser)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.webRtcManager.initialize(context)
        viewModel.webRtcManager.context = context
        viewModel.loadHistory(otherUser)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUser) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
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
            if (isVideoCallActive) {
                VideoCallOverlay(viewModel.webRtcManager) { viewModel.endCall() }
            }
        }
    }
}

@Composable
fun VideoCallOverlay(webRtcManager: WebRtcManager, onEndCall: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachRemoteView(this) } },
            modifier = Modifier.fillMaxSize()
        )
        AndroidView(
            factory = { ctx -> SurfaceViewRenderer(ctx).apply { webRtcManager.attachLocalView(this) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(100.dp, 150.dp).background(Color.DarkGray)
        )
        FloatingActionButton(onClick = onEndCall, containerColor = Color.Red, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Icon(Icons.Default.CallEnd, "End Call", tint = Color.White)
        }
    }
}

// ... (MessageBubble remains same) ...
@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(modifier = Modifier.widthIn(max = 300.dp).background(if (isMe) Color(0xFFDCF8C6) else Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
            Text(text = msg.content, fontSize = 16.sp, color = Color.Black)
        }
    }
}

fun formatMessageTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    try { return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp.toLong())) } catch (e: Exception) {}
    try {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date = iso.parse(if (timestamp.length >= 19) timestamp.substring(0, 19) else timestamp)
        if (date != null) return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    } catch (e: Exception) {}
    return "Now"
}