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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.africonnect.model.Job
import com.example.africonnect.viewmodel.JobViewModel
import com.google.firebase.auth.FirebaseAuth

private val BackgroundColor = Color(0xFFF5F6FA)
private val CardColor = Color.White
private val TextPrimary = Color(0xFF0D1B2A)
private val TextSecondary = Color(0xFF6B7280)
private val TealLight = Color(0xFFE6F4F2)
private val OrangeLight = Color(0xFFFFF0E8)
private val StatusOnline = Color(0xFF22C55E)

data class StatCard(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val isFeatured: Boolean = false
)

data class TalentItem(
    val name: String,
    val role: String,
    val rating: String,
    val skills: List<String>
)

@Composable
fun ClientHomeScreen(
    onPostJob: () -> Unit,
    onProfile: () -> Unit,
    onViewApplicant: (String) -> Unit = {},
    viewModel: JobViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val jobs by viewModel.jobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName = currentUser?.email?.substringBefore("@") ?: "there"

    LaunchedEffect(Unit) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        viewModel.fetchJobsForClient(uid)
    }

    val stats = listOf(
        StatCard(
            "Active Jobs", jobs.size.toString(),
            "Jobs you have posted",
            Icons.Outlined.Work, TealColor
        ),
        StatCard(
            "Total Proposals",
            jobs.sumOf { it.applicants.size }.toString(),
            "Across all your jobs",
            Icons.Outlined.Description, OrangeColor
        ),
        StatCard(
            "Network Reach", "300",
            "Talents in your ecosystem",
            Icons.Outlined.Hub, Color.White,
            isFeatured = true
        )
    )

    val talents = listOf(
        TalentItem("Gray Hawi", "Senior UI/UX Designer", "4.1", listOf("Figma", "React")),
        TalentItem("Troey Sambu", "Backend Engineer", "4.3", listOf("Python", "Django")),
        TalentItem("Joy Abayo", "Mobile Developer", "4.8", listOf("Kotlin", "Flutter"))
    )

    Scaffold(
        containerColor = BackgroundColor,
        bottomBar = {
            ClientBottomNavBar(
                selected = "Home",
                onNavigate = { item ->
                    when (item) {
                        "Profile" -> onProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPostJob,
                containerColor = OrangeColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Post a Job", fontWeight = FontWeight.SemiBold)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // --- Top Bar ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(TealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.first().uppercaseChar().toString(),
                            color = TealColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "AfriConnect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // --- Welcome ---
            item {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(
                        "Welcome back, $userName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Here's what's happening with your projects today.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // --- Status Badge ---
            item {
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusOnline)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "System Online",
                                fontSize = 12.sp,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // --- Stat Cards ---
            items(stats) { stat ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    ClientStatCard(stat)
                }
                Spacer(Modifier.height(12.dp))
            }

            // --- Active Jobs Header ---
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Active Jobs",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    TextButton(onClick = {}) {
                        Text(
                            "View all",
                            color = TealColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // --- Job Cards ---
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TealColor)
                    }
                }
            } else if (jobs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📋", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No jobs posted yet",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "Post your first job to find African talent",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(jobs) { job ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        ClientJobCard(
                            job = job,
                            onViewApplicant = onViewApplicant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // --- Top Hired Talent Header ---
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Top Hired Talent",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {},
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // --- Talent Row ---
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(talents) { talent ->
                        ClientTalentCard(talent)
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun ClientStatCard(stat: StatCard) {
    val bgColor = if (stat.isFeatured) TealColor else CardColor
    val txtColor = if (stat.isFeatured) Color.White else TextPrimary
    val subColor = if (stat.isFeatured) Color.White.copy(alpha = 0.8f) else TextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (stat.isFeatured) 0.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stat.title, fontSize = 13.sp, color = subColor, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(stat.value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = txtColor)
                Spacer(Modifier.height(4.dp))
                Text(stat.subtitle, fontSize = 12.sp, color = subColor)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (stat.isFeatured) Color.White.copy(alpha = 0.15f)
                        else if (stat.tint == TealColor) TealLight else OrangeLight
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    stat.icon,
                    contentDescription = null,
                    tint = stat.tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ClientJobCard(
    job: Job,
    onViewApplicant: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Work,
                    contentDescription = null,
                    tint = TealColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        job.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TealLight
                    ) {
                        Text(
                            job.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealColor,
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 3.dp
                            )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    job.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰 ${job.rate}", fontSize = 12.sp, color = TextSecondary)
                    if (job.isUrgent) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = OrangeLight
                        ) {
                            Text(
                                "URGENT",
                                fontSize = 9.sp,
                                color = OrangeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }

                // Show applicants
                if (job.applicants.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "👥 ${job.applicants.size} Applicant(s)",
                        fontSize = 12.sp,
                        color = TealColor,
                        fontWeight = FontWeight.Bold
                    )
                    job.applicants.keys.forEach { freelancerUid ->
                        TextButton(
                            onClick = { onViewApplicant(freelancerUid) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "View Applicant Profile →",
                                color = TealColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                if (job.status == "hired") {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Hired: ${job.hiredFreelancerName}",
                                fontSize = 12.sp,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientTalentCard(talent: TalentItem) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(TealColor, Color(0xFF0D4F47)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White.copy(0.4f),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TealLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        talent.name.first().toString(),
                        color = TealColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        talent.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        talent.role,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    talent.skills.take(2).forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = TealLight
                        ) {
                            Text(
                                skill,
                                fontSize = 9.sp,
                                color = TealColor,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        talent.rating,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Send Message",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ClientBottomNavBar(
    selected: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.border(BorderStroke(1.dp, Color(0xFFF0F0F0)))
    ) {
        listOf(
            Triple("Home", Icons.Outlined.Home, "Home"),
            Triple("Jobs", Icons.Outlined.Work, "Jobs"),
            Triple("Messages", Icons.Outlined.ChatBubble, "Messages"),
            Triple("Profile", Icons.Outlined.Person, "Profile")
        ).forEach { (label, icon, route) ->
            NavigationBarItem(
                selected = selected == label,
                onClick = { onNavigate(route) },
                icon = {
                    Box {
                        Icon(
                            icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp)
                        )
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
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealColor,
                    selectedTextColor = TealColor,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = TealLight
                )
            )
        }
    }
}