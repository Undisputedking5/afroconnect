package com.example.africonnect.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.africonnect.model.*
import com.example.africonnect.navigation.Nav
import com.example.africonnect.ui.components.*
import com.example.africonnect.ui.theme.Sunset
import com.example.africonnect.util.distanceKm
import com.example.africonnect.util.formatKm
import com.example.africonnect.viewmodel.AppViewModel
import java.util.Calendar

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun HomeHeader(me: UserProfile, app: AppViewModel, nav: Nav, subtitle: String) {
    val unread by app.unreadNotifications.collectAsState()
    LightStatusBarIcons()
    Column(
        Modifier.fillMaxWidth().background(BrandGradient)
            .statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { nav.profile(me.uid) }) { Avatar(me.avatarUrl, me.displayName, 46.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${greeting()},", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                Text(
                    me.displayName.substringBefore(" "), color = Color.White, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                if (!me.isClient) IconButton(onClick = nav::saved) { Icon(Icons.Outlined.BookmarkBorder, "Saved jobs") }
                BadgedIconButton(Icons.Outlined.Notifications, unread, "Notifications", nav::notifications)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProfileNudge(me: UserProfile, nav: Nav, modifier: Modifier = Modifier) {
    if (me.completion >= 0.85f) return
    AppCard(modifier, onClick = nav::editProfile) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { me.completion }, modifier = Modifier.size(48.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 5.dp
                )
                Text("${(me.completion * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Complete your profile", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (me.isClient) "Freelancers trust clients with complete profiles."
                    else "Complete profiles get hired up to 3× more often.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Freelancer: Discover ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(me: UserProfile, app: AppViewModel, nav: Nav) {
    val jobs by app.jobs.collectAsState()
    val loaded by app.jobsLoaded.collectAsState()
    val saved by app.savedIds.collectAsState()
    val applied by app.appliedIds.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var sort by rememberSaveable { mutableStateOf("Newest") }
    var sortMenu by remember { mutableStateOf(false) }

    val open = remember(jobs, me.uid) { jobs.filter { it.isOpen && it.postedBy != me.uid } }
    val mySkills = remember(me.skills) { me.skills.map { it.lowercase() }.toSet() }
    val recommended = remember(open, mySkills) {
        if (mySkills.isEmpty()) emptyList()
        else open.map { j -> j to j.skills.count { it.lowercase() in mySkills } }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(6).map { it.first }
    }
    fun dist(j: Job) = if (me.hasLocation && j.hasLocation) distanceKm(me.latitude, me.longitude, j.latitude, j.longitude) else null
    val filtered = remember(open, query, category, filter, sort, me.latitude) {
        open.filter { j ->
            (query.isBlank() || listOf(j.title, j.description, j.category, j.skills.joinToString(" "))
                .any { it.contains(query, ignoreCase = true) }) &&
                (category == "All" || j.category == category) &&
                when (filter) {
                    "Urgent" -> j.isUrgent
                    "Remote" -> j.workType == "Remote"
                    "On-site" -> j.workType != "Remote"
                    else -> true
                }
        }.let { list ->
            when (sort) {
                "Highest budget" -> list.sortedByDescending { it.budgetAmount }
                "Nearest" -> list.sortedBy { dist(it) ?: Double.MAX_VALUE }
                "Fewest proposals" -> list.sortedBy { it.proposalCount }
                else -> list
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { HomeHeader(me, app, nav, "${open.size} open jobs waiting for you") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                SearchField(query, { query = it }, "Search jobs, skills, categories")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("My applications", "${applied.size}", Icons.AutoMirrored.Outlined.Assignment, Modifier.weight(1f), nav::applications)
                    QuickAction("Saved jobs", "${saved.size}", Icons.Outlined.Bookmarks, Modifier.weight(1f), nav::saved)
                }
                Spacer(Modifier.height(12.dp))
                ProfileNudge(me, nav)
            }
        }
        if (recommended.isNotEmpty() && query.isBlank()) {
            item {
                SectionHeader("Recommended for you", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recommended, key = { "rec" + it.id }) { j ->
                        JobCard(
                            j, j.id in saved, j.id in applied, { nav.job(j.id) }, { app.toggleSaved(j.id) },
                            Modifier.width(300.dp), dist(j)?.let(::formatKm)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Browse jobs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { sortMenu = true }) {
                        Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(sort)
                    }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        listOf("Newest", "Highest budget", "Nearest", "Fewest proposals").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { sort = it; sortMenu = false })
                        }
                    }
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Urgent", "Remote", "On-site")) { f ->
                    FilterChip(
                        selected = filter == f, onClick = { filter = f }, label = { Text(f) },
                        leadingIcon = if (f == "Urgent") ({ Icon(Icons.Filled.Bolt, null, Modifier.size(16.dp), tint = Sunset) }) else null
                    )
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All") + JobCategories) { c ->
                    FilterChip(
                        selected = category == c, onClick = { category = c },
                        label = { Text(if (c == "All") "All categories" else c) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        when {
            !loaded -> item { LoadingBox() }
            filtered.isEmpty() -> item {
                EmptyState(
                    Icons.Outlined.SearchOff, "No jobs match",
                    if (open.isEmpty()) "No open jobs yet — check back soon." else "Try a different search or filter.",
                    actionLabel = if (open.isNotEmpty()) "Clear filters" else null,
                    onAction = { query = ""; category = "All"; filter = "All" }
                )
            }
            else -> items(filtered, key = { it.id }) { j ->
                JobCard(
                    j, j.id in saved, j.id in applied, { nav.job(j.id) }, { app.toggleSaved(j.id) },
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp), dist(j)?.let(::formatKm)
                )
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

// ── Client: Dashboard ───────────────────────────────────────────────────────

@Composable
fun ClientHomeScreen(me: UserProfile, app: AppViewModel, nav: Nav) {
    val myJobs by app.myPostedJobs.collectAsState()
    val loaded by app.jobsLoaded.collectAsState()
    val talent by app.freelancers.collectAsState()
    var tab by rememberSaveable { mutableStateOf("Active") }

    val active = myJobs.filter { it.status == JobStatus.OPEN || it.status == JobStatus.HIRED }
    val past = myJobs.filter { it.status == JobStatus.COMPLETED || it.status == JobStatus.CLOSED }
    val shown = if (tab == "Active") active else past

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.postJob() }, containerColor = Sunset, contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Post a job") }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            item { HomeHeader(me, app, nav, "Here's how your projects are doing") }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Open jobs", "${myJobs.count { it.isOpen }}", Icons.Outlined.WorkOutline, Modifier.weight(1f))
                        StatTile("Proposals", "${myJobs.sumOf { it.proposalCount }}", Icons.Outlined.Description, Modifier.weight(1f), Sunset)
                        StatTile("Hired", "${myJobs.count { it.hiredFreelancerId.isNotBlank() }}", Icons.Outlined.Handshake, Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(12.dp))
                    ProfileNudge(me, nav)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                SectionHeader("Your jobs", Modifier.padding(horizontal = 16.dp))
                SegmentedTabs(listOf("Active", "Past"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
            }
            when {
                !loaded -> item { LoadingBox() }
                shown.isEmpty() -> item {
                    EmptyState(
                        Icons.Outlined.PostAdd,
                        if (tab == "Active") "No active jobs" else "No past jobs yet",
                        if (tab == "Active") "Post a job and start receiving proposals from talented freelancers." else "Completed and closed jobs will show up here.",
                        actionLabel = if (tab == "Active") "Post your first job" else null, onAction = { nav.postJob() }
                    )
                }
                else -> items(shown, key = { it.id }) { j -> ClientJobRow(j, nav, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
            }
            if (talent.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Top talent", Modifier.padding(horizontal = 16.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(talent.take(10), key = { it.uid }) { p -> MiniTalentCard(p) { nav.profile(p.uid) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientJobRow(j: Job, nav: Nav, modifier: Modifier) {
    AppCard(modifier, onClick = { nav.job(j.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(j.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(j.budget.ifBlank { "Budget negotiable" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(j.status)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (j.hiredFreelancerName.isNotBlank()) {
                MetaItem(Icons.Outlined.Handshake, "Hired: ${j.hiredFreelancerName}", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
            } else {
                MetaItem(Icons.Outlined.Groups, "${j.proposalCount} proposal${if (j.proposalCount == 1) "" else "s"}")
                Spacer(Modifier.weight(1f))
                if (j.proposalCount > 0) {
                    FilledTonalButton(onClick = { nav.proposals(j.id) }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Review") }
                }
            }
        }
    }
}

@Composable
private fun MiniTalentCard(p: UserProfile, onClick: () -> Unit) {
    AppCard(Modifier.width(150.dp), onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(p.avatarUrl, p.displayName, 60.dp, online = p.isOnline)
            Spacer(Modifier.height(8.dp))
            Text(p.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.title.ifBlank { "Freelancer" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            RatingRow(p.rating, p.reviews)
        }
    }
}

// ── Client: Talent directory ────────────────────────────────────────────────

@Composable
fun TalentScreen(app: AppViewModel, nav: Nav) {
    val me by app.me.collectAsState()
    val all by app.freelancers.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var onlyAvailable by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf("Top rated") }

    val list = remember(all, query, onlyAvailable, sort) {
        all.filter { p ->
            (query.isBlank() || listOf(p.displayName, p.title, p.location, p.skills.joinToString(" "))
                .any { it.contains(query, true) }) && (!onlyAvailable || p.isAvailable)
        }.let { l ->
            val m = me
            when (sort) {
                "Nearest" -> if (m != null && m.hasLocation) l.sortedBy {
                    if (it.hasLocation) distanceKm(m.latitude, m.longitude, it.latitude, it.longitude) else Double.MAX_VALUE
                } else l
                "Most jobs" -> l.sortedByDescending { it.completedJobs }
                else -> l
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Find talent", style = MaterialTheme.typography.headlineMedium)
                    Text("${all.size} freelancers on AfroConnect", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                FilledTonalIconButton(onClick = nav::map) { Icon(Icons.Outlined.Map, "Map view") }
            }
            Spacer(Modifier.height(12.dp))
            SearchField(query, { query = it }, "Search by skill, name or city", Modifier.padding(horizontal = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(onlyAvailable, { onlyAvailable = !onlyAvailable }, { Text("Available now") },
                        leadingIcon = if (onlyAvailable) ({ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }) else null)
                }
                items(listOf("Top rated", "Most jobs", "Nearest")) { s ->
                    FilterChip(sort == s, { sort = s }, { Text(s) })
                }
            }
        }
        if (list.isEmpty()) item {
            EmptyState(Icons.Outlined.PersonSearch, "No freelancers found", if (all.isEmpty()) "Freelancers will appear here as they join." else "Try another skill or remove filters.")
        }
        items(list, key = { it.uid }) { p ->
            val m = me
            val d = if (m != null && m.hasLocation && p.hasLocation) formatKm(distanceKm(m.latitude, m.longitude, p.latitude, p.longitude)) else null
            TalentCard(p, { nav.profile(p.uid) }, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), d)
        }
    }
}
