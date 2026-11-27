package com.example.africonnect.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.africonnect.data.Repo
import com.example.africonnect.model.*
import com.example.africonnect.navigation.Nav
import com.example.africonnect.ui.components.*
import com.example.africonnect.ui.theme.Gold
import com.example.africonnect.ui.theme.Success
import com.example.africonnect.ui.theme.Sunset
import com.example.africonnect.util.*
import com.example.africonnect.viewmodel.AppViewModel
import com.example.africonnect.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// ── Job detail ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobDetailScreen(jobId: String, app: AppViewModel, nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me by app.me.collectAsState()
    val jobs by app.jobs.collectAsState()
    val loaded by app.jobsLoaded.collectAsState()
    val saved by app.savedIds.collectAsState()
    val applied by app.appliedIds.collectAsState()
    val job = jobs.firstOrNull { it.id == jobId }
    val poster by remember(job?.postedBy) { Repo.observeUser(job?.postedBy.orEmpty()) }.collectAsState(null)
    val snackbar = remember { SnackbarHostState() }

    var showApply by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun run(success: String, block: suspend () -> Unit) {
        busy = true
        scope.launch {
            runCatching { block() }
                .onSuccess { if (success.isNotBlank()) snackbar.showSnackbar(success) }
                .onFailure { snackbar.showSnackbar(AuthViewModel.friendly(it)) }
            busy = false
        }
    }

    if (job == null) {
        Scaffold(topBar = { AppTopBar("Job", nav::back) }) { p ->
            Box(Modifier.padding(p)) {
                if (!loaded) LoadingBox()
                else EmptyState(Icons.Outlined.WorkOff, "Job not available", "This job may have been removed by the client.", actionLabel = "Go back", onAction = nav::back)
            }
        }
        return
    }
    val user = me
    val isOwner = user != null && job.postedBy == user.uid
    val isFreelancer = user != null && !user.isClient
    val hasApplied = job.id in applied
    val hiredMe = user != null && job.hiredFreelancerId == user.uid

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppTopBar("Job details", nav::back) {
                IconButton(onClick = {
                    shareText(context, "${job.title}\n${job.budget}\n\n${job.description.take(300)}\n\nFind it on AfroConnect.")
                }) { Icon(Icons.Outlined.Share, "Share") }
                if (isFreelancer) {
                    val isSaved = job.id in saved
                    IconButton(onClick = { app.toggleSaved(job.id) }) {
                        Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, "Save", tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                }
                if (isOwner) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            if (job.isOpen) DropdownMenuItem({ Text("Edit job") }, { menu = false; nav.postJob(job.id) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                            if (job.isOpen) DropdownMenuItem({ Text("Close job") }, {
                                menu = false; run("Job closed") { Repo.updateJob(job.id, mapOf("status" to JobStatus.CLOSED)) }
                            }, leadingIcon = { Icon(Icons.Outlined.Block, null) })
                            if (job.status == JobStatus.CLOSED) DropdownMenuItem({ Text("Reopen job") }, {
                                menu = false; run("Job reopened") { Repo.updateJob(job.id, mapOf("status" to JobStatus.OPEN)) }
                            }, leadingIcon = { Icon(Icons.Outlined.LockOpen, null) })
                            DropdownMenuItem({ Text("Delete job") }, { menu = false; confirmDelete = true }, leadingIcon = { Icon(Icons.Outlined.Delete, null) })
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        isOwner && job.isOpen -> PrimaryButton(
                            "View proposals (${job.proposalCount})", { nav.proposals(job.id) },
                            icon = Icons.Outlined.Groups
                        )
                        isOwner && job.status == JobStatus.HIRED -> {
                            OutlinedButton(onClick = { nav.chat(job.hiredFreelancerId) }, Modifier.height(54.dp)) {
                                Icon(Icons.AutoMirrored.Outlined.Chat, null)
                            }
                            PrimaryButton("Mark as completed", { showReview = true }, Modifier.weight(1f), icon = Icons.Outlined.TaskAlt, loading = busy)
                        }
                        isOwner -> PrimaryButton(
                            if (job.status == JobStatus.COMPLETED) "Job completed" else "Job closed", {}, enabled = false
                        )
                        hiredMe -> PrimaryButton("You're hired — message client", { nav.chat(job.postedBy) }, icon = Icons.AutoMirrored.Outlined.Chat, container = Success)
                        isFreelancer && hasApplied -> {
                            OutlinedButton(onClick = { nav.chat(job.postedBy) }, Modifier.height(54.dp)) { Icon(Icons.AutoMirrored.Outlined.Chat, null) }
                            if (job.isOpen) {
                                OutlinedButton(
                                    onClick = { run("Application withdrawn") { Repo.withdrawApplication(job.id) } },
                                    modifier = Modifier.weight(1f).height(54.dp), enabled = !busy
                                ) { Text("Withdraw application") }
                            } else PrimaryButton("Applied", {}, Modifier.weight(1f), enabled = false)
                        }
                        isFreelancer && job.isOpen -> PrimaryButton("Apply now", { showApply = true }, icon = Icons.AutoMirrored.Outlined.Send)
                        user != null && !isOwner -> PrimaryButton("Message client", { nav.chat(job.postedBy) }, icon = Icons.AutoMirrored.Outlined.Chat)
                        else -> PrimaryButton("No longer accepting proposals", {}, enabled = false)
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(job.status)
                if (job.isUrgent) Pill("Urgent", container = Sunset.copy(alpha = 0.14f), content = Sunset, icon = Icons.Filled.Bolt)
                if (job.category.isNotBlank()) Pill(job.category)
            }
            Spacer(Modifier.height(12.dp))
            Text(job.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text("Posted ${timeAgo(job.postedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            AppCard {
                Row {
                    InfoCell(Icons.Outlined.Payments, if (job.budgetType == "Hourly") "Hourly rate" else "Budget", job.budget.ifBlank { "Negotiable" }, Modifier.weight(1f))
                    InfoCell(if (job.workType == "Remote") Icons.Outlined.Public else Icons.Outlined.Place, "Work type", job.workType, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row {
                    InfoCell(Icons.Outlined.Groups, "Proposals", "${job.proposalCount}", Modifier.weight(1f))
                    InfoCell(Icons.Outlined.Event, "Deadline", job.deadline.ifBlank { "Flexible" }, Modifier.weight(1f))
                }
                if (job.hiredFreelancerName.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.clickable { nav.profile(job.hiredFreelancerId) }) {
                        InfoCell(Icons.Outlined.Handshake, "Hired freelancer", job.hiredFreelancerName, Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("About this job", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(job.description.ifBlank { "No description provided." }, style = MaterialTheme.typography.bodyLarge)

            if (job.skills.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Skills required", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val mine = user?.skills?.map { it.lowercase() }?.toSet().orEmpty()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    job.skills.forEach { s ->
                        val match = s.lowercase() in mine
                        Pill(
                            s,
                            container = if (match) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            content = if (match) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                            icon = if (match) Icons.Filled.Check else null
                        )
                    }
                }
            }

            if (job.location.isNotBlank() || job.hasLocation) {
                Spacer(Modifier.height(20.dp))
                Text("Location", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                AppCard(onClick = if (job.hasLocation) ({
                    val uri = Uri.parse("geo:${job.latitude},${job.longitude}?q=${job.latitude},${job.longitude}(${Uri.encode(job.title)})")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                }) else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(job.location.ifBlank { "Pinned location" }, style = MaterialTheme.typography.bodyLarge)
                            val u = user
                            if (u != null && u.hasLocation && job.hasLocation) {
                                Text("${formatKm(distanceKm(u.latitude, u.longitude, job.latitude, job.longitude))} from you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (job.hasLocation) Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open in maps", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            poster?.let { p ->
                Spacer(Modifier.height(20.dp))
                Text("About the client", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                AppCard(onClick = { nav.profile(p.uid) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(p.avatarUrl, p.displayName, 48.dp, online = p.isOnline)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOf(p.company, p.location).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Client" },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (p.createdAt > 0) Text("Member since ${memberSince(p.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showApply && user != null) {
        ApplySheet(job, onDismiss = { showApply = false }) { cover, bid ->
            showApply = false
            run("Proposal sent! The client has been notified.") { Repo.applyToJob(job, user, cover, bid) }
        }
    }
    if (showReview && user != null) {
        ReviewDialog(job.hiredFreelancerName, onDismiss = { showReview = false }) { rating, comment ->
            showReview = false
            run("Job completed and review posted") { Repo.completeJob(job, rating, comment, user) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, null) },
            title = { Text("Delete this job?") },
            text = { Text("The job and all its proposals will be permanently removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        runCatching { Repo.deleteJob(job) }
                            .onSuccess { nav.back() }
                            .onFailure { snackbar.showSnackbar(AuthViewModel.friendly(it)) }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun InfoCell(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplySheet(job: Job, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var cover by remember { mutableStateOf("") }
    var bid by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding().verticalScroll(rememberScrollState())) {
            Text("Submit a proposal", style = MaterialTheme.typography.headlineSmall)
            Text(job.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            ErrorBanner(error)
            Spacer(Modifier.height(8.dp))
            AppTextField(
                bid, { bid = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; error = null }, "Your bid",
                placeholder = job.budget.ifBlank { "e.g. 25,000" }, prefix = "${job.currency} ",
                keyboardType = KeyboardType.Number, leadingIcon = Icons.Outlined.Payments,
                supporting = if (job.budget.isNotBlank()) "Client's budget: ${job.budget}" else null
            )
            Spacer(Modifier.height(8.dp))
            AppTextField(
                cover, { cover = it; error = null }, "Cover letter",
                placeholder = "Introduce yourself, explain why you're a great fit and how you'd approach this job…",
                minLines = 6, singleLine = false, supporting = "${cover.length} characters · at least 30 recommended"
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Send proposal", {
                when {
                    cover.trim().length < 30 -> error = "Write at least 30 characters so the client knows why you're a fit."
                    else -> onSubmit(
                        cover.trim(),
                        bid.replace(",", "").toDoubleOrNull()?.let { formatMoney(it, job.currency) } ?: bid.trim()
                    )
                }
            }, icon = Icons.AutoMirrored.Outlined.Send)
        }
    }
}

@Composable
private fun ReviewDialog(name: String, onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete job & review") },
        text = {
            Column {
                Text("How was your experience working with $name?")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { i ->
                        IconButton(onClick = { rating = i }) {
                            Icon(if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder, "$i stars", tint = Gold, modifier = Modifier.size(34.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(comment, { comment = it }, placeholder = { Text("Share a few words about their work (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(rating, comment.trim()) }) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Post / edit job ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostJobScreen(editId: String, app: AppViewModel, nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me by app.me.collectAsState()
    val existing = app.jobs.collectAsState().value.firstOrNull { it.id == editId && editId.isNotBlank() }

    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var category by remember(existing?.id) { mutableStateOf(existing?.category?.ifBlank { null } ?: JobCategories.first()) }
    var skills by remember(existing?.id) { mutableStateOf(existing?.skills.orEmpty().joinToString(", ")) }
    var amount by remember(existing?.id) { mutableStateOf(existing?.budgetAmount?.takeIf { it > 0 }?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    var currency by remember(existing?.id) { mutableStateOf(existing?.currency ?: "KES") }
    var budgetType by remember(existing?.id) { mutableStateOf(existing?.budgetType ?: "Fixed") }
    var workType by remember(existing?.id) { mutableStateOf(existing?.workType ?: "Remote") }
    var location by remember(existing?.id) { mutableStateOf(existing?.location.orEmpty()) }
    var lat by remember(existing?.id) { mutableDoubleStateOf(existing?.latitude ?: 0.0) }
    var lng by remember(existing?.id) { mutableDoubleStateOf(existing?.longitude ?: 0.0) }
    var urgent by remember(existing?.id) { mutableStateOf(existing?.isUrgent ?: false) }
    var deadline by remember(existing?.id) { mutableStateOf(existing?.deadline.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            locating = true
            scope.launch {
                val p = currentLocation(context)
                if (p != null) { lat = p.lat; lng = p.lng; if (location.isBlank() || location.contains(",").not()) location = p.label }
                else error = "Couldn't get your location. Turn on location services and try again."
                locating = false
            }
        } else error = "Location permission denied"
    }

    fun submit() {
        val amt = amount.replace(",", "").toDoubleOrNull()
        error = when {
            title.trim().length < 5 -> "Give your job a clear title (at least 5 characters)"
            description.trim().length < 20 -> "Describe the job in at least 20 characters"
            amount.isNotBlank() && (amt == null || amt <= 0) -> "Enter a valid budget"
            workType != "Remote" && location.isBlank() -> "Add a location for on-site work"
            else -> null
        }
        if (error != null) return
        val budget = amt?.let { formatMoney(it, currency) + if (budgetType == "Hourly") "/hr" else "" } ?: ""
        val data = mapOf(
            "title" to title.trim(), "description" to description.trim(), "category" to category,
            "skills" to skills.splitSkills(), "budget" to budget, "rate" to budget,
            "budgetAmount" to (amt ?: 0.0), "currency" to currency, "budgetType" to budgetType,
            "workType" to workType, "location" to location.trim(),
            "latitude" to lat, "longitude" to lng, "isUrgent" to urgent, "deadline" to deadline.trim(),
            "postedByName" to (me?.displayName ?: ""),
        )
        saving = true
        scope.launch {
            runCatching {
                if (existing != null) Repo.updateJob(existing.id, data)
                else Repo.postJob(data + mapOf("status" to JobStatus.OPEN))
            }.onSuccess { nav.back() }
                .onFailure { error = AuthViewModel.friendly(it); saving = false }
        }
    }

    Scaffold(
        topBar = { AppTopBar(if (existing != null) "Edit job" else "Post a job", nav::back) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                PrimaryButton(
                    if (existing != null) "Save changes" else "Publish job", ::submit,
                    Modifier.navigationBarsPadding().padding(16.dp), loading = saving, icon = Icons.Outlined.Publish
                )
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
            ErrorBanner(error)
            Spacer(Modifier.height(8.dp))
            FormSection("The basics")
            AppTextField(title, { title = it; error = null }, "Job title", placeholder = "e.g. Build a Flutter delivery app")
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(catMenu, { catMenu = it }) {
                OutlinedTextField(
                    category, {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catMenu) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(catMenu, { catMenu = false }) {
                    JobCategories.forEach { c -> DropdownMenuItem({ Text(c) }, { category = c; catMenu = false }) }
                }
            }
            Spacer(Modifier.height(12.dp))
            AppTextField(description, { description = it; error = null }, "Description", placeholder = "What needs to be done? Deliverables, timeline, anything important…", minLines = 6, singleLine = false)
            Spacer(Modifier.height(12.dp))
            AppTextField(skills, { skills = it }, "Skills needed", placeholder = "Kotlin, Firebase, UI Design", supporting = "Separate skills with commas")
            if (skills.splitSkills().isNotEmpty()) SkillChips(skills.splitSkills())

            FormSection("Budget")
            SegmentedTabs(listOf("Fixed", "Hourly"), budgetType, { budgetType = it })
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                var curMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { curMenu = true }, Modifier.height(56.dp).padding(top = 6.dp)) { Text(currency); Icon(Icons.Filled.ArrowDropDown, null) }
                    DropdownMenu(curMenu, { curMenu = false }) {
                        listOf("KES", "USD", "NGN", "ZAR", "GHS", "UGX", "TZS", "EUR").forEach { c ->
                            DropdownMenuItem({ Text(c) }, { currency = c; curMenu = false })
                        }
                    }
                }
                AppTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    if (budgetType == "Hourly") "Rate per hour" else "Total budget", Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal, supporting = "Leave empty if negotiable")
            }

            FormSection("Where")
            SegmentedTabs(listOf("Remote", "On-site", "Hybrid"), workType, { workType = it })
            Spacer(Modifier.height(12.dp))
            AppTextField(location, { location = it; error = null }, "Location", placeholder = "e.g. Westlands, Nairobi", leadingIcon = Icons.Outlined.Place)
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = {
                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, enabled = !locating) {
                if (locating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (lat != 0.0) "Pinned — update to my current location" else "Pin my current location (shows job on the map)")
            }

            FormSection("Extras")
            AppTextField(deadline, { deadline = it }, "Deadline (optional)", placeholder = "e.g. 30 Dec 2026 or 2 weeks", leadingIcon = Icons.Outlined.Event)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { urgent = !urgent }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, null, tint = Sunset)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mark as urgent", style = MaterialTheme.typography.titleSmall)
                    Text("Urgent jobs are highlighted to freelancers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(urgent, { urgent = it })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun FormSection(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(10.dp))
}

// ── Proposals (client) ──────────────────────────────────────────────────────

@Composable
fun ProposalsScreen(jobId: String, app: AppViewModel, nav: Nav) {
    val scope = rememberCoroutineScope()
    val me by app.me.collectAsState()
    val job = app.jobs.collectAsState().value.firstOrNull { it.id == jobId }
    val proposals by remember(jobId) {
        Repo.observeProposals(jobId).map { s -> s.children.map { Proposal.from(jobId, it) }.sortedByDescending { it.createdAt } }
    }.collectAsState(null)
    val snackbar = remember { SnackbarHostState() }
    var confirmHire by remember { mutableStateOf<Proposal?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }

    // Applicants from the old app have no proposal record — show them too.
    val legacy = remember(job, proposals) {
        val ids = proposals.orEmpty().map { it.freelancerId }.toSet()
        job?.applicants.orEmpty().filter { it !in ids }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { AppTopBar("Proposals", nav::back, subtitle = job?.title) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val list = proposals
            when {
                list == null -> item { LoadingBox() }
                list.isEmpty() && legacy.isEmpty() -> item {
                    EmptyState(Icons.Outlined.Inbox, "No proposals yet", "Freelancers who apply will show up here. Urgent jobs and clear descriptions attract more proposals.")
                }
            }
            items(list.orEmpty(), key = { it.freelancerId }) { p ->
                val hired = job?.hiredFreelancerId == p.freelancerId
                AppCard(onClick = { expanded = if (expanded == p.freelancerId) null else p.freelancerId }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(p.freelancerAvatar, p.freelancerName, 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.freelancerName.ifBlank { "Freelancer" }, style = MaterialTheme.typography.titleSmall)
                            Text(p.freelancerTitle.ifBlank { "Freelancer" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeAgo(p.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (p.bid.isNotBlank()) Text(p.bid, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            if (hired) Pill("Hired", container = Success.copy(alpha = 0.15f), content = Success, icon = Icons.Filled.Check)
                        }
                    }
                    if (p.coverLetter.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            p.coverLetter, style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (expanded == p.freelancerId) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { nav.profile(p.freelancerId) }, Modifier.weight(1f)) { Text("Profile") }
                        OutlinedButton(onClick = { nav.chat(p.freelancerId) }, Modifier.weight(1f)) { Text("Message") }
                        if (job?.isOpen == true) Button(onClick = { confirmHire = p }, Modifier.weight(1f)) { Text("Hire") }
                    }
                }
            }
            items(legacy, key = { "legacy$it" }) { uid -> LegacyApplicant(uid, job, nav) { confirmHire = it } }
        }
    }

    confirmHire?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmHire = null },
            icon = { Icon(Icons.Outlined.Handshake, null) },
            title = { Text("Hire ${p.freelancerName}?") },
            text = { Text("The job will move to In progress and stop accepting proposals. ${p.freelancerName} will be notified and a chat will open.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmHire = null
                    val j = job ?: return@TextButton
                    val m = me ?: return@TextButton
                    scope.launch {
                        runCatching { Repo.hire(j, p, m) }
                            .onSuccess { nav.chat(p.freelancerId) }
                            .onFailure { snackbar.showSnackbar(AuthViewModel.friendly(it)) }
                    }
                }) { Text("Hire") }
            },
            dismissButton = { TextButton(onClick = { confirmHire = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LegacyApplicant(uid: String, job: Job?, nav: Nav, onHire: (Proposal) -> Unit) {
    val p by remember(uid) { Repo.observeUser(uid) }.collectAsState(null)
    val u = p ?: return
    AppCard(onClick = { nav.profile(uid) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(u.avatarUrl, u.displayName, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.displayName, style = MaterialTheme.typography.titleSmall)
                Text(u.title.ifBlank { "Freelancer" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RatingRow(u.rating, u.reviews)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { nav.chat(uid) }, Modifier.weight(1f)) { Text("Message") }
            if (job?.isOpen == true) Button(onClick = {
                onHire(Proposal(job.id, uid, u.displayName, u.title, u.avatarUrl))
            }, Modifier.weight(1f)) { Text("Hire") }
        }
    }
}
