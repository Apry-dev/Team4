package com.example.esnmessenger.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esnmessenger.model.Message
import com.example.esnmessenger.ui.theme.*
import com.example.esnmessenger.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

private fun base64ToBitmap(base64: String): Bitmap? = try {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (e: Exception) {
    null
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
    val error by chatViewModel.error.collectAsState()
    val otherUserName by chatViewModel.otherUserName.collectAsState()
    val otherUserPhotoBase64 by chatViewModel.otherUserPhotoBase64.collectAsState()
    val otherUserIsTyping by chatViewModel.otherUserIsTyping.collectAsState()
    val otherUserLastSeen by chatViewModel.otherUserLastSeen.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val otherUserBitmap = remember(otherUserPhotoBase64) {
        otherUserPhotoBase64?.let { base64ToBitmap(it) }
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
        // Gradient header — background drawn first so it extends behind the status bar,
        // then statusBarsPadding() pushes content (back button / avatar / title) below it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                UserAvatar(bitmap = otherUserBitmap, name = otherUserName, size = 40)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = otherUserName ?: "Chat",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val presenceText = formatPresence(otherUserLastSeen)
                    if (presenceText.isNotEmpty()) {
                        Text(
                            text = presenceText,
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Block user") },
                            onClick = { showBlockConfirm = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Report user") },
                            onClick = { showReportDialog = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                        )
                    }
                }
            }

            if (showBlockConfirm) {
                AlertDialog(
                    onDismissRequest = { showBlockConfirm = false },
                    title = { Text("Block ${otherUserName ?: "user"}?") },
                    text = { Text("They won't appear in your chats or Meet. You can unblock them later.") },
                    confirmButton = {
                        Button(
                            onClick = { chatViewModel.blockUser(otherUserId); showBlockConfirm = false; onBack() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Block") }
                    },
                    dismissButton = { TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel") } }
                )
            }

            if (showReportDialog) {
                ReportDialog(
                    name = otherUserName ?: "user",
                    onReport = { reason -> chatViewModel.reportUser(otherUserId, reason); showReportDialog = false },
                    onDismiss = { showReportDialog = false }
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

        // Input bar — navigationBarsPadding() handles the gesture nav bar when the
        // keyboard is hidden; imePadding() on the outer Column handles it when shown.
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        chatViewModel.updateTypingState(it.isNotBlank())
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...") },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            chatViewModel.sendMessage(otherUserId, inputText)
                            inputText = ""
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ESNCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            chatViewModel.sendMessage(otherUserId, inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (inputText.isNotBlank()) ESNCyan else OutlineColor,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
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
        // Incoming
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(28.dp).background(brush, CircleShape))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.fillMaxWidth(0.55f).height(40.dp).background(brush, RoundedCornerShape(16.dp)))
        }
        // Outgoing
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.fillMaxWidth(0.6f).height(32.dp).background(brush, RoundedCornerShape(16.dp)))
        }
        // Incoming
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.size(28.dp).background(brush, CircleShape))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.fillMaxWidth(0.45f).height(32.dp).background(brush, RoundedCornerShape(16.dp)))
        }
        // Outgoing
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.fillMaxWidth(0.7f).height(48.dp).background(brush, RoundedCornerShape(16.dp)))
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
    // 72% of the screen width so bubbles stay readable on both small and large phones
    val maxBubbleWidth = LocalConfiguration.current.screenWidthDp.dp * 0.72f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            // Avatar for incoming messages
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
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .widthIn(max = maxBubbleWidth)
            ) {
                Text(
                    text = message.text,
                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
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
            Spacer(Modifier.width(6.dp + 28.dp)) // balance the left-side avatar space
        }
    }
}

private fun formatPresence(lastSeen: Long): String {
    if (lastSeen == 0L) return ""
    val diff = System.currentTimeMillis() - lastSeen
    return when {
        diff < 5 * 60_000L -> "Online"
        diff < 3_600_000L -> "Last seen ${diff / 60_000}m ago"
        diff < 86_400_000L -> "Last seen ${diff / 3_600_000}h ago"
        else -> "Last seen recently"
    }
}

@Composable
private fun ReportDialog(
    name: String,
    onReport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf("Spam", "Inappropriate content", "Harassment", "Fake profile")
    var selected by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $name", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select a reason:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == reason, onClick = { selected = reason })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == reason, onClick = { selected = reason })
                        Spacer(Modifier.width(8.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
