package com.example.africonnect.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.africonnect.model.ChatMessage
import com.example.africonnect.model.MessageType
import com.example.africonnect.ui.theme.*
import com.example.africonnect.viewmodel.ChatViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    navController: NavController,
    otherUserId: String,
    otherUserName: String,
    otherUserAvatar: String = "",
    chatViewModel: ChatViewModel = viewModel()
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val currentUserId = chatViewModel.currentUserId

    // Load messages when screen opens
    LaunchedEffect(otherUserId) {
        chatViewModel.loadMessages(otherUserId, otherUserName)
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            ChatTopBar(
                name      = otherUserName,
                avatarUrl = otherUserAvatar,
                isOnline  = true,
                onBack    = { navController.popBackStack() },
                onVideoCall = { },
                onMore    = { }
            )
        },
        bottomBar = {
            ChatInputBar(
                value         = messageInput,
                onValueChange = { messageInput = it },
                onSend        = {
                    if (messageInput.isNotBlank()) {
                        chatViewModel.sendMessage(messageInput)
                        messageInput = ""
                    }
                },
                onAttach = { },
                onEmoji  = { }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealColor)
            }
            return@Scaffold
        }

        if (messages.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Say hello 👋", color = TextSecondary, fontSize = 16.sp)
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Group messages by date and show separators
            val grouped = messages.groupBy { msg ->
                val cal = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }

            grouped.forEach { (_, dayMessages) ->
                item {
                    val label = formatDateLabel(dayMessages.first().timestamp)
                    DateSeparator(label = label)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(dayMessages, key = { it.id.ifEmpty { it.timestamp.toString() } }) { message ->
                    val isMe = message.senderId == currentUserId
                    MessageRow(
                        message   = message,
                        isMe      = isMe,
                        timestamp = chatViewModel.formatTimestamp(message.timestamp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun formatDateLabel(timestamp: Long): String {
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayCal = Calendar.getInstance()
    return when {
        msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
                msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) -> "TODAY"
        msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) - 1 &&
                msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) -> "YESTERDAY"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp)).uppercase()
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────
@Composable
private fun ChatTopBar(
    name: String,
    avatarUrl: String,
    isOnline: Boolean,
    onBack: () -> Unit,
    onVideoCall: () -> Unit,
    onMore: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Box(modifier = Modifier.size(42.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(TealColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarUrl).crossfade(true).build(),
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            color = TealColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .align(Alignment.BottomEnd)
                            .border(2.dp, Color.White, CircleShape)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(
                    text = if (isOnline) "Active now" else "Offline",
                    fontSize = 12.sp,
                    color = if (isOnline) Color(0xFF22C55E) else TextSecondary
                )
            }

            IconButton(onClick = onVideoCall) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = TextPrimary)
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimary)
            }
        }
    }
}

// ─── Date Separator ───────────────────────────────────────────────────────────
@Composable
private fun DateSeparator(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
        Text(
            text = label,
            fontSize = 11.sp,
            color = DateLabelColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp),
            letterSpacing = 1.5.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
    }
}

// ─── Message Row ──────────────────────────────────────────────────────────────
@Composable
private fun MessageRow(message: ChatMessage, isMe: Boolean, timestamp: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (isMe) Spacer(modifier = Modifier.width(48.dp))

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            when (message.type) {
                MessageType.TEXT  -> TextBubble(message = message, isMe = isMe)
                MessageType.FILE  -> FileBubble(message = message, isMe = isMe)
                MessageType.IMAGE -> ImageBubble(message = message, isMe = isMe)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(text = timestamp, fontSize = 11.sp, color = DateLabelColor)
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (message.isRead) TealColor else DateLabelColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (!isMe) Spacer(modifier = Modifier.width(48.dp))
    }
}

// ─── Text Bubble ──────────────────────────────────────────────────────────────
@Composable
private fun TextBubble(message: ChatMessage, isMe: Boolean) {
    val bgColor   = if (isMe) SentBubble else ReceivedBubble
    val textColor = if (isMe) Color.White else TextPrimary
    val shape = if (isMe)
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = Modifier
            .background(bgColor, shape)
            .shadow(if (isMe) 0.dp else 2.dp, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = message.text, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

// ─── File Bubble ──────────────────────────────────────────────────────────────
@Composable
private fun FileBubble(message: ChatMessage, isMe: Boolean) {
    val bgColor   = if (isMe) SentBubble else ReceivedBubble
    val textColor = if (isMe) Color.White else TextPrimary
    val subColor  = if (isMe) Color.White.copy(alpha = 0.7f) else TextSecondary
    val shape = if (isMe)
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .background(bgColor, shape)
            .shadow(if (isMe) 0.dp else 2.dp, shape)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PictureAsPdf, null, tint = OrangeColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(message.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                Text(
                    "${message.fileSize / 1024} KB • ${message.fileType}",
                    fontSize = 11.sp,
                    color = subColor
                )
            }
        }
    }
}

// ─── Image Bubble ─────────────────────────────────────────────────────────────
@Composable
private fun ImageBubble(message: ChatMessage, isMe: Boolean) {
    val shape = if (isMe)
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)

    Box(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .aspectRatio(4f / 3f)
            .clip(shape)
    ) {
        AsyncImage(
            model = message.mediaUrl,
            contentDescription = "Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─── Input Bar ────────────────────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onEmoji: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttach,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFF0F2F5), CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Attach", tint = TextSecondary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF0F2F5), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text("Type a message...", color = Color(0xFFB0B7C3), fontSize = 15.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onEmoji, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Emoji", tint = TextSecondary)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (value.isNotBlank()) TealColor else Color(0xFFB0B7C3),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}