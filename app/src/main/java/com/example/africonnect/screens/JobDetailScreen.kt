package com.example.africonnect.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.africonnect.model.Job
import com.example.africonnect.ui.theme.OrangeColor
import com.example.africonnect.ui.theme.TealColor
import com.example.africonnect.viewmodel.JobViewModel

// ── Colours ───────────────────────────────────────────────────────────────────
private val JD_BgColor       = Color(0xFFF5F6FA)
private val JD_CardColor     = Color.White
private val JD_TextPrimary   = Color(0xFF0D1B2A)
private val JD_TextSecondary = Color(0xFF6B7280)
private val JD_TealLight     = Color(0xFFE6F4F2)
private val JD_ChipOutline   = Color(0xFFD1D5DB)
private val JD_InfoBg        = Color(0xFFF0F4FF)

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    navController: NavController,
    jobViewModel: JobViewModel,
    jobId: String
) {
    LaunchedEffect(jobId) {
        jobViewModel.getJobById(jobId)
        // ── NEW: also load which jobs this user has already applied to ──
        jobViewModel.loadAppliedJobs()
    }

    val selectedJob  by jobViewModel.selectedJob.collectAsState()
    val isLoading    by jobViewModel.isLoading.collectAsState()
    val applySuccess by jobViewModel.applySuccess.collectAsState()
    val applyError   by jobViewModel.applyError.collectAsState()
    // ── NEW ──
    val appliedIds   by jobViewModel.appliedJobIds.collectAsState()
    val hasApplied   = jobId in appliedIds

    // ── Snackbar for error feedback ──
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(applyError) {
        applyError?.let {
            snackbarHostState.showSnackbar(it)
            jobViewModel.resetApplyError()
        }
    }

    // ── Navigate back only on success AND this job wasn't already applied ──
    // (we no longer pop back — we stay so the user sees the "Applied" state)
    LaunchedEffect(applySuccess) {
        if (applySuccess) {
            jobViewModel.resetApplySuccess()
            // Optional: pop back. Comment out if you want to stay on detail screen.
            // navController.popBackStack()
        }
    }

    if (isLoading || selectedJob == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealColor)
        }
        return
    }

    val job            = selectedJob!!
    val selectedSkills = remember(job.id) { mutableStateListOf<String>() }

    Scaffold(
        containerColor  = JD_BgColor,
        snackbarHost    = { SnackbarHost(snackbarHostState) },   // ── NEW ──
        topBar          = {
            JobDetailTopBar(onBack = { navController.popBackStack() })
        },
        bottomBar       = {
            // ── NEW: pass hasApplied so the bar reflects state correctly ──
            JobDetailBottomBar(
                hasApplied = hasApplied,
                onApply    = {
                    jobViewModel.applyForJob(
                        jobId     = jobId,
                        onSuccess = {},
                        onError   = {}
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderCard(job = job, hasApplied = hasApplied)    // ── NEW: pass hasApplied ──
            Spacer(Modifier.height(12.dp))
            SectionCard {
                Text("Job Description", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealColor)
                Spacer(Modifier.height(10.dp))
                Text(job.description, fontSize = 13.sp, color = JD_TextSecondary, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            SectionCard {
                Text("Required Skills", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealColor)
                Spacer(Modifier.height(12.dp))
                FlowRow(skills = job.skills, selectedSkills = selectedSkills)
            }
            Spacer(Modifier.height(12.dp))
            InfoCard(job = job)
            Spacer(Modifier.height(12.dp))
            LocationCard(locationLabel = job.location)
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobDetailTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text("Job Detail", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = JD_TextPrimary)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = JD_TextPrimary)
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifications", tint = JD_TextPrimary)
            }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(JD_TealLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TealColor, modifier = Modifier.size(20.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = JD_CardColor)
    )
}

// ── Header Card ───────────────────────────────────────────────────────────────
@Composable
private fun HeaderCard(job: Job, hasApplied: Boolean) {       // ── NEW: hasApplied ──
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = JD_CardColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(JD_TealLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.BusinessCenter,
                        contentDescription = null,
                        tint     = TealColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
                // ── NEW: applied banner in header ──
                if (hasApplied) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFE6F4F2)
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TealColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "You've applied",
                                fontSize   = 13.sp,
                                color      = TealColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JD_TextPrimary)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Verified, contentDescription = null, tint = TealColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${job.postedBy} • ${job.locationLabel}",
                fontSize = 13.sp,
                color    = JD_TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip(label = job.category.ifEmpty { "Full-Time" })
                TypeChip(label = job.workType.ifEmpty { "Remote Optional" })
            }
        }
    }
}

