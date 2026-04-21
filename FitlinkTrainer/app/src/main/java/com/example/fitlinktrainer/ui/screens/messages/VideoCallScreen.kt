package com.example.fitlinktrainer.ui.screens.messages

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.fitlinktrainer.R
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas

@Composable
fun VideoCallScreen(
    channelName: String,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val appId = "dfc43e7c71ee44ef9033ebdba28ee289"

    var engine by remember { mutableStateOf<RtcEngine?>(null) }
    var localView by remember { mutableStateOf<View?>(null) }
    var remoteView by remember { mutableStateOf<View?>(null) }

    var isJoined by remember { mutableStateOf(false) }
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }

    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted =
            result[Manifest.permission.CAMERA] == true &&
                    result[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Initialize Agora engine only once
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && engine == null) {
            val handler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    isJoined = true
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    remoteUid = uid

                    if (remoteView == null) {
                        remoteView = RtcEngine.CreateRendererView(context)
                    }

                    engine?.setupRemoteVideo(
                        VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_FIT, uid)
                    )
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    if (remoteUid == uid) {
                        remoteUid = null
                        remoteView = null
                    }
                }
            }

            engine = RtcEngine.create(context, appId, handler).apply {
                enableVideo()
                enableAudio()
                setDefaultAudioRoutetoSpeakerphone(true)
                setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
                setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

                if (localView == null) {
                    localView = RtcEngine.CreateRendererView(context)
                }

                setupLocalVideo(
                    VideoCanvas(localView, VideoCanvas.RENDER_MODE_FIT, 0)
                )

                startPreview()

                val options = ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                    publishCameraTrack = true
                    publishMicrophoneTrack = true
                    autoSubscribeAudio = true
                    autoSubscribeVideo = true
                }

                joinChannel(null, channelName, 0, options)
            }
        }
    }

    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                engine?.leaveChannel()
                engine?.stopPreview()
                RtcEngine.destroy()
                engine = null
                localView = null
                remoteView = null
                remoteUid = null
                isJoined = false
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
    ) {
        when {
            !permissionsGranted -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Allow camera and mic permissions", color = Color.White)
                }
            }

            remoteView != null -> {
                AndroidView(
                    factory = { remoteView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }

            localView != null -> {
                AndroidView(
                    factory = { localView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isJoined) "Waiting for other person..." else "Joining call...",
                        color = Color.White
                    )
                }
            }
        }

        if (localView != null && remoteView != null) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .size(width = 120.dp, height = 170.dp)
                    .align(Alignment.TopEnd),
                shape = RoundedCornerShape(20.dp)
            ) {
                AndroidView(
                    factory = { localView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.Black.copy(alpha = 0.35f)
        ) {
            Text(
                text = if (remoteUid != null) "Connected" else "Calling...",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = {
                        isMuted = !isMuted
                        engine?.muteLocalAudioStream(isMuted)
                    },
                    containerColor = Color(0xFF1F2937)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
                        ),
                        contentDescription = "Mic",
                        tint = Color.White
                    )
                }

                FloatingActionButton(
                    onClick = {
                        isCameraOff = !isCameraOff
                        engine?.muteLocalVideoStream(isCameraOff)
                    },
                    containerColor = Color(0xFF1F2937)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isCameraOff) R.drawable.ic_video_camera_off else R.drawable.ic_video_camera_on
                        ),
                        contentDescription = "Camera",
                        tint = Color.White
                    )
                }

                FloatingActionButton(
                    onClick = { engine?.switchCamera() },
                    containerColor = Color(0xFF1F2937)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera_switch),
                        contentDescription = "Switch camera",
                        tint = Color.White
                    )
                }

                FloatingActionButton(
                    onClick = onEndCall,
                    containerColor = Color(0xFFDC2626)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call_end),
                        contentDescription = "End call",
                        tint = Color.White
                    )
                }
            }
        }
    }
}