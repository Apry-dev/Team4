package com.example.esnmessenger.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.esnmessenger.model.Message
import com.example.esnmessenger.ui.theme.*
import com.example.esnmessenger.viewmodel.ChatViewModel
import com.example.esnmessenger.viewmodel.RecordingState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private fun base64ToBitmap(base64: String): Bitmap? = try {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) {
    null
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun UserAvatar(
    bitmap: Bitmap?,
    name: String?,
    size: Int,
    modifier: Modifier = Modifier
) {
    val sizeDp = size.dp
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(sizeDp)
                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name?.take(1)?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size / 2.5).sp
            )
        }
    }
}

@Composable
fun ChatScreen(
    otherUserId: String,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val isUploading by chatViewModel.isUploading.collectAsState()
    val recordingState by chatViewModel.recordingState.collectAsState()
    val error by chatViewModel.error.collectAsState()
    val otherUserName by chatViewModel.otherUserName.collectAsState()
    val otherUserPhotoBase64 by chatViewModel.otherUserPhotoBase64.collectAsState()
    val otherUserIsTyping by chatViewModel.otherUserIsTyping.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val listState = rememberLazyListState()

    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.Recording) {
            val start = (recordingState as RecordingState.Recording).startTimeMs
            while (true) {
                recordingElapsedMs = System.currentTimeMillis() - start
                delay(500)
            }
        } else {
            recordingElapsedMs = 0L
        }
    }

    val otherUserBitmap = remember(otherUserPhotoBase64) {
        otherUserPhotoBase64?.let { base64ToBitmap(it) }
    }

    val imagePicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        selectedImageUri = uri
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) chatViewModel.startRecording()
    }

    LaunchedEffect(otherUserId) {
        chatViewModel.loadMessages(otherUserId)
    }

    LaunchedEffect(messages.size, otherUserIsTyping) {
        val itemCount = messages.size + (if (otherUserIsTyping) 1 else 0)
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                UserAvatar(bitmap = otherUserBitmap, name = otherUserName, size = 40)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = otherUserName ?: "Chat",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isLoading) {
            ChatSkeleton(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.fromId == currentUid,
                        otherUserBitmap = otherUserBitmap,
                        otherUserName = otherUserName
                    )
                }
                if (otherUserIsTyping) {
                    item(key = "typing_indicator") {
                        TypingBubble(
                            otherUserBitmap = otherUserBitmap,
                            otherUserName = otherUserName
                        )
                    }
                }
            }
        }

        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            when (recordingState) {
                is RecordingState.Recording -> RecordingBar(
                    elapsedMs = recordingElapsedMs,
                    onCancel = { chatViewModel.cancelRecording() },
                    onSend = { chatViewModel.stopAndSendRecording(otherUserId, recordingElapsedMs) }
                )
                is RecordingState.Uploading -> UploadingBar()
                else -> NormalInputBar(
                    inputText = inputText,
                    onInputChange = {
                        inputText = it
                        chatViewModel.updateTypingState(it.isNotBlank())
                    },
                    selectedImageUri = selectedImageUri,
                    onRemoveImage = { selectedImageUri = null },
                    isUploading = isUploading,
                    onAttach = {
                        imagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                    onMic = {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onSend = {
                        val uri = selectedImageUri
                        if (uri != null) {
                            chatViewModel.sendMessageWithImage(otherUserId, inputText, uri)
                            selectedImageUri = null
                        } else {
                            chatViewModel.sendMessage(otherUserId, inputText)
                        }
                        inputText = ""
                    },
                    otherUserId = otherUserId
                )
            }
        }
    }
}