@Composable
private fun TypeChip(label: String) {
    Surface(
        shape  = RoundedCornerShape(50),
        color  = Color.Transparent,
        border = BorderStroke(1.dp, JD_ChipOutline)
    ) {
        Text(
            label.uppercase(),
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color      = JD_TextSecondary,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// ── Section Card ──────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = JD_CardColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

// ── Skills FlowRow ────────────────────────────────────────────────────────────
@Composable
private fun FlowRow(skills: List<String>, selectedSkills: MutableList<String>) {
    val rows      = mutableListOf<MutableList<String>>()
    var currentRow = mutableListOf<String>()
    skills.forEach { skill ->
        currentRow.add(skill)
        if (currentRow.size == 2) { rows.add(currentRow); currentRow = mutableListOf() }
    }
    if (currentRow.isNotEmpty()) rows.add(currentRow)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { skill ->
                    val isSelected = selectedSkills.contains(skill)
                    Surface(
                        shape    = RoundedCornerShape(50),
                        color    = if (isSelected) TealColor else Color.Transparent,
                        border   = if (!isSelected) BorderStroke(1.dp, JD_ChipOutline) else null,
                        modifier = Modifier.clickable {
                            if (isSelected) selectedSkills.remove(skill) else selectedSkills.add(skill)
                        }
                    ) {
                        Text(
                            skill,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = if (isSelected) Color.White else JD_TextPrimary,
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Info Card ─────────────────────────────────────────────────────────────────
@Composable
private fun InfoCard(job: Job) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = JD_InfoBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            InfoRow(
                icon      = Icons.Outlined.AccountBalanceWallet,
                label     = "BUDGET RANGE",
                value     = job.budget.ifEmpty { "Not specified" },
                valueBold = true
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(Modifier.height(16.dp))
            InfoRow(
                icon     = Icons.Outlined.CalendarMonth,
                label    = "TIMELINE",
                value    = "6-Month Contract",
                subValue = "With potential for permanent role"
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String = "",
    valueBold: Boolean = false
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(JD_CardColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = OrangeColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = JD_TextSecondary, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                fontSize   = 16.sp,
                fontWeight = if (valueBold) FontWeight.Bold else FontWeight.SemiBold,
                color      = JD_TextPrimary
            )
            if (subValue.isNotEmpty()) {
                Text(subValue, fontSize = 12.sp, color = JD_TextSecondary)
            }
        }
    }
}

// ── Location Card ─────────────────────────────────────────────────────────────
@Composable
private fun LocationCard(locationLabel: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Location", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = JD_TextPrimary)
        Spacer(Modifier.height(10.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = JD_CardColor),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFF1A4A44)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val lineColor = Color(0xFF1F5C55)
                        val step      = 30.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(lineColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f)
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                            y += step
                        }
                    }
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeColor, modifier = Modifier.size(36.dp))
                }
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(locationLabel, fontSize = 13.sp, color = JD_TextSecondary)
                }
            }
        }
    }
}

// ── Bottom Apply Bar ──────────────────────────────────────────────────────────
@Composable
private fun JobDetailBottomBar(
    hasApplied: Boolean,       // ── NEW ──
    onApply: () -> Unit
) {
    Surface(
        color           = JD_CardColor,
        shadowElevation = 8.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Button(
                onClick  = { if (!hasApplied) onApply() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape   = RoundedCornerShape(50),
                enabled = !hasApplied,
                colors  = ButtonDefaults.buttonColors(
                    containerColor         = if (hasApplied) Color(0xFFE6F4F2) else OrangeColor,
                    contentColor           = if (hasApplied) TealColor else Color.White,
                    disabledContainerColor = Color(0xFFE6F4F2),
                    disabledContentColor   = TealColor
                )
            ) {
                if (hasApplied) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Application Submitted ✓", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Apply Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}