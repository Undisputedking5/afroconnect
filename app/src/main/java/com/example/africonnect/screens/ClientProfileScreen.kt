package com.example.africonnect.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.africonnect.model.Job
import com.example.africonnect.ui.theme.*
import com.example.africonnect.viewmodel.JobViewModel
import com.example.africonnect.viewmodel.ProfileViewModel

private val CardBg         = Color.White
private val DividerColor   = Color(0xFFE5E7EB)
private val StatLabelColor = Color(0xFF9CA3AF)
private val ActiveGreen    = Color(0xFF16A34A)
private val LinkedInColor  = Color(0xFF0A66C2)
private val DangerRed      = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    navController: NavController,
    uid: String,
    isOwnProfile: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = viewModel()
    val jobViewModel: JobViewModel         = viewModel()

    val profile    by profileViewModel.profile.collectAsState()
    val isLoading  by profileViewModel.isLoading.collectAsState()
    val jobs       by jobViewModel.jobs.collectAsState()

    var isFollowing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Load data on first composition
    LaunchedEffect(uid) {
        profileViewModel.loadProfile(uid)
        jobViewModel.fetchJobsForClient(uid)
    }

    // ── Logout confirmation dialog ────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?", fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to log out of AfriConnect?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout?.invoke()
                    }
                ) {
                    Text("Log out", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TealColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp).align(Alignment.Center)
                            )
                        }
                        Text(
                            text = "AfriConnect",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        // Edit button in top bar
                        IconButton(onClick = { onEdit?.invoke() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = TealColor
                            )
                        }
                        // Logout button in top bar
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = DangerRed
                            )
                        }
                    } else {
                        IconButton(onClick = { /* notifications */ }) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { innerPadding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TealColor)
            }
            return@Scaffold
        }

        // If profile is null (new user), show empty state with setup prompt
        val currentProfile = profile

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Hero Section ──────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, TealColor, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(TealColor.copy(alpha = 0.15f))
                    ) {
                        if (!currentProfile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentProfile!!.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TealColor,
                                modifier = Modifier.size(52.dp).align(Alignment.Center)
                            )
                        }
                    }
                    if (isOwnProfile) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(TealColor)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { onEdit?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit avatar",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name — fallback to "Set up your profile" if empty
                Text(
                    text = if (currentProfile?.name.isNullOrBlank()) "Set up your profile" else currentProfile!!.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentProfile?.name.isNullOrBlank()) TextSecondary else TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Company + Location
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!currentProfile?.company.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.Business,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentProfile!!.company,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    if (!currentProfile?.location.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentProfile!!.location,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Prompt to complete profile if own and empty
                if (isOwnProfile && currentProfile?.name.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onEdit?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = TealColor),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Complete your profile", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Follow + Contact (only when viewing someone else's profile)
                if (!isOwnProfile) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isFollowing = !isFollowing },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) Color.White else TealColor,
                                contentColor   = if (isFollowing) TealColor   else Color.White
                            ),
                            border = if (isFollowing)
                                androidx.compose.foundation.BorderStroke(1.5.dp, TealColor)
                            else null
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val name = currentProfile?.name ?: ""
                                navController.navigate("chat/$uid/$name")
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, DividerColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Contact", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                // Edit + Logout buttons for own profile
                if (isOwnProfile) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onEdit?.invoke() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, TealColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, DangerRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log out", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Stats Row ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "PROJECTS POSTED",
                    value = currentProfile?.projectsPosted
                        ?.takeIf { it > 0 }?.toString() ?: "—"
                )
                StatCard(
                    label      = "TOTAL SPENT",
                    value      = currentProfile?.totalSpent
                        ?.takeIf { it.isNotBlank() } ?: "—",
                    valueColor = TealColor
                )
                StatCard(
                    label         = "MEMBER SINCE",
                    value         = currentProfile?.memberSince
                        ?.takeIf { it.isNotBlank() } ?: "—",
                    valueColor    = TealColor,
                    valueFontSize = 28
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── About ─────────────────────────────────────────────────────────
            if (!currentProfile?.about.isNullOrBlank()) {
                ProfileSection(icon = Icons.Outlined.Info, title = "About") {
                    Text(
                        text       = currentProfile!!.about,
                        fontSize   = 14.sp,
                        color      = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Contact Info ──────────────────────────────────────────────────
            ProfileSection(icon = Icons.Outlined.PermContactCalendar, title = "Contact Info") {
                Card(
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (!currentProfile?.email.isNullOrBlank()) {
                            ContactRow(
                                icon  = Icons.Outlined.Email,
                                label = "Email",
                                value = currentProfile!!.email
                            )
                            HorizontalDivider(
                                modifier  = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.8.dp,
                                color     = DividerColor
                            )
                        }
                        if (!currentProfile?.linkedin.isNullOrBlank()) {
                            ContactRow(
                                icon       = Icons.Outlined.Link,
                                label      = "LinkedIn",
                                value      = currentProfile!!.linkedin,
                                valueColor = LinkedInColor
                            )
                        }
                        if (currentProfile?.email.isNullOrBlank() && currentProfile?.linkedin.isNullOrBlank()) {
                            Text(
                                text     = if (isOwnProfile) "Tap Edit Profile to add contact info" else "No contact info",
                                fontSize = 13.sp,
                                color    = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Active Postings ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Active Postings",
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    if (jobs.isNotEmpty()) {
                        Text(
                            text     = "View all",
                            fontSize = 13.sp,
                            color    = TealColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (jobs.isEmpty()) {
                    Text(
                        text     = if (isOwnProfile) "You haven't posted any jobs yet." else "No active postings.",
                        fontSize = 13.sp,
                        color    = TextSecondary
                    )
                } else {
                    jobs.take(3).forEach { job ->
                        ActiveJobCard(
                            job     = job,
                            onClick = { navController.navigate("job_detail/${job.id}") }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    valueFontSize: Int = 32
) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text          = label,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = StatLabelColor,
                letterSpacing = 1.sp
            )
            Text(
                text       = value,
                fontSize   = valueFontSize.sp,
                fontWeight = FontWeight.Bold,
                color      = valueColor
            )
        }
    }
}

// ── Section Wrapper ───────────────────────────────────────────────────────────
@Composable
private fun ProfileSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector    = icon,
                contentDescription = null,
                tint           = TextSecondary,
                modifier       = Modifier.size(20.dp)
            )
            Text(
                text       = title,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
        }
        content()
    }
}

// ── Contact Row ───────────────────────────────────────────────────────────────
@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector    = icon,
            contentDescription = null,
            tint           = TextSecondary,
            modifier       = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, fontSize = 11.sp, color = StatLabelColor)
            Text(text = value, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Active Job Card ───────────────────────────────────────────────────────────
@Composable
private fun ActiveJobCard(job: Job, onClick: () -> Unit) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(90.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(TealColor, TealColor.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )
            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = job.title,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        modifier   = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ActiveGreen)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text          = "ACTIVE",
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(text = job.description, fontSize = 12.sp, color = TextSecondary)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector    = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint           = TextSecondary,
                            modifier       = Modifier.size(14.dp)
                        )
                        Text(
                            text       = job.budget.ifBlank { job.rate.ifBlank { "—" } },
                            fontSize   = 13.sp,
                            color      = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text       = "View →",
                        fontSize   = 13.sp,
                        color      = TealColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}