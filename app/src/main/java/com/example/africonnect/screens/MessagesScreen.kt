package com.example.africonnect.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.africonnect.model.Conversation
import com.example.africonnect.navigation.Routes
import com.example.africonnect.ui.theme.BackgroundColor
import com.example.africonnect.ui.theme.OrangeColor
import com.example.africonnect.ui.theme.TealColor
import com.example.africonnect.ui.theme.TextPrimary
import com.example.africonnect.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
    navController: NavController,
    currentUserId: String = ""
) {
    val auth = FirebaseAuth.getInstance()
    val db   = FirebaseDatabase.getInstance().reference
    val uid  = currentUserId.ifEmpty { auth.currentUser?.uid ?: "" }

    var searchQuery       by remember { mutableStateOf("") }
    var selectedTab       by remember { mutableStateOf(0) }
    var conversations     by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var currentUserName   by remember { mutableStateOf("AfriConnect") }
    var currentUserAvatar by remember { mutableStateOf("") }

    // Load current user info
    LaunchedEffect(uid) {
        if (uid.isEmpty()) return@LaunchedEffect
        db.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUserName   = snapshot.child("name").getValue(String::class.java) ?: "AfriConnect"
                    currentUserAvatar = snapshot.child("avatarUrl").getValue(String::class.java) ?: ""
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Load conversations from Firebase
    LaunchedEffect(uid) {
        if (uid.isEmpty()) return@LaunchedEffect
        db.child("conversations").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val convList = mutableListOf<Conversation>()
                    var pending  = snapshot.childrenCount.toInt()
                    if (pending == 0) { conversations = emptyList(); return }

                    snapshot.children.forEach { convSnap ->
                        val otherId     = convSnap.key ?: return@forEach
                        val lastMsg     = convSnap.child("lastMessage").getValue(String::class.java) ?: ""
                        val lastTime    = convSnap.child("lastMessageTime").getValue(Long::class.java) ?: 0L
                        val unread      = convSnap.child("unreadCount").getValue(Int::class.java) ?: 0
                        val isPhoto     = convSnap.child("isPhoto").getValue(Boolean::class.java) ?: false
                        val cachedName  = convSnap.child("otherUserName").getValue(String::class.java) ?: ""
                        val cachedAvatar = convSnap.child("otherUserAvatar").getValue(String::class.java) ?: ""

                        // Fetch live user info for name/avatar/online status
                        db.child("users").child(otherId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    val name   = userSnap.child("name").getValue(String::class.java) ?: cachedName
                                    val avatar = userSnap.child("avatarUrl").getValue(String::class.java) ?: cachedAvatar
                                    val online = userSnap.child("isOnline").getValue(Boolean::class.java) ?: false
                                    convList.add(
                                        Conversation(
                                            uid             = otherId,
                                            otherUserId     = otherId,
                                            otherUserName   = name,
                                            otherUserAvatar = avatar,
                                            lastMessage     = lastMsg,
                                            lastMessageTime = lastTime,
                                            unreadCount     = unread,
                                            isOnline        = online,
                                            isPhoto         = isPhoto
                                        )
                                    )
                                    pending--
                                    if (pending <= 0) {
                                        conversations = convList.sortedByDescending { it.lastMessageTime }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) { pending-- }
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    val tabs = listOf("All Messages", "Recruiters", "Freelancers")
    val filtered = conversations.filter { conv ->
        searchQuery.isBlank() ||
                conv.otherUserName.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { },
                containerColor = OrangeColor,
                shape          = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New Message", tint = Color.White)
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // — Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TealColor)
                ) {
                    if (currentUserAvatar.isNotEmpty()) {
                        AsyncImage(
                            model              = currentUserAvatar,
                            contentDescription = null,
                            modifier           = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                currentUserName.firstOrNull()?.toString() ?: "A",
                                color      = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = currentUserName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = TextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint               = TextPrimary,
                    modifier           = Modifier.size(24.dp)
                )
            }

            // — Search Bar
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search conversations...", color = TextSecondary) },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor    = Color.Transparent,
                    focusedBorderColor      = TealColor,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor   = Color.White
                ),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // — Tabs
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .background(
                                if (isSelected) TealColor else Color.White,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text       = label,
                            color      = if (isSelected) Color.White else TextSecondary,
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // — Conversation List
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No conversations yet", color = TextSecondary)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(filtered, key = { it.uid }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick      = {
                                navController.navigate(
                                    Routes.chatRoute(
                                        otherUserId = conv.otherUserId,
                                        userName    = conv.otherUserName
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar + online dot
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier         = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(TealColor),
                contentAlignment = Alignment.Center
            ) {
                if (conversation.otherUserAvatar.isNotEmpty()) {
                    AsyncImage(
                        model              = conversation.otherUserAvatar,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    val initials = conversation.otherUserName
                        .split(" ").take(2)
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("")
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            // Online indicator
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .background(
                        if (conversation.isOnline) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                        CircleShape
                    )
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = conversation.otherUserName,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conversation.isPhoto) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = null,
                        tint               = TextSecondary,
                        modifier           = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    text     = if (conversation.isPhoto) "Sent a photo" else conversation.lastMessage,
                    color    = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text     = formatTime(conversation.lastMessageTime),
                color    = if (conversation.unreadCount > 0) OrangeColor else TextSecondary,
                fontSize = 12.sp
            )
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(20.dp)
                        .background(OrangeColor, CircleShape)
                ) {
                    Text(
                        text       = conversation.unreadCount.toString(),
                        color      = Color.White,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now  = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60 * 60 * 1000       -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        diff < 24 * 60 * 60 * 1000  -> "Yesterday"
        else                         -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}