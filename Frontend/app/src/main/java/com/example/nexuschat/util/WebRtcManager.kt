package com.example.nexuschat.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.nexuschat.data.model.SignalMessage
import com.example.nexuschat.data.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcManager @Inject constructor(
    private val repository: ChatRepository,
    private val gson: Gson
) {
    private val TAG = "WebRtcManager"
    private var peerConnection: PeerConnection? = null
    private var factory: PeerConnectionFactory? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var dataChannel: DataChannel? = null
    private val eglBase = EglBase.create()

    var onRemoteStreamReady: ((VideoTrack) -> Unit)? = null
    lateinit var context: Context

    fun attachLocalView(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        renderer.setZOrderMediaOverlay(true)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(true)
        localVideoTrack?.addSink(renderer)
    }

    fun attachRemoteView(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        renderer.setZOrderMediaOverlay(false)
        renderer.setEnableHardwareScaler(false)
        renderer.setMirror(false)
        remoteVideoTrack?.addSink(renderer)
        this.onRemoteStreamReady = { track -> track.addSink(renderer) }
    }

    fun initialize(context: Context) {
        if (factory != null) return
        this.context = context.applicationContext
        val options = PeerConnectionFactory.InitializationOptions.builder(this.context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        val factoryOptions = PeerConnectionFactory.Options().apply { disableNetworkMonitor = true }
        factory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun startCall(targetUser: String, myUsername: String, isVideo: Boolean) {
        if (factory == null) initialize(context)
        createPeerConnection(targetUser, myUsername)
        if (isVideo) setupLocalMedia()

        val init = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("nexus-files", init)

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {}, desc)
                val sdpMap = mapOf("sdp" to desc?.description, "type" to desc?.type?.canonicalForm())
                repository.sendSignal(SignalMessage("offer", myUsername, targetUser, sdpMap))
            }
        }, MediaConstraints())
    }

    fun handleSignal(signal: SignalMessage, myUsername: String) {
        val dataMap = signal.data as? Map<String, Any> ?: return
        try {
            if (signal.type == "offer") {
                if (factory == null) initialize(context)
                createPeerConnection(signal.sender, myUsername)
                setupLocalMedia()
                val sdpStr = dataMap["sdp"] as? String
                if (sdpStr != null) {
                    peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {}, SessionDescription(SessionDescription.Type.OFFER, sdpStr))
                    peerConnection?.createAnswer(object : SdpObserverAdapter() {
                        override fun onCreateSuccess(desc: SessionDescription?) {
                            peerConnection?.setLocalDescription(object : SdpObserverAdapter() {}, desc)
                            val answerMap = mapOf("sdp" to desc?.description, "type" to desc?.type?.canonicalForm())
                            repository.sendSignal(SignalMessage("answer", myUsername, signal.sender, answerMap))
                        }
                    }, MediaConstraints())
                }
            } else if (signal.type == "answer") {
                val sdpStr = dataMap["sdp"] as? String
                if (sdpStr != null) {
                    peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {}, SessionDescription(SessionDescription.Type.ANSWER, sdpStr))
                }
            } else if (signal.type == "candidate") {
                val candidateStr = dataMap["candidate"] as? String
                val sdpMid = dataMap["sdpMid"] as? String
                val sdpMLineIndex = (dataMap["sdpMLineIndex"] as? Double)?.toInt() ?: 0
                if (candidateStr != null) {
                    peerConnection?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidateStr))
                }
            } else if (signal.type == "bye") {
                endCall()
            }
        } catch (e: Exception) { Log.e(TAG, "Signal Error", e) }
    }

    fun endCall() {
        try { peerConnection?.close() } catch (e: Exception) {}
        peerConnection = null
    }

    // 👇 THIS IS THE FUNCTION THAT WAS MISSING OR UNRESOLVED
    fun sendFile(uri: android.net.Uri) {
        if (dataChannel == null) return
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) dataChannel?.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))
        } catch (e: Exception) { Log.e(TAG, "Send File Error", e) }
    }

    private fun setupLocalMedia() {
        if (localVideoTrack == null) {
            try {
                val helper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
                val capturer = createVideoCapturer()
                if (capturer != null) {
                    val videoSource = factory?.createVideoSource(capturer.isScreencast)
                    capturer.initialize(helper, context, videoSource?.capturerObserver)
                    capturer.startCapture(640, 480, 30)
                    localVideoTrack = factory?.createVideoTrack("100", videoSource)
                    localVideoTrack?.setEnabled(true)
                }
                val audioSource = factory?.createAudioSource(MediaConstraints())
                localAudioTrack = factory?.createAudioTrack("101", audioSource)
                localAudioTrack?.setEnabled(true)
            } catch (e: Exception) { Log.e(TAG, "Media Creation Failed", e) }
        }
        if (peerConnection != null) {
            try {
                if (localVideoTrack != null) peerConnection?.addTrack(localVideoTrack, listOf("ARDAMS"))
                if (localAudioTrack != null) peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))
            } catch (e: Exception) { Log.e(TAG, "Attach Track Failed", e) }
        }
    }

    private fun createPeerConnection(targetUser: String, myUsername: String) {
        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        peerConnection = factory?.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                try {
                    if (candidate != null) {
                        val candMap = mapOf("sdpMid" to candidate.sdpMid, "sdpMLineIndex" to candidate.sdpMLineIndex, "candidate" to candidate.sdp)
                        repository.sendSignal(SignalMessage("candidate", myUsername, targetUser, candMap))
                    }
                } catch (e: Exception) { Log.e(TAG, "ICE Error", e) }
            }
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                receiver?.track()?.let { track ->
                    if (track.kind() == "video") {
                        val videoTrack = track as VideoTrack
                        videoTrack.setEnabled(true)
                        remoteVideoTrack = videoTrack
                        CoroutineScope(Dispatchers.Main).launch { onRemoteStreamReady?.invoke(videoTrack) }
                    }
                }
            }
            override fun onDataChannel(dc: DataChannel?) {
                dc?.registerObserver(object : DataChannel.Observer {
                    override fun onMessage(buffer: DataChannel.Buffer) { saveReceivedFile(buffer) }
                    override fun onStateChange() {}
                    override fun onBufferedAmountChange(l: Long) {}
                })
            }
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        enumerator.deviceNames.forEach { if (enumerator.isFrontFacing(it)) return enumerator.createCapturer(it, null) }
        enumerator.deviceNames.forEach { return enumerator.createCapturer(it, null) }
        return null
    }

    private fun saveReceivedFile(buffer: DataChannel.Buffer) {
        val data = ByteArray(buffer.data.remaining())
        buffer.data.get(data)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "NexusChat_${System.currentTimeMillis()}.bin"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let { context.contentResolver.openOutputStream(it)?.use { stream -> stream.write(data) } }
                } else {
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                    FileOutputStream(file).use { it.write(data) }
                }
                CoroutineScope(Dispatchers.Main).launch { Toast.makeText(context, "File Saved!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) { Log.e(TAG, "File Save Error", e) }
        }
    }

    fun initSurfaceView(renderer: SurfaceViewRenderer) { renderer.init(eglBase.eglBaseContext, null) }

    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}