package com.example.africonnect.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.africonnect.model.UserProfile
import com.example.africonnect.model.Job
import com.example.africonnect.navigation.Routes
import androidx.navigation.NavController
import java.io.File
import kotlin.math.*
import androidx.compose.ui.graphics.Color
import com.example.africonnect.ui.theme.*
import androidx.compose.foundation.layout.BoxWithConstraints

// ─── Brand Colors ────────────────────────────────────────────────────────────
private val TealLight   = Color(0xFFE6F4F2)
private val BackgroundColor = Color(0xFFF5F7FA)
private val CardColor   = Color.White
private val TextPrimary = Color(0xFF1A1A2E)
private val TextSecondary = Color(0xFF6B7280)

// ─── Mock data (replace with ViewModel in prod) ───────────────────────────────
private data class MapFreelancer(
    val uid: String,
    val name: String,
    val title: String,
    val rating: Double,
    val reviews: Int,
    val hourlyRate: String,
    val distanceKm: Double,
    val isVerified: Boolean,
    val avatarUrl: String,
    // Normalized map position [0..1]
    val mapX: Float,
    val mapY: Float,
    val skills: List<String>
)

private data class MapJob(
    val id: String,
    val budget: String,
    val mapX: Float,
    val mapY: Float
)

private val sampleFreelancers = listOf(
    MapFreelancer("1", "Amara Osei",    "UI/UX Designer",        4.9, 124, "\$28", 1.2, true,  "", 0.35f, 0.58f, listOf("UI Design", "Figma")),
    MapFreelancer("2", "Kofi Mensah",   "Senior Mobile Dev",     5.0, 98,  "\$35", 2.1, true,  "", 0.65f, 0.42f, listOf("Android", "Kotlin")),
    MapFreelancer("3", "Zara Diallo",   "Backend Developer",     4.7, 56,  "\$30", 3.4, false, "", 0.25f, 0.35f, listOf("Node.js", "Firebase")),
    MapFreelancer("4", "Seun Adeyemi",  "Graphic Designer",      4.8, 83,  "\$22", 4.8, true,  "", 0.72f, 0.65f, listOf("Branding", "Illustrator")),
)

private val sampleJobs = listOf(
    MapJob("j1", "\$450", 0.55f, 0.68f),
    MapJob("j2", "\$200", 0.40f, 0.30f),
)

private val skillFilters = listOf("UI Design", "Backend Dev", "Marketing", "Python", "Android")

// ─── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    currentUserId: String = ""
) {
    var searchQuery      by remember { mutableStateOf("") }
    var selectedTab      by remember { mutableStateOf(0) }   // 0 = Nearby, 1 = Remote
    var selectedSkills   by remember { mutableStateOf(setOf<String>()) }
    var radiusKm         by remember { mutableStateOf(15f) }
    var selectedFreelancer by remember { mutableStateOf<MapFreelancer?>(sampleFreelancers.firstOrNull()) }

    var showFilterSheet  by remember { mutableStateOf(false) }

    val filteredFreelancers = remember(searchQuery, selectedSkills, selectedTab) {
        sampleFreelancers.filter { f ->
            val matchesSearch = searchQuery.isBlank() ||
                    f.name.contains(searchQuery, ignoreCase = true) ||
                    f.title.contains(searchQuery, ignoreCase = true)
            val matchesSkills = selectedSkills.isEmpty() ||
                    f.skills.any { it in selectedSkills }
            val matchesType = selectedTab == 1 // Remote shows all; Nearby filters by radius
            matchesSearch && matchesSkills
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {

        // ── Map Background ──────────────────────────────────────────────────
        MapBackground(modifier = Modifier.fillMaxSize())

        // ── Radius Circle (subtle overlay) ──────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.55f
            val px = (radiusKm / 50f) * size.width * 0.45f
            drawCircle(color = TealColor.copy(alpha = 0.08f), radius = px, center = Offset(cx, cy))
            drawCircle(color = TealColor.copy(alpha = 0.20f), radius = px, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        }

        // ── Map Pins ────────────────────────────────────────────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = this.maxWidth.value
            val h = this.maxHeight.value

            // Job pins
            sampleJobs.forEach { job ->
                val x = (job.mapX * w).dp - 28.dp
                val y = (job.mapY * h * 0.72f + h * 0.12f).dp - 28.dp
                JobPin(job = job, modifier = Modifier.offset(x = x, y = y))
            }

            // Freelancer pins
            filteredFreelancers.forEach { freelancer ->
                val x = (freelancer.mapX * w).dp - 36.dp
                val y = (freelancer.mapY * h * 0.72f + h * 0.12f).dp - 50.dp
                FreelancerPin(
                    freelancer = freelancer,
                    isSelected = selectedFreelancer?.uid == freelancer.uid,
                    onClick = { selectedFreelancer = freelancer },
                    modifier = Modifier.offset(x = x, y = y)
                )
            }
        }

        // ── Top Filter Card ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 12.dp)
        ) {
            FilterCard(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                selectedSkills = selectedSkills,
                onSkillToggle = { skill ->
                    selectedSkills = if (skill in selectedSkills)
                        selectedSkills - skill else selectedSkills + skill
                },
                radiusKm = radiusKm,
                onRadiusChange = { radiusKm = it }
            )
        }

        // ── Bottom: Selected Freelancer Card ────────────────────────────────
        AnimatedVisibility(
            visible = selectedFreelancer != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedFreelancer?.let { f ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    FreelancerCard(
                        freelancer = f,
                        onViewProfile = {
                            navController.navigate("${Routes.FREELANCER_PROFILE}/${f.uid}")
                        },
                        onDismiss = { selectedFreelancer = null }
                    )
                    // Bottom Nav
                    FreelancerBottomNavBar(
                        navController = navController,
                        selectedRoute = Routes.MAP
                    )
                }
            }
        }

        // ── No selection — still show nav ───────────────────────────────────
        if (selectedFreelancer == null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                FreelancerBottomNavBar(
                    navController = navController,
                    selectedRoute = Routes.MAP
                )
            }
        }

        // ── Re-center FAB ───────────────────────────────────────────────────
        val bottomOffset = if (selectedFreelancer != null) 180.dp else 90.dp
        FloatingActionButton(
            onClick = { /* Re-center map */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomOffset),
            containerColor = CardColor,
            contentColor = TealColor,
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "My Location")
        }
    }
}

