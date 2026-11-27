package com.example.africonnect.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.africonnect.data.Repo
import com.example.africonnect.model.*
import com.example.africonnect.navigation.Nav
import com.example.africonnect.ui.components.*
import com.example.africonnect.ui.theme.Gold
import com.example.africonnect.ui.theme.Success
import com.example.africonnect.util.*
import com.example.africonnect.viewmodel.AppViewModel
import com.example.africonnect.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(uid: String, app: AppViewModel, nav: Nav, isTab: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me by app.me.collectAsState()
    val isOwn = uid == me?.uid
    val other by remember(uid, isOwn) { if (isOwn) kotlinx.coroutines.flow.flowOf(null) else Repo.observeUser(uid) }.collectAsState(null)
    val profile = if (isOwn) me else other
    val reviews by remember(uid) {
        Repo.observeReviews(uid).map { s -> s.children.map { Review.from(it) }.sortedByDescending { it.createdAt } }
    }.collectAsState(emptyList())
    val jobs by app.jobs.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var uploading by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<String?>(null) }

    fun upload(uri: Uri, portfolio: Boolean) {
        uploading = true
        scope.launch {
            runCatching {
                if (portfolio) {
                    val url = Repo.uploadImage("portfolio/${Repo.uid}/${System.currentTimeMillis()}.jpg", uri)
                    Repo.updateUser(mapOf("portfolioImages" to (me?.portfolioImages.orEmpty() + url)))
                } else {
                    val url = Repo.uploadImage("avatars/${Repo.uid}.jpg", uri)
                    Repo.updateUser(mapOf("avatarUrl" to "$url"))
                }
            }.onFailure { snackbar.showSnackbar("Upload failed: ${AuthViewModel.friendly(it)}") }
            uploading = false
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { u -> upload(u, false) } }
    val portfolioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { u -> upload(u, true) } }

    val p = profile
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0)) { pad ->
        if (p == null) { Box(Modifier.padding(pad).fillMaxSize()) { LoadingBox() }; return@Scaffold }
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())) {
            // ── Cover + avatar ──────────────────────────────────────────────
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(150.dp).background(BrandGradient))
                Row(Modifier.statusBarsPadding().fillMaxWidth().padding(4.dp)) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        if (!isTab) IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            shareText(context, "Check out ${p.displayName}${if (p.title.isNotBlank()) ", ${p.title}," else ""} on AfroConnect!")
                        }) { Icon(Icons.Outlined.Share, "Share profile") }
                        if (isOwn) IconButton(onClick = nav::settings) { Icon(Icons.Outlined.Settings, "Settings") }
                    }
                }
                Box(Modifier.padding(top = 100.dp, start = 20.dp)) {
                    Box(Modifier.border(4.dp, MaterialTheme.colorScheme.background, CircleShape).padding(4.dp)) {
                        Avatar(p.avatarUrl, p.displayName, 96.dp)
                    }
                    if (isOwn) {
                        SmallFloatingActionButton(
                            onClick = { avatarPicker.launch("image/*") },
                            modifier = Modifier.align(Alignment.BottomEnd).size(34.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            if (uploading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            else Icon(Icons.Filled.PhotoCamera, "Change photo", Modifier.size(16.dp))
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.displayName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f, false))
                    if (p.completedJobs > 0) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.Verified, null, tint = MaterialTheme.colorScheme.primary) }
                }
                val subtitle = if (p.isClient) p.company.ifBlank { "Client" } else p.title.ifBlank { "Freelancer" }
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (p.location.isNotBlank()) Pill(p.location, icon = Icons.Outlined.Place)
                    if (!p.isClient) {
                        if (p.isAvailable) Pill("Available for work", container = Success.copy(alpha = 0.14f), content = Success, icon = Icons.Filled.Circle)
                        else Pill("Not available", icon = Icons.Outlined.DoNotDisturb)
                        Pill(p.workType, icon = Icons.Outlined.Public)
                    }
                    if (p.isOnline && !isOwn) Pill("Online now", container = Success.copy(alpha = 0.14f), content = Success)
                }
                Spacer(Modifier.height(16.dp))

                // ── Actions ─────────────────────────────────────────────────
                if (isOwn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = nav::editProfile, Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit profile")
                        }
                        if (!p.isClient) OutlinedButton(onClick = nav::applications, Modifier.weight(1f).height(48.dp)) { Text("My applications") }
                        else OutlinedButton(onClick = { nav.postJob() }, Modifier.weight(1f).height(48.dp)) { Text("Post a job") }
                    }
                    Spacer(Modifier.height(12.dp))
                    ProfileNudge(p, nav)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { nav.chat(p.uid) }, Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Message")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── Stats ───────────────────────────────────────────────────
                val postedJobs = jobs.filter { it.postedBy == p.uid }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (p.isClient) {
                        StatTile("Jobs posted", "${postedJobs.size}", Icons.Outlined.WorkOutline, Modifier.weight(1f))
                        StatTile("Hires", "${postedJobs.count { it.hiredFreelancerId.isNotBlank() }}", Icons.Outlined.Handshake, Modifier.weight(1f))
                        StatTile("Joined", memberSince(p.createdAt).ifBlank { "—" }, Icons.Outlined.CalendarMonth, Modifier.weight(1f))
                    } else {
                        StatTile("Rating", if (p.reviews > 0) "%.1f".format(p.rating) else "New", Icons.Outlined.StarOutline, Modifier.weight(1f), Gold)
                        StatTile("Jobs done", "${p.completedJobs}", Icons.Outlined.TaskAlt, Modifier.weight(1f))
                        StatTile("Rate", p.hourlyRate.ifBlank { "—" }, Icons.Outlined.Payments, Modifier.weight(1f))
                    }
                }

                ProfileSection("About") {
                    Text(
                        p.about.ifBlank { if (isOwn) "Tell people about yourself — tap Edit profile." else "No bio yet." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (p.about.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (p.experience.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        MetaItem(Icons.Outlined.WorkHistory, "${p.experience} experience")
                    }
                }

                if (!p.isClient && p.skills.isNotEmpty()) ProfileSection("Skills") { SkillChips(p.skills) }

                if (!p.isClient && (p.portfolioImages.isNotEmpty() || isOwn)) {
                    ProfileSection("Portfolio", action = if (isOwn) "Add" else null, onAction = { portfolioPicker.launch("image/*") }) {
                        if (p.portfolioImages.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.large)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                    .clickable { portfolioPicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Showcase your best work", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(p.portfolioImages) { url ->
                                    Box {
                                        AsyncImage(
                                            url, "Portfolio item", contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(150.dp).clip(MaterialTheme.shapes.large)
                                                .background(MaterialTheme.colorScheme.surfaceVariant).clickable { viewing = url }
                                        )
                                        if (isOwn) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        Repo.updateUser(mapOf("portfolioImages" to p.portfolioImages.filter { it != url }))
                                                        Repo.deleteByUrl(url)
                                                    }
                                                },
                                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(30.dp)
                                                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                                            ) { Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val links = listOf(
                    Triple(Icons.Outlined.Code, "GitHub", p.github),
                    Triple(Icons.Outlined.BusinessCenter, "LinkedIn", p.linkedin),
                    Triple(Icons.Outlined.PhotoCamera, "Instagram", p.instagram),
                    Triple(Icons.Outlined.Language, "Website", p.website),
                ).filter { it.third.isNotBlank() }
                if (links.isNotEmpty()) ProfileSection("Links") {
                    links.forEach { (icon, label, url) -> LinkRow(icon, label, url) { openUrl(context, url) } }
                }

                if (p.isClient) {
                    val open = postedJobs.filter { it.isOpen }
                    if (open.isNotEmpty()) ProfileSection("Open jobs") {
                        open.forEach { j ->
                            JobCard(j, false, false, { nav.job(j.id) }, null, Modifier.padding(bottom = 10.dp))
                        }
                    }
                } else {
                    ProfileSection("Reviews (${reviews.size})") {
                        if (reviews.isEmpty()) Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        reviews.forEach { r -> ReviewCard(r) { nav.profile(r.clientId) } }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    viewing?.let { url ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { viewing = null }) {
                AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, action: String? = null, onAction: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(20.dp))
    SectionHeader(title, action = action, onAction = onAction)
    Spacer(Modifier.height(4.dp))
    Column(content = content)
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, url: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReviewCard(r: Review, onClient: () -> Unit) {
    AppCard(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row { repeat(5) { i -> Icon(if (i < r.rating) Icons.Filled.Star else Icons.Filled.StarBorder, null, Modifier.size(16.dp), tint = Gold) } }
            Spacer(Modifier.weight(1f))
            Text(timeAgo(r.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (r.comment.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text("“${r.comment}”", style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(8.dp))
        Text(
            "${r.clientName} · ${r.jobTitle}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onClient), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Edit profile ────────────────────────────────────────────────────────────

@Composable
fun EditProfileScreen(app: AppViewModel, nav: Nav) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me = app.me.collectAsState().value ?: run { LoadingBox(); return }

    var name by remember { mutableStateOf(me.name) }
    var phone by remember { mutableStateOf(me.phone) }
    var title by remember { mutableStateOf(me.title) }
    var company by remember { mutableStateOf(me.company) }
    var location by remember { mutableStateOf(me.location) }
    var about by remember { mutableStateOf(me.about) }
    var skills by remember { mutableStateOf(me.skills.joinToString(", ")) }
    var rate by remember { mutableStateOf(me.hourlyRate) }
    var experience by remember { mutableStateOf(me.experience) }
    var github by remember { mutableStateOf(me.github) }
    var linkedin by remember { mutableStateOf(me.linkedin) }
    var instagram by remember { mutableStateOf(me.instagram) }
    var website by remember { mutableStateOf(me.website) }
    var workType by remember { mutableStateOf(me.workType) }
    var available by remember { mutableStateOf(me.isAvailable) }
    var showOnMap by remember { mutableStateOf(me.showOnMap) }
    var lat by remember { mutableDoubleStateOf(me.latitude) }
    var lng by remember { mutableDoubleStateOf(me.longitude) }
    var saving by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            locating = true
            scope.launch {
                currentLocation(context)?.let { lat = it.lat; lng = it.lng; if (location.isBlank()) location = it.label }
                    ?: run { error = "Couldn't get your location. Is location turned on?" }
                locating = false
            }
        } else error = "Location permission denied"
    }

    fun save() {
        if (name.isBlank()) { error = "Name can't be empty"; return }
        if (showOnMap && lat == 0.0 && lng == 0.0) { error = "Set your location to appear on the map"; return }
        saving = true
        val fields = mutableMapOf<String, Any?>(
            "name" to name.trim(), "phone" to phone.trim(), "location" to location.trim(), "about" to about.trim(),
            "linkedin" to linkedin.trim(), "website" to website.trim(),
            "latitude" to lat, "longitude" to lng, "showOnMap" to showOnMap,
        )
        if (me.isClient) fields["company"] = company.trim()
        else fields.putAll(
            mapOf(
                "title" to title.trim(), "skills" to skills.splitSkills(), "hourlyRate" to rate.trim(),
                "experience" to experience.trim(), "github" to github.trim(), "instagram" to instagram.trim(),
                "workType" to workType, "isAvailable" to available,
            )
        )
        scope.launch {
            runCatching { Repo.updateUser(fields) }
                .onSuccess { nav.back() }
                .onFailure { error = AuthViewModel.friendly(it); saving = false }
        }
    }

    Scaffold(
        topBar = { AppTopBar("Edit profile", nav::back) { TextButton(onClick = ::save, enabled = !saving) { Text("Save") } } },
        bottomBar = {
            Surface(tonalElevation = 3.dp) { PrimaryButton("Save profile", ::save, Modifier.navigationBarsPadding().padding(16.dp), loading = saving) }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
            ErrorBanner(error)
            FormSection("Personal")
            AppTextField(name, { name = it; error = null }, "Full name", leadingIcon = Icons.Outlined.Person)
            Spacer(Modifier.height(10.dp))
            if (me.isClient) AppTextField(company, { company = it }, "Company / organisation", leadingIcon = Icons.Outlined.Business)
            else AppTextField(title, { title = it }, "Professional title", placeholder = "e.g. Android Developer", leadingIcon = Icons.Outlined.Badge)
            Spacer(Modifier.height(10.dp))
            AppTextField(phone, { phone = it }, "Phone (optional)", keyboardType = KeyboardType.Phone, leadingIcon = Icons.Outlined.Phone)
            Spacer(Modifier.height(10.dp))
            AppTextField(about, { about = it }, "About you", minLines = 4, singleLine = false,
                placeholder = if (me.isClient) "What does your company do? What kind of talent do you hire?" else "Your background, what you love working on, and what clients can expect.")

            if (!me.isClient) {
                FormSection("Work")
                AppTextField(skills, { skills = it }, "Skills", placeholder = "Kotlin, Figma, Python", supporting = "Separate with commas — used to recommend jobs to you")
                if (skills.splitSkills().isNotEmpty()) SkillChips(skills.splitSkills())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(rate, { rate = it }, "Hourly rate", Modifier.weight(1f), placeholder = "KES 1,500")
                    AppTextField(experience, { experience = it }, "Experience", Modifier.weight(1f), placeholder = "3 years")
                }
                Spacer(Modifier.height(12.dp))
                Text("Preferred work type", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                SegmentedTabs(listOf("Remote", "On-site", "Hybrid"), workType, { workType = it })
                SwitchRow("Available for work", "Show clients you're open to new jobs", available) { available = it }
            }

            FormSection("Location")
            AppTextField(location, { location = it }, "City / area", placeholder = "e.g. Kilimani, Nairobi", leadingIcon = Icons.Outlined.Place)
            TextButton(onClick = { permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, enabled = !locating) {
                if (locating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (lat != 0.0 || lng != 0.0) "Location set — update to current" else "Use my current location")
            }
            SwitchRow(
                "Show me on the map",
                if (me.isClient) "Let nearby freelancers find you" else "Let nearby clients find you for on-site work",
                showOnMap
            ) { showOnMap = it }

            FormSection("Links")
            if (!me.isClient) {
                AppTextField(github, { github = it }, "GitHub", placeholder = "github.com/username", leadingIcon = Icons.Outlined.Code)
                Spacer(Modifier.height(10.dp))
            }
            AppTextField(linkedin, { linkedin = it }, "LinkedIn", placeholder = "linkedin.com/in/username", leadingIcon = Icons.Outlined.BusinessCenter)
            Spacer(Modifier.height(10.dp))
            if (!me.isClient) {
                AppTextField(instagram, { instagram = it }, "Instagram", placeholder = "instagram.com/username", leadingIcon = Icons.Outlined.PhotoCamera)
                Spacer(Modifier.height(10.dp))
            }
            AppTextField(website, { website = it }, "Website / portfolio", placeholder = "yourname.com", leadingIcon = Icons.Outlined.Language)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked, onChange)
    }
}

