package com.example.africonnect.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.africonnect.model.Job
import com.example.africonnect.navigation.Routes
import com.example.africonnect.ui.theme.OrangeColor
import com.example.africonnect.ui.theme.TealColor
import com.example.africonnect.viewmodel.JobViewModel
import com.example.africonnect.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

private val FreelancerBgColor       = Color(0xFFF5F6FA)
private val FreelancerCardColor     = Color.White
private val FreelancerTextPrimary   = Color(0xFF0D1B2A)
private val FreelancerTextSecondary = Color(0xFF6B7280)
private val FreelancerTealLight     = Color(0xFFE6F4F2)
private val FreelancerOrangeLight   = Color(0xFFFFF0E8)

data class NearbyTalent(
    val name: String,
    val role: String,
    val isTopRated: Boolean = false,
    val avatarColor: Color = TealColor
)

data class NearbyJob(
    val title: String,
    val description: String,
    val distance: String,
    val budgetRange: String,
    val isFeatured: Boolean = false
)

@Composable
fun FreelancerHomeScreen(
    navController: NavController,
    onProfile: () -> Unit,
    onMap: () -> Unit = {},
    onJobClick: (String) -> Unit = {},
    viewModel: JobViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    isViewedByClient: Boolean,
    jobId:String? = null,
    clientUid: String? = null,
    jobViewModel: JobViewModel = viewModel
) {
    LaunchedEffect(Unit) {
        viewModel.fetchAllJobs()
    }

    val jobs       by viewModel.jobs.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    // ── NEW: observe which jobs the current user has already applied to ──
    val appliedIds by viewModel.appliedJobIds.collectAsState()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName    = currentUser?.email?.substringBefore("@") ?: "there"

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11  -> "Good morning"
        in 12..17 -> "Good afternoon"
        else      -> "Good evening"
    }

    val nearbyTalents = listOf(
        NearbyTalent("Kofi Mensah",  "UI Designer",  avatarColor = Color(0xFF2D6A4F)),
        NearbyTalent("Amara Okoro",  "React Dev",    isTopRated = true, avatarColor = Color(0xFF6B4226)),
        NearbyTalent("David Moy",    "Illustrator",  avatarColor = Color(0xFF3D5A80))
    )

    val nearbyJobs = listOf(
        NearbyJob(
            "Local Brand Identity",
            "Need a brand logo and menu design for a new cafe opening…",
            "2.4 km away", "\$450 - \$600"
        ),
        NearbyJob(
            "Mobile App UI",
            "Perform a beautiful Flutter-based redesign…",
            "3.1 km away", "\$1,200", isFeatured = true
        )
    )
    val profileUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val profileViewModel: ProfileViewModel = viewModel()
    val profile by profileViewModel.profile.collectAsState()

    Scaffold(
        containerColor = FreelancerBgColor,
        bottomBar = {
            FreelancerBottomNavBar(
                selected   = "Home",
                onNavigate = { item ->
                    when (item) {
                        "Profile"  -> onProfile()
                        "Map"      -> onMap()
                        "Messages" -> navController.navigate(Routes.MESSAGES)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = {},
                containerColor = OrangeColor,
                contentColor   = Color.White,
                shape          = CircleShape,
                modifier       = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Top Bar ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FreelancerCardColor)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF2D6A4F), TealColor))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            userName.first().uppercaseChar().toString(),
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$greeting, $userName",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp,
                            color      = FreelancerTextPrimary
                        )
                        Text(
                            "${jobs.size} jobs available today",
                            fontSize = 12.sp,
                            color    = FreelancerTextSecondary
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.NotificationsNone,
                            contentDescription = "Notifications",
                            tint               = FreelancerTextPrimary
                        )
                    }
                }
            }

            // ── Search Bar ───────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(14.dp),
                        color           = FreelancerCardColor,
                        tonalElevation  = 1.dp,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint     = FreelancerTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Search for jobs or talent…",
                                fontSize = 14.sp,
                                color    = FreelancerTextSecondary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TealColor)
                            .clickable {},
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint     = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Nearby Talent ────────────────────────────────────────────────
            item {
                SectionHeader(title = "Nearby Talent", actionLabel = "See all", onAction = onMap)
                Spacer(Modifier.height(12.dp))
            }
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(nearbyTalents) { talent -> NearbyTalentCard(talent) }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Jobs Near You ────────────────────────────────────────────────
            item {
                SectionHeader(title = "Jobs Near You", actionLabel = "Browse map", onAction = onMap)
                Spacer(Modifier.height(12.dp))
            }
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(nearbyJobs) { job -> NearbyJobCard(job) }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Recommended Jobs ─────────────────────────────────────────────
            item {
                Text(
                    "Recommended for you",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = FreelancerTextPrimary,
                    modifier   = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TealColor)
                    }
                }
            } else if (jobs.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No jobs available yet.\nCheck back soon!",
                            color     = FreelancerTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(jobs) { job ->
                    val hasApplied = job.id in appliedIds          // ── NEW ──
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        if (job.isUrgent) {
                            UrgentJobCard(
                                job        = job,
                                navController = navController,
                                hasApplied = hasApplied            // ── NEW ──
                            )
                        } else {
                            RecommendedJobCard(
                                job        = job,
                                hasApplied = hasApplied,           // ── NEW ──
                                onApply    = {
                                    // Navigate to detail — apply happens there
                                    navController.navigate(Routes.jobDetail(job.id))
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                if (isViewedByClient && jobId != null && clientUid != null) {
                    val job by jobViewModel.selectedJob.collectAsState()
                    val isAlreadyHired = job?.hiredFreelancerId == profileUid
                    var showHiredSnackbar by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                jobViewModel.hireFreelancer(
                                    jobId          = jobId,
                                    jobTitle       = job?.title ?: "",
                                    freelancerUid  = profileUid,
                                    freelancerName = profile?.name ?: "",
                                    clientUid      = clientUid,
                                    onSuccess      = { showHiredSnackbar = true },
                                    onError        = { /* optional: show toast */ }
                                )
                            },
                            enabled  = !isAlreadyHired,
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (isAlreadyHired) Color.Gray else TealColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (isAlreadyHired) "✅ Hired" else "Hire ${profile?.name ?: ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp
                            )
                        }

                        if (showHiredSnackbar) {
                            LaunchedEffect(Unit) {
                                // Optional: show a snackbar or toast
                                showHiredSnackbar = false
                            }
                        }
                    }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = FreelancerTextPrimary)
        TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
            Text(actionLabel, color = TealColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Nearby Talent Card ────────────────────────────────────────────────────────
@Composable
fun NearbyTalentCard(talent: NearbyTalent) {
    Column(modifier = Modifier.width(120.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(talent.avatarColor, talent.avatarColor.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint     = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            if (talent.isTopRated) {
                Surface(
                    shape  = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                    color  = TealColor,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    Text(
                        "TOP RATED",
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        textAlign  = TextAlign.Center
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint     = TealColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            talent.name,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = FreelancerTextPrimary,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        Text(talent.role, fontSize = 11.sp, color = FreelancerTextSecondary)
    }
}

// ── Nearby Job Card ───────────────────────────────────────────────────────────
@Composable
fun NearbyJobCard(job: NearbyJob) {
    val bgColor     = if (job.isFeatured) TealColor else FreelancerCardColor
    val txtColor    = if (job.isFeatured) Color.White else FreelancerTextPrimary
    val subColor    = if (job.isFeatured) Color.White.copy(0.8f) else FreelancerTextSecondary
    val budgetColor = if (job.isFeatured) Color.White else OrangeColor

    Card(
        modifier  = Modifier.width(220.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (job.isFeatured) Color.White.copy(0.2f) else FreelancerTealLight
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint     = if (job.isFeatured) Color.White else TealColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (job.isFeatured) Color.White.copy(0.2f) else Color(0xFFF3F4F6)
                ) {
                    Text(
                        job.distance,
                        fontSize   = 11.sp,
                        color      = if (job.isFeatured) Color.White else FreelancerTextSecondary,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(job.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = txtColor)
            Spacer(Modifier.height(4.dp))
            Text(
                job.description,
                fontSize  = 12.sp,
                color     = subColor,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(job.budgetRange, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = budgetColor)
                if (!job.isFeatured) {
                    Button(
                        onClick         = {},
                        shape           = RoundedCornerShape(50),
                        colors          = ButtonDefaults.buttonColors(containerColor = OrangeColor),
                        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier        = Modifier.height(34.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Recommended Job Card ──────────────────────────────────────────────────────
@Composable
fun RecommendedJobCard(
    job: Job,
    hasApplied: Boolean,       // ── NEW ──
    onApply: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = FreelancerCardColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Text(
                    job.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = FreelancerTextPrimary,
                    modifier   = Modifier.weight(1f)
                )
                // ── NEW: show green check badge if already applied ──
                if (hasApplied) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFE6F4F2)
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint     = TealColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Applied", fontSize = 11.sp, color = TealColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint     = FreelancerTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (job.rating > 0) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${job.rating} (${job.reviews} reviews)", fontSize = 12.sp, color = FreelancerTextSecondary)
                    Spacer(Modifier.width(12.dp))
                }
                if (job.location.isNotEmpty()) {
                    Text("📍 ${job.location}", fontSize = 12.sp, color = FreelancerTextSecondary)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                job.description,
                fontSize   = 13.sp,
                color      = FreelancerTextSecondary,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            if (job.skills.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    job.skills.take(3).forEach { skill ->
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFF3F4F6)) {
                            Text(
                                skill,
                                fontSize   = 11.sp,
                                color      = FreelancerTextSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("RATE", fontSize = 10.sp, color = FreelancerTextSecondary, fontWeight = FontWeight.SemiBold)
                    Text(job.rate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealColor)
                }
                // ── NEW: disabled state when already applied ──
                Button(
                    onClick  = { if (!hasApplied) onApply() },
                    shape    = RoundedCornerShape(50),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (hasApplied) Color(0xFFE6F4F2) else TealColor,
                        contentColor   = if (hasApplied) TealColor else Color.White
                    ),
                    enabled        = !hasApplied,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    if (hasApplied) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Applied", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else {
                        Text("View & Apply", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Urgent Job Card ───────────────────────────────────────────────────────────
@Composable
fun UrgentJobCard(
    job: Job,
    navController: NavController,
    hasApplied: Boolean        // ── NEW ──
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F0)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Surface(shape = RoundedCornerShape(50), color = OrangeColor) {
                    Text(
                        "🔥 URGENT",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                // ── NEW: applied badge ──
                if (hasApplied) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFE6F4F2)) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TealColor, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Applied", fontSize = 11.sp, color = TealColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = FreelancerTextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(job.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FreelancerTextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                job.description,
                fontSize   = 13.sp,
                color      = FreelancerTextSecondary,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFFFE0D0))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    job.budget.ifEmpty { "Negotiable" },
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = OrangeColor
                )
                Button(
                    onClick  = { if (!hasApplied) navController.navigate(Routes.jobDetail(job.id)) },
                    shape    = RoundedCornerShape(50),
                    enabled  = !hasApplied,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (hasApplied) Color(0xFFE6F4F2) else OrangeColor,
                        contentColor   = if (hasApplied) TealColor else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    if (hasApplied) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Applied", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else {
                        Text("Apply Now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Bottom Nav ────────────────────────────────────────────────────────────────
@Composable
fun FreelancerBottomNavBar(
    selected: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = FreelancerCardColor,
        tonalElevation = 0.dp,
        modifier       = Modifier.border(BorderStroke(1.dp, Color(0xFFF0F0F0)))
    ) {
        listOf(
            Triple("Home",     Icons.Outlined.Home,        "Home"),
            Triple("Jobs",     Icons.Outlined.Work,        "Jobs"),
            Triple("Map",      Icons.Outlined.Map,         "Map"),
            Triple("Messages", Icons.Outlined.ChatBubble,  "Messages"),
            Triple("Profile",  Icons.Outlined.Person,      "Profile")
        ).forEach { (label, icon, route) ->
            NavigationBarItem(
                selected = selected == label,
                onClick  = { onNavigate(route) },
                icon     = {
                    Box {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
                        if (label == "Messages") {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OrangeColor)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                },
                label  = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = TealColor,
                    selectedTextColor   = TealColor,
                    unselectedIconColor = FreelancerTextSecondary,
                    unselectedTextColor = FreelancerTextSecondary,
                    indicatorColor      = FreelancerTealLight
                )
            )
        }
    }
}