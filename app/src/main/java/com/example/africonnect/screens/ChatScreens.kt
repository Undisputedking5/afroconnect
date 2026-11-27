package com.example.africonnect.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.africonnect.data.Repo
import com.example.africonnect.model.ChatMessage
import com.example.africonnect.model.Conversation
import com.example.africonnect.model.MessageType
import com.example.africonnect.navigation.Nav
import com.example.africonnect.ui.components.*
import com.example.africonnect.ui.theme.Sunset
import com.example.africonnect.util.*
import com.example.africonnect.viewmodel.AppViewModel
import com.example.africonnect.viewmodel.ChatViewModel

// ── Inbox ───────────────────────────────────────────────────────────────────

@Composable
fun MessagesScreen(app: AppViewModel, nav: Nav) {
    val conversations by app.conversations.collectAsState()
    var query by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Conversation?>(null) }
    val shown = conversations.filter { query.isBlank() || it.otherUserName.contains(query, true) || it.lastMessage.contains(query, true) }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Messages", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp))
            if (conversations.isNotEmpty()) SearchField(query, { query = it }, "Search conversations", Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }
        if (conversations.isEmpty()) item {
            EmptyState(
                Icons.Outlined.Forum, "No conversations yet",
                "Message a client from a job post, or a freelancer from their profile, to start chatting."
            )
        }
        items(shown, key = { it.otherUserId }) { c ->
            ConversationRow(c, onClick = { nav.chat(c.otherUserId) }, onLongClick = { toDelete = c })
        }
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete conversation?") },
            text = { Text("This removes the chat with ${c.otherUserName.ifBlank { "this user" }} from your inbox. It will reappear if they message you again.") },
            confirmButton = { TextButton(onClick = { Repo.deleteConversation(c.otherUserId); toDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(c: Conversation, onClick: () -> Unit, onLongClick: () -> Unit) {
    val live by remember(c.otherUserId) { Repo.observeUser(c.otherUserId) }.collectAsState(null)
    val name = live?.displayName ?: c.otherUserName.ifBlank { "AfroConnect user" }
    val avatar = live?.avatarUrl ?: c.otherUserAvatar
    val unread = c.unreadCount > 0
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(avatar, name, 54.dp, online = live?.isOnline == true)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    inboxTime(c.lastMessageTime), style = MaterialTheme.typography.labelSmall,
                    color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val prefix = if (c.lastSenderId == Repo.uid) "You: " else ""
                Text(
                    prefix + c.lastMessage, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = Sunset) { Text("${c.unreadCount}") }
                }
            }
        }
    }
}

// ── Chat ────────────────────────────────────────────────────────────────────

private sealed interface ChatRow {
    data class Day(val label: String) : ChatRow
    data class Msg(val m: ChatMessage) : ChatRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(otherUid: String, app: AppViewModel, nav: Nav, vm: ChatViewModel = viewModel(key = "chat_$otherUid")) {
    val me by app.me.collectAsState()
    val messages by vm.messages.collectAsState()
    val loading by vm.loading.collectAsState()
    val other by vm.other.collectAsState()
    val sending by vm.sending.collectAsState()
    val error by vm.error.collectAsState()
    var text by remember { mutableStateOf("") }
    var viewing by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(otherUid) { vm.open(otherUid) }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); vm.clearError() } }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> { vm.visible = true; vm.markRead() }
                Lifecycle.Event.ON_PAUSE -> vm.visible = false
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        onDispose { vm.visible = false; lifecycle.removeObserver(obs) }
    }

    val rows = remember(messages) {
        buildList {
            var last = ""
            messages.forEach { m ->
                val d = dayLabel(m.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis())
                if (d != last) { add(ChatRow.Day(d)); last = d }
                add(ChatRow.Msg(m))
            }
        }.asReversed()
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.sendImage(me, uri)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = {
                    Row(Modifier.clickable { nav.profile(otherUid) }, verticalAlignment = Alignment.CenterVertically) {
                        Avatar(other?.avatarUrl.orEmpty(), other?.displayName ?: "", 40.dp, online = other?.isOnline == true)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(other?.displayName ?: "…", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val status = when {
                                other?.isOnline == true -> "Online"
                                (other?.lastSeen ?: 0) > 0 -> "Last seen ${timeAgo(other!!.lastSeen)}"
                                else -> other?.title.orEmpty()
                            }
                            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(Modifier.navigationBarsPadding().imePadding()) {
                    if (sending) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                        IconButton(onClick = { picker.launch("image/*") }, enabled = !sending) { Icon(Icons.Outlined.Image, "Send photo") }
                        TextField(
                            value = text, onValueChange = { text = it },
                            placeholder = { Text("Message") },
                            maxLines = 5,
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { vm.send(me, text); text = "" },
                            enabled = text.isNotBlank(), modifier = Modifier.size(52.dp)
                        ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when {
                loading -> LoadingBox()
                messages.isEmpty() -> EmptyState(
                    Icons.Outlined.WavingHand, "Say hello 👋",
                    "Start the conversation with ${other?.displayName?.substringBefore(" ") ?: "them"}. Be clear about what you need and when.",
                    Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    state = listState, reverseLayout = true, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(rows, key = { r -> if (r is ChatRow.Msg) r.m.id else "day" + (r as ChatRow.Day).label }) { r ->
                        when (r) {
                            is ChatRow.Day -> DayChip(r.label)
                            is ChatRow.Msg -> Bubble(r.m, r.m.senderId == me?.uid) { viewing = it }
                        }
                    }
                }
            }
        }
    }

    viewing?.let { url ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { viewing = null }) {
                AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                IconButton(onClick = { viewing = null }, Modifier.statusBarsPadding().align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DayChip(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Pill(label, container = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun Bubble(m: ChatMessage, mine: Boolean, onImage: (String) -> Unit) {
    if (m.type == MessageType.SYSTEM) {
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(
                m.text, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.widthIn(max = 300.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
        return
    }
    val cs = MaterialTheme.colorScheme
    val shape = if (mine) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 290.dp).clip(shape).background(if (mine) cs.primary else cs.surface)
                .padding(if (m.type == MessageType.IMAGE) 4.dp else 0.dp)
        ) {
            if (m.type == MessageType.IMAGE && m.mediaUrl.isNotBlank()) {
                AsyncImage(
                    m.mediaUrl, "Photo", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(240.dp).clip(RoundedCornerShape(16.dp)).clickable { onImage(m.mediaUrl) }
                )
            }
            if (m.text.isNotBlank()) {
                Text(
                    m.text, color = if (mine) cs.onPrimary else cs.onSurface, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp)
                )
            }
            Row(
                Modifier.align(Alignment.End).padding(start = 14.dp, end = 10.dp, bottom = 6.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(clockTime(m.timestamp), style = MaterialTheme.typography.labelSmall, color = (if (mine) cs.onPrimary else cs.onSurfaceVariant).copy(alpha = 0.75f))
                if (mine) {
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        if (m.isRead) Icons.Filled.DoneAll else Icons.Filled.Done, if (m.isRead) "Read" else "Sent",
                        Modifier.size(14.dp), tint = if (m.isRead) Color(0xFFB9F6E8) else cs.onPrimary.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
