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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.africonnect.ui.theme.BackgroundColor
import com.example.africonnect.ui.theme.TealColor
import com.example.africonnect.ui.theme.OrangeColor
import com.example.africonnect.ui.theme.TextPrimary
import com.example.africonnect.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth

// ─────────────────────────────────────────────
// Local colors (settings-screen only)
// ─────────────────────────────────────────────
private val SectionLabelColor = Color(0xFF9CA3AF)
private val DividerColor      = Color(0xFFE5E7EB)
private val CardBg            = Color.White
private val DangerRed         = Color(0xFFDC2626)
private val InfoBg            = Color(0xFFEEF2FF)
private val InfoText          = Color(0xFF4B5563)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    // ── State ──────────────────────────────────
    var jobAlertsEnabled       by remember { mutableStateOf(true) }
    var messagesEnabled        by remember { mutableStateOf(true) }
    var appUpdatesEnabled      by remember { mutableStateOf(false) }
    var mapVisibilityEnabled   by remember { mutableStateOf(true) }
    var discoveryRadius        by remember { mutableStateOf(25f) }
    var workTypeExpanded       by remember { mutableStateOf(false) }
    var selectedWorkType       by remember { mutableStateOf("Hybrid") }
    val workTypeOptions        = listOf("Remote", "On-site", "Hybrid")
    var showLogoutDialog       by remember { mutableStateOf(false) }
    var showDeleteDialog       by remember { mutableStateOf(false) }

    // Avatar from local filesDir (same pattern as ProfileScreen)
    val avatarFile = remember(currentUser?.uid) {
        currentUser?.uid?.let {
            java.io.File(context.filesDir, "avatar_$it.jpg")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* more options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            // ── Profile Card ──────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(TealColor.copy(alpha = 0.15f))
                    ) {
                        if (avatarFile != null && avatarFile.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarFile)
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
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = currentUser?.displayName ?: "User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ── ACCOUNT Section ───────────────────────
            SettingsSectionLabel(title = "ACCOUNT")

            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Outlined.Person,
                    label = "Edit Profile",
                    onClick = { navController.navigate("edit_profile") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Lock,
                    label = "Change Password",
                    onClick = { /* navigate to change password */ }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    label = "Payments & Payouts",
                    onClick = { /* navigate to payments */ }
                )
            }

            // ── NOTIFICATIONS Section ─────────────────
            SettingsSectionLabel(title = "NOTIFICATIONS")

            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Outlined.WorkOutline,
                    label = "Job Alerts",
                    checked = jobAlertsEnabled,
                    onCheckedChange = { jobAlertsEnabled = it }
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = "Messages",
                    checked = messagesEnabled,
                    onCheckedChange = { messagesEnabled = it }
                )
                SettingsDivider()
                SettingsToggleRow(
                    icon = Icons.Outlined.Timelapse,
                    label = "Application Updates",
                    checked = appUpdatesEnabled,
                    onCheckedChange = { appUpdatesEnabled = it }
                )
            }

            // ── GEOMATCH PRIVACY Section ──────────────
            SettingsSectionLabel(title = "GEOMATCH PRIVACY")

            SettingsCard {
                // Map Visibility toggle
                SettingsToggleRow(
                    icon = Icons.Outlined.Map,
                    label = "Map Visibility",
                    checked = mapVisibilityEnabled,
                    onCheckedChange = { mapVisibilityEnabled = it }
                )

                SettingsDivider()

                // Discovery Radius
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Discovery Radius",
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${discoveryRadius.toInt()} km",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealColor
                        )
                    }
                    Slider(
                        value = discoveryRadius,
                        onValueChange = { discoveryRadius = it },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = TealColor,
                            activeTrackColor = TealColor,
                            inactiveTrackColor = TealColor.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1 km",   fontSize = 11.sp, color = TextSecondary)
                        Text(text = "100 km", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                SettingsDivider()

                // Work Type Preference
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BusinessCenter,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Work Type Preference",
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }

                    // Dropdown
                    ExposedDropdownMenuBox(
                        expanded = workTypeExpanded,
                        onExpandedChange = { workTypeExpanded = !workTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedWorkType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = workTypeExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealColor,
                                unfocusedBorderColor = DividerColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = workTypeExpanded,
                            onDismissRequest = { workTypeExpanded = false }
                        ) {
                            workTypeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedWorkType = option
                                        workTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Info banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(InfoBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TealColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Your exact location is never shared. We only show a generalized area to help clients find talent in their region.",
                        fontSize = 12.sp,
                        color = InfoText,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── SUPPORT Section ───────────────────────
            SettingsSectionLabel(title = "SUPPORT")

            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Outlined.HelpOutline,
                    label = "Help & FAQ",
                    trailingIcon = Icons.Outlined.OpenInNew,
                    onClick = { /* open browser */ }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.BugReport,
                    label = "Report a Bug",
                    onClick = { /* navigate to bug report */ }
                )
            }

            // ── DANGER ZONE Section ───────────────────
            SettingsSectionLabel(title = "DANGER ZONE", color = DangerRed)

            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteDialog = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Delete Account",
                            fontSize = 15.sp,
                            color = DangerRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Logout Button ─────────────────────────
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Log Out",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Version Label ─────────────────────────
            Text(
                text = "AfriConnect v2.4.0",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Logout Confirmation Dialog ─────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Delete Account Confirmation Dialog ────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account", fontWeight = FontWeight.Bold, color = DangerRed) },
            text  = {
                Text("This action is permanent and cannot be undone. All your data will be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        // TODO: call AuthViewModel.deleteAccount()
                    }
                ) {
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ─────────────────────────────────────────────
// Reusable sub-composables
// ─────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(
    title: String,
    color: Color = SectionLabelColor
) {
    Text(
        text  = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        letterSpacing = 1.sp
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.8.dp,
        color = DividerColor
    )
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    label: String,
    trailingIcon: ImageVector = Icons.Default.ChevronRight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                color = TextPrimary
            )
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                color = TextPrimary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = TealColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB)
            )
        )
    }
}