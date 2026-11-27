package com.example.africonnect.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.africonnect.data.Repo
import com.example.africonnect.model.AppNotification
import com.example.africonnect.model.JobStatus
import com.example.africonnect.navigation.Nav
import com.example.africonnect.ui.components.*
import com.example.africonnect.ui.theme.Gold
import com.example.africonnect.ui.theme.Success
import com.example.africonnect.ui.theme.Sunset
import com.example.africonnect.util.Prefs
import com.example.africonnect.util.timeAgo
import com.example.africonnect.viewmodel.AppViewModel
import com.example.africonnect.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Settings ────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(app: AppViewModel, nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me by app.me.collectAsState()
    val theme by Prefs.theme.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = { AppTopBar("Settings", nav::back) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            me?.let { u ->
                AppCard(onClick = { nav.profile(u.uid) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.avatarUrl, u.displayName, 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Pill(if (u.isClient) "Client account" else "Freelancer account", container = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }

            SettingsGroup("Account") {
                SettingsRow(Icons.Outlined.Edit, "Edit profile", onClick = nav::editProfile)
                SettingsRow(Icons.Outlined.Password, "Change password", "We'll email you a secure reset link") {
                    val email = me?.email ?: return@SettingsRow
                    scope.launch {
                        runCatching { Repo.auth.sendPasswordResetEmail(email).await() }
                            .onSuccess { snackbar.showSnackbar("Reset link sent to $email") }
                            .onFailure { snackbar.showSnackbar(AuthViewModel.friendly(it)) }
                    }
                }
                if (me?.isClient == false) {
                    SettingsSwitch(Icons.Outlined.WorkOutline, "Available for work", me?.isAvailable == true) {
                        scope.launch { runCatching { Repo.updateUser(mapOf("isAvailable" to it)) } }
                    }
                }
                SettingsSwitch(Icons.Outlined.Map, "Show me on the map", me?.showOnMap == true) { on ->
                    if (on && me?.hasLocation != true) {
                        scope.launch { snackbar.showSnackbar("Set your location in Edit profile first") }
                    } else scope.launch { runCatching { Repo.updateUser(mapOf("showOnMap" to on)) } }
                }
            }

            SettingsGroup("Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
                SegmentedTabs(
                    listOf("System", "Light", "Dark"), theme.replaceFirstChar { it.uppercase() },
                    { Prefs.setTheme(context, it.lowercase()) }, Modifier.padding(16.dp)
                )
            }

            SettingsGroup("Activity") {
                SettingsRow(Icons.Outlined.Notifications, "Notifications", onClick = nav::notifications)
                if (me?.isClient == false) {
                    SettingsRow(Icons.AutoMirrored.Outlined.Assignment, "My applications", onClick = nav::applications)
                    SettingsRow(Icons.Outlined.Bookmarks, "Saved jobs", onClick = nav::saved)
                } else {
                    SettingsRow(Icons.Outlined.PostAdd, "Post a job", onClick = { nav.postJob() })
                }
            }

            SettingsGroup("Support") {
                SettingsRow(Icons.AutoMirrored.Outlined.HelpOutline, "Help & FAQ") { info = "Help & FAQ" to HELP }
                SettingsRow(Icons.Outlined.Policy, "Privacy policy") { info = "Privacy policy" to PRIVACY }
                SettingsRow(Icons.Outlined.Description, "Terms of service") { info = "Terms of service" to TERMS }
                SettingsRow(Icons.Outlined.Info, "About AfroConnect", "Version ${appVersion(context)}") { info = "About AfroConnect" to ABOUT }
            }

            SettingsGroup("") {
                SettingsRow(Icons.AutoMirrored.Outlined.Logout, "Log out", tint = Sunset) { confirmLogout = true }
                SettingsRow(Icons.Outlined.DeleteForever, "Delete account", "Permanently remove your account and data", tint = MaterialTheme.colorScheme.error) { confirmDelete = true }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to see your jobs and messages.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; app.logout() }) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } }
        )
    }
    if (confirmDelete) DeleteAccountDialog(onDismiss = { confirmDelete = false }) { password, setError, setBusy ->
        setBusy(true)
        scope.launch {
            runCatching { Repo.deleteAccount(password) }
                .onSuccess { confirmDelete = false }
                .onFailure { setError(AuthViewModel.friendly(it)); setBusy(false) }
        }
    }
    info?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { info = null }, title = { Text(title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(body) } },
            confirmButton = { TextButton(onClick = { info = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: (String, (String?) -> Unit, (Boolean) -> Unit) -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete account?") },
        text = {
            Column {
                Text("Your profile, inbox, notifications and saved jobs will be permanently deleted. This can't be undone. Enter your password to confirm.")
                Spacer(Modifier.height(12.dp))
                AppTextField(password, { password = it; error = null }, "Password", isPassword = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password, { error = it }, { busy = it }) }, enabled = password.isNotBlank() && !busy) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Delete forever", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

private fun appVersion(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "2.0"

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(20.dp))
    if (title.isNotBlank()) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (tint == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else tint)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitch(icon: ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

private const val HELP = """How does AfroConnect work?
Clients post jobs. Freelancers browse jobs, send proposals with a cover letter and bid, and chat with clients. When a client hires someone the job moves to In progress; when the work is done the client marks it completed and leaves a review.

How do I get more jobs?
Complete your profile (photo, title, skills, rate, portfolio). Jobs that match your skills appear under "Recommended for you".

How do I appear on the map?
Go to Edit profile → Location, tap "Use my current location", then switch on "Show me on the map".

Payments
Agree on payment terms directly with your client in chat. Never share passwords or one-time codes with anyone."""

private const val PRIVACY = """We store the information you provide — your name, email, profile details, jobs, proposals, messages and optional location — in Google Firebase so the app can work.

Your profile is visible to other signed-in AfroConnect users. Your exact location is only used for the map if you turn on "Show me on the map".

Messages are visible only to you and the person you're chatting with.

You can edit your details at any time, and permanently delete your account from Settings → Delete account."""

private const val TERMS = """By using AfroConnect you agree to: provide accurate information; treat other users with respect; not post illegal, fraudulent or misleading jobs; and not use the platform to send spam.

AfroConnect connects clients and freelancers but is not a party to agreements between them. Users are responsible for the work they deliver and the payments they agree to.

Accounts that break these rules may be removed."""

private const val ABOUT = """AfroConnect connects Africa's talent with clients locally and globally.

Find remote and on-site work, hire skilled freelancers, chat in real time and build your reputation through reviews.

Made in Kenya 🇰🇪"""

// ── Notifications ───────────────────────────────────────────────────────────

@Composable
fun NotificationsScreen(app: AppViewModel, nav: Nav) {
    val list by app.notifications.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    // Mark everything read once the user has seen it.
    DisposableEffect(Unit) { onDispose { Repo.markNotificationsRead(app.notifications.value.filter { !it.read }.map { it.id }) } }

    Scaffold(topBar = {
        AppTopBar("Notifications", nav::back) {
            if (list.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Outlined.ClearAll, "Clear all") }
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (list.isEmpty()) item {
                EmptyState(Icons.Outlined.NotificationsNone, "You're all caught up", "Proposals, hires and reviews will show up here.")
            }
            items(list, key = { it.id }) { n -> NotificationRow(n) { if (n.jobId.isNotBlank()) nav.job(n.jobId) } }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false }, title = { Text("Clear all notifications?") },
            confirmButton = { TextButton(onClick = { Repo.clearNotifications(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val (icon, color) = when (n.type) {
        "proposal" -> Icons.Outlined.Description to MaterialTheme.colorScheme.primary
        "hired" -> Icons.Outlined.Celebration to Success
        "review" -> Icons.Outlined.StarOutline to Gold
        else -> Icons.Outlined.Notifications to MaterialTheme.colorScheme.primary
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(if (!n.read) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(n.title, style = MaterialTheme.typography.titleSmall)
            Text(n.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(timeAgo(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!n.read) Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(Sunset))
    }
}

// ── Saved jobs & applications (freelancer) ──────────────────────────────────

@Composable
fun SavedJobsScreen(app: AppViewModel, nav: Nav) {
    val jobs by app.jobs.collectAsState()
    val saved by app.savedIds.collectAsState()
    val applied by app.appliedIds.collectAsState()
    val list = jobs.filter { it.id in saved }
    Scaffold(topBar = { AppTopBar("Saved jobs", nav::back) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (list.isEmpty()) item { EmptyState(Icons.Outlined.BookmarkBorder, "No saved jobs", "Tap the bookmark on any job to save it for later.") }
            items(list, key = { it.id }) { j -> JobCard(j, true, j.id in applied, { nav.job(j.id) }, { app.toggleSaved(j.id) }) }
        }
    }
}

@Composable
fun ApplicationsScreen(app: AppViewModel, nav: Nav) {
    val me by app.me.collectAsState()
    val jobs by app.jobs.collectAsState()
    val applied by app.appliedIds.collectAsState()
    var tab by remember { mutableStateOf("Active") }
    val mine = jobs.filter { it.id in applied }
    val hired = mine.filter { it.hiredFreelancerId == me?.uid }
    val active = mine.filter { it.isOpen }
    val closed = mine.filter { !it.isOpen && it.hiredFreelancerId != me?.uid }
    val shown = when (tab) { "Hired" -> hired; "Closed" -> closed; else -> active }

    Scaffold(topBar = { AppTopBar("My applications", nav::back) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SegmentedTabs(listOf("Active", "Hired", "Closed"), tab, { tab = it })
                Spacer(Modifier.height(4.dp))
                Text(
                    when (tab) {
                        "Hired" -> "${hired.size} job(s) you were hired for"
                        "Closed" -> "Jobs that were filled by someone else or closed"
                        else -> "${active.size} proposal(s) waiting for a response"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (shown.isEmpty()) item {
                EmptyState(
                    Icons.AutoMirrored.Outlined.Assignment,
                    when (tab) { "Hired" -> "No hires yet"; "Closed" -> "Nothing here"; else -> "No active applications" },
                    if (tab == "Active") "Find a job you like and send a proposal." else "Keep applying — your next client is out there."
                )
            }
            items(shown, key = { it.id }) { j ->
                AppCard(onClick = { nav.job(j.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(j.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(j.postedByName.ifBlank { "Client" } + " · " + j.budget.ifBlank { "Negotiable" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        when {
                            j.hiredFreelancerId == me?.uid && j.status == JobStatus.COMPLETED -> Pill("Completed", container = Success.copy(alpha = 0.15f), content = Success)
                            j.hiredFreelancerId == me?.uid -> Pill("Hired", container = Success.copy(alpha = 0.15f), content = Success)
                            else -> StatusPill(j.status)
                        }
                    }
                    if (j.hiredFreelancerId == me?.uid) {
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(onClick = { nav.chat(j.postedBy) }, modifier = Modifier.fillMaxWidth()) { Text("Message client") }
                    }
                }
            }
        }
    }
}