// ─── Filter Card ──────────────────────────────────────────────────────────────
@Composable
private fun FilterCard(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    selectedSkills: Set<String>,
    onSkillToggle: (String) -> Unit,
    radiusKm: Float,
    onRadiusChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Search + Nearby/Remote tabs
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search skills...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealColor,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedContainerColor = Color(0xFFF8F9FA),
                        unfocusedContainerColor = Color(0xFFF8F9FA)
                    ),
                    modifier = Modifier.weight(1f).height(50.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Nearby / Remote pill tabs
                Row(
                    modifier = Modifier
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(50))
                        .padding(4.dp)
                ) {
                    listOf("Nearby", "Remote").forEachIndexed { i, label ->
                        val selected = selectedTab == i
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) TealColor else Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                                .clickable { onTabChange(i) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Skill filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(skillFilters) { skill ->
                    val isSelected = skill in selectedSkills
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSkillToggle(skill) },
                        label = {
                            Text(
                                skill,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color(0xFFF0F0F0),
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = Color.Transparent,
                            borderColor = Color.Transparent
                        )
                    )
                }
            }

            // Only show radius slider in Nearby tab
            AnimatedVisibility(visible = selectedTab == 0) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Radius",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(55.dp)
                        )
                        Slider(
                            value = radiusKm,
                            onValueChange = onRadiusChange,
                            valueRange = 1f..50f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = TealColor,
                                activeTrackColor = TealColor,
                                inactiveTrackColor = Color(0xFFD1D5DB)
                            )
                        )
                        Text(
                            "${radiusKm.toInt()} km",
                            fontSize = 13.sp,
                            color = TealColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Map Background (Canvas-drawn teal city grid) ─────────────────────────────
@Composable
private fun MapBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Teal-washed background
        drawRect(color = Color(0xFF1A7A6E).copy(alpha = 0.85f))
        drawRect(color = Color(0xFF0D5C52).copy(alpha = 0.3f))

        val roadColor = Color.White.copy(alpha = 0.15f)
        val majorRoadColor = Color.White.copy(alpha = 0.22f)

        // Draw concentric rings (ring-road city feel)
        val cx = size.width * 0.5f
        val cy = size.height * 0.55f
        for (i in 1..5) {
            val radius = (i * size.width * 0.09f)
            drawCircle(
                color = roadColor,
                radius = radius,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (i % 2 == 0) 3f else 1.5f)
            )
        }

        // Spoke roads radiating from center
        val spokesCount = 12
        for (i in 0 until spokesCount) {
            val angle = (i * 360.0 / spokesCount) * (PI / 180.0)
            val endX = cx + cos(angle).toFloat() * size.width * 0.6f
            val endY = cy + sin(angle).toFloat() * size.height * 0.6f
            drawLine(
                color = if (i % 3 == 0) majorRoadColor else roadColor,
                start = Offset(cx, cy),
                end = Offset(endX, endY),
                strokeWidth = if (i % 3 == 0) 2.5f else 1.2f
            )
        }

        // Horizontal grid lines
        val gridStep = size.height / 12f
        for (i in 0..12) {
            drawLine(
                color = roadColor.copy(alpha = 0.08f),
                start = Offset(0f, i * gridStep),
                end = Offset(size.width, i * gridStep),
                strokeWidth = 1f
            )
        }
        // Vertical grid lines
        val vGridStep = size.width / 8f
        for (i in 0..8) {
            drawLine(
                color = roadColor.copy(alpha = 0.08f),
                start = Offset(i * vGridStep, 0f),
                end = Offset(i * vGridStep, size.height),
                strokeWidth = 1f
            )
        }
    }
}