@Composable
private fun NormalInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    selectedImageUri: Uri?,
    onRemoveImage: () -> Unit,
    isUploading: Boolean,
    onAttach: () -> Unit,
    onMic: () -> Unit,
    onSend: () -> Unit,
    otherUserId: String
) {
    val canSend = inputText.isNotBlank() || selectedImageUri != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .size(80.dp)
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove image",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttach) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach image",
                    tint = ESNCyan
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (canSend) onSend()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ESNCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (canSend && !isUploading) ESNCyan else OutlineColor,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    canSend -> IconButton(
                        onClick = onSend,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                    else -> IconButton(
                        onClick = onMic,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record voice note",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingBar(
    elapsedMs: Long,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "rec_pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "rec_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel recording",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color.Red.copy(alpha = alpha), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDuration(elapsedMs),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(48.dp)
                .background(ESNCyan, RoundedCornerShape(24.dp))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send voice note",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun UploadingBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ESNCyan, strokeWidth = 2.dp)
        Text("Sending voice note...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TypingBubble(otherUserBitmap: Bitmap?, otherUserName: String?) {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "typing_phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (otherUserBitmap != null) {
            Image(
                bitmap = otherUserBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(28.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(ESNCyan.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = otherUserName?.take(1)?.uppercase() ?: "?",
                    color = ESNCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotPhase = ((phase + index * 0.33f) % 1f)
                    val alpha = when {
                        dotPhase < 0.5f -> 0.3f + dotPhase * 1.4f
                        else -> 1f - (dotPhase - 0.5f) * 1.4f
                    }.coerceIn(0.3f, 1f)
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "chat_shimmer")
    val x by transition.animateFloat(
        initialValue = 0f, targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "chat_shimmer_x"
    )
    val shimmer = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
    )
    val brush = Brush.linearGradient(shimmer,
        start = androidx.compose.ui.geometry.Offset(x - 400f, 0f),
        end = androidx.compose.ui.geometry.Offset(x, 0f))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(28.dp).background(brush, CircleShape))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.fillMaxWidth(0.55f).height(40.dp).background(brush, RoundedCornerShape(16.dp)))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.fillMaxWidth(0.6f).height(32.dp).background(brush, RoundedCornerShape(16.dp)))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(28.dp).background(brush, CircleShape))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.fillMaxWidth(0.45f).height(32.dp).background(brush, RoundedCornerShape(16.dp)))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.fillMaxWidth(0.7f).height(48.dp).background(brush, RoundedCornerShape(16.dp)))
        }
    }
}

@Composable
private fun VoiceNoteBubble(audioUrl: String, durationMs: Long, isMine: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    val totalMs = if (durationMs > 0) durationMs else 1L

    val player = remember {
        MediaPlayer().apply {
            setOnPreparedListener { isPrepared = true }
            setOnCompletionListener {
                isPlaying = false
                positionMs = 0L
                seekTo(0)
            }
            setOnErrorListener { _, _, _ -> isPlaying = false; false }
        }
    }

    LaunchedEffect(audioUrl) {
        try {
            player.setDataSource(audioUrl)
            player.prepareAsync()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = player.currentPosition.toLong()
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    val progress = (positionMs.toFloat() / totalMs).coerceIn(0f, 1f)
    val displayDuration = if (isPlaying) formatDuration(positionMs) else formatDuration(totalMs)
    val accentColor = if (isMine) Color.White else ESNCyan
    val trackColor = if (isMine) Color.White.copy(alpha = 0.3f) else ESNCyan.copy(alpha = 0.2f)
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(min = 180.dp, max = 240.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        IconButton(
            onClick = {
                if (!isPrepared) return@IconButton
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.start()
                    isPlaying = true
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = trackColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = displayDuration,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    otherUserBitmap: Bitmap?,
    otherUserName: String?
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeText = remember(message.timestamp) {
        timeFormat.format(message.timestamp.toDate())
    }
    val maxBubbleWidth = LocalConfiguration.current.screenWidthDp.dp * 0.72f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            if (otherUserBitmap != null) {
                Image(
                    bitmap = otherUserBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(ESNCyan.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = otherUserName?.take(1)?.uppercase() ?: "?",
                        color = ESNCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isMine) ESNCyan else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        )
                    )
                    .widthIn(max = maxBubbleWidth)
                    .padding(
                        horizontal = if (message.imageUrl != null || message.audioUrl != null) 0.dp else 14.dp,
                        vertical = if (message.imageUrl != null || message.audioUrl != null) 0.dp else 10.dp
                    )
            ) {
                Column {
                    if (message.audioUrl != null) {
                        VoiceNoteBubble(
                            audioUrl = message.audioUrl,
                            durationMs = message.audioDurationMs,
                            isMine = isMine
                        )
                    }
                    if (message.imageUrl != null) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Image",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .widthIn(max = maxBubbleWidth)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMine || message.text.isNotBlank()) 0.dp else 4.dp,
                                        bottomEnd = if (!isMine || message.text.isNotBlank()) 0.dp else 4.dp
                                    )
                                )
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = timeText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                if (isMine) {
                    Icon(
                        imageVector = if (message.read) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = if (message.read) "Read" else "Sent",
                        tint = if (message.read) ESNCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (isMine) {
            Spacer(Modifier.width(6.dp + 28.dp))
        }
    }
}
