package com.example.nexuschat.data.remote

import android.util.Log
import com.example.nexuschat.data.model.ChatMessage
import com.example.nexuschat.data.model.MessageAck
import com.google.gson.Gson
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) : WebSocketListener() {

    private var webSocket: WebSocket? = null

    // IMPORTANT:
    // 1. Use 'wss://' for Cloudflare (Secure WebSocket).
    // 2. Update this URL every time you restart the Cloudflare tunnel!
    private val WS_URL = "wss://marriage-villages-sacramento-skirts.trycloudflare.com/ws/websocket"

    // FIX 1: Add buffer capacity so messages don't get dropped when UI is busy
    private val _incomingMessages = MutableSharedFlow<ChatMessage>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages

    private val _incomingAcks = MutableSharedFlow<MessageAck>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingAcks: SharedFlow<MessageAck> = _incomingAcks

    fun connect(token: String) {
        if (webSocket != null) return // Already connected

        val request = Request.Builder().url(WS_URL).build()
        webSocket = client.newWebSocket(request, this)

        // Send STOMP CONNECT Frame
        val connectFrame = "CONNECT\naccept-version:1.1,1.0\nAuthorization:Bearer $token\n\n\u0000"
        webSocket?.send(connectFrame)
    }

    fun subscribe(topic: String) {
        val id = "sub-${System.currentTimeMillis()}"
        val frame = "SUBSCRIBE\nid:$id\ndestination:$topic\n\n\u0000"
        webSocket?.send(frame)
    }

    fun sendMessage(msg: ChatMessage) {
        val json = gson.toJson(msg)
        val length = json.toByteArray(Charsets.UTF_8).size

        // Strict STOMP format with content-length
        val frame = "SEND\n" +
                "destination:/app/chat.private\n" +
                "content-type:application/json\n" +
                "content-length:$length\n" +
                "\n" +
                json +
                "\u0000"

        Log.d("WebSocket", "Sending: $frame")

        val success = webSocket?.send(frame) ?: false
        if (!success) {
            Log.e("WebSocket", "Send Failed: Socket might be closed")
        }
    }

    fun sendAck(msgId: String, status: String) {
        val ackObj = mapOf("messageId" to msgId, "status" to status)
        val json = gson.toJson(ackObj)
        val frame = "SEND\ndestination:/app/chat.ack\ncontent-type:application/json\n\n$json\u0000"
        webSocket?.send(frame)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("WebSocket", "Received: $text")

        if (text.startsWith("CONNECTED")) {
            // Auto-Subscribe upon connection
            subscribe("/user/queue/messages")
            subscribe("/user/queue/ack")
        } else if (text.startsWith("MESSAGE")) {
            // Parse Body
            // We use safe substrings to avoid crashes on weird server responses
            val bodyStartIndex = text.indexOf("\n\n") + 2
            val bodyEndIndex = text.lastIndexOf("\u0000")

            if (bodyStartIndex >= 2 && bodyEndIndex > bodyStartIndex) {
                val body = text.substring(bodyStartIndex, bodyEndIndex)

                try {
                    if (body.contains("\"messageId\"") && body.contains("\"status\"")) {
                        val ack = gson.fromJson(body, MessageAck::class.java)
                        _incomingAcks.tryEmit(ack)
                    } else {
                        val msg = gson.fromJson(body, ChatMessage::class.java)
                        // FIX 2: tryEmit will now succeed because of the buffer!
                        _incomingMessages.tryEmit(msg)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Parse Error", e)
                }
            }
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("WebSocket", "Closing: $reason")
        webSocket.close(1000, null)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("WebSocket", "Error: ${t.message}")
        this.webSocket = null
    }

    fun disconnect() {
        webSocket?.close(1000, "Logout")
        webSocket = null
    }
}