// ─── Freelancer Map Pin ───────────────────────────────────────────────────────
@Composable
private fun FreelancerPin(
    freelancer: MapFreelancer,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        label = "pinScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable { onClick() }
    ) {
        // Avatar circle
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) OrangeColor else TealColor,
                        shape = CircleShape
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(TealLight)
            ) {
                if (freelancer.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = File(freelancer.avatarUrl),
                        contentDescription = freelancer.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    // Initials fallback
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = freelancer.name.firstOrNull()?.toString() ?: "?",
                            color = TealColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Verified badge
            if (freelancer.isVerified) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = TealColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Pin triangle tip
        Canvas(modifier = Modifier.size(10.dp, 6.dp)) {
            val path = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(
                path,
                color = if (isSelected) OrangeColor else TealColor
            )
        }

        // Name + rate bubble (only when selected)
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .background(TealColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${freelancer.name.split(" ").first()} • ${freelancer.hourlyRate}/hr",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
// ─── Job Map Pin ──────────────────────────────────────────────────────────────
@Composable
private fun JobPin(
    job: MapJob,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Budget bubble
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(OrangeColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = job.budget,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Pin triangle tip
        Canvas(modifier = Modifier.size(10.dp, 6.dp)) {
            val path = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, color = OrangeColor)
        }
    }
}
// ─── Freelancer Info Card (bottom sheet style) ────────────────────────────────
@Composable
private fun FreelancerCard(
    freelancer: MapFreelancer,
    onViewProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (freelancer.avatarUrl.isNotBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(freelancer.avatarUrl))
                            .crossfade(true)
                            .build(),
                        contentDescription = freelancer.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Text(
                        text = freelancer.name.split(" ").map { it.first() }.take(2).joinToString(""),
                        color = TealColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = freelancer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    if (freelancer.isVerified) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Verified",
                            tint = TealColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "Verified",
                            color = TealColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = "${freelancer.title} • ${freelancer.distanceKm}km away",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${freelancer.hourlyRate}/hr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TealColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = OrangeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${freelancer.rating} (${freelancer.reviews} reviews)",
                        fontSize = 13.sp,
                        color = OrangeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // View button
            Button(
                onClick = onViewProfile,
                colors = ButtonDefaults.buttonColors(containerColor = TealColor),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("View", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Bottom Nav (reuse your existing one — this is a standalone fallback) ─────
@Composable
private fun FreelancerBottomNavBar(
    navController: NavController,
    selectedRoute: String
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .shadow(elevation = 12.dp)
    ) {
        data class NavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

        val items = listOf(
            NavItem(Routes.FREELANCER_HOME, Icons.Default.Home,     "Home"),
            NavItem(Routes.FREELANCER_HOME, Icons.Default.Work,     "Jobs"),   // adjust when Jobs route exists
            NavItem(Routes.MAP,             Icons.Default.Map,      "Map"),
            NavItem(Routes.FREELANCER_HOME, Icons.Default.Message,  "Messages"),
            NavItem(Routes.PROFILE,         Icons.Default.Person,   "Profile"),
        )

        items.forEach { item ->
            val isSelected = item.route == selectedRoute && item.label == "Map"
                    || (item.label != "Map" && false)  // only Map truly selected here
            NavigationBarItem(
                selected = item.label == "Map",
                onClick = {
                    if (item.route != selectedRoute) navController.navigate(item.route)
                },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealColor,
                    selectedTextColor = TealColor,
                    indicatorColor = TealLight,
                    unselectedIconColor = Color(0xFF9CA3AF),
                    unselectedTextColor = Color(0xFF9CA3AF)
                )
            )
        }
    }
}