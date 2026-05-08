package com.example.africonnect.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
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
import com.example.africonnect.viewmodel.ProfileViewModel
import java.io.File
import androidx.compose.material.icons.automirrored.filled.OpenInNew

// ── Palette ───────────────────────────────────────────────────────────────────
private val FPBg           = Color(0xFFF5F6FA)
private val FPCard         = Color.White
private val FPTeal         = Color(0xFF1A7A6E)
private val FPOrange       = Color(0xFFE8621A)
private val FPTextPri      = Color(0xFF0D1B2A)
private val FPTextSec      = Color(0xFF6B7280)
private val FPStar         = Color(0xFFFBBF24)
private val FPChipSel      = Color(0xFF1A7A6E)
private val FPChipUnsel    = Color(0xFFF0F4F3)
private val FPChipSelTxt   = Color.White
private val FPChipUnselTxt = Color(0xFF374151)

private data class FPReview(
    val initials: String,
    val name: String,
    val role: String,
    val rating: Int,
    val comment: String,
    val avatarBg: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreelancerProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    isOwnProfile: Boolean = true,
    onEdit: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context   = LocalContext.current
    val profile   by profileViewModel.profile.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val isUploadingAvatar    by profileViewModel.isUploadingAvatar.collectAsState()
    val isUploadingPortfolio by profileViewModel.isUploadingPortfolio.collectAsState()

    // Load own profile on open
    LaunchedEffect(Unit) {
        if (isOwnProfile && profile == null) profileViewModel.loadProfile()
    }

    // ── Avatar picker — triggers upload immediately ────────────────────────────
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { profileViewModel.uploadAvatar(it) }    }

    // ── Portfolio image picker ────────────────────────────────────────────────
    val portfolioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { profileViewModel.uploadPortfolioImage(it) }
    }

    // ── Chips ─────────────────────────────────────────────────────────────────
    val chips = remember(profile) {
        profile?.skills?.takeIf { it.isNotEmpty() }
            ?: listOf(
                "UX Architecture", "Product Strategy",
                "Visual Design",   "Figma Mastery",
                "React Native UI", "Motion Design"
            )
    }
    val selectedChips = remember(chips) {
        mutableStateListOf<String>().also { list ->
            if (chips.size >= 2) list.add(chips[1])
            if (chips.size >= 6) list.add(chips[5])
        }
    }

    // ── Reviews ───────────────────────────────────────────────────────────────
    val reviews = listOf(
        FPReview("KO", "Kofi Osei", "CEO, TechLoom Solutions", 5,
            "\"Amara is a brilliant designer. She took our messy requirements and turned them into a seamless, beautiful product that our users love.\"",
            Color(0xFF6B4226)),
        FPReview("SN", "Sarah Ndidi", "Product Lead, Bloom", 4,
            "\"Great attention to detail and very responsive. The final delivery exceeded our expectations.\"",
            Color(0xFF2D6A4F))
    )

    // ── Display values ────────────────────────────────────────────────────────
    val displayName   = profile?.name.takeIf      { !it.isNullOrBlank() } ?: "Amara Okafor"
    val displayTitle  = profile?.title.takeIf     { !it.isNullOrBlank() } ?: "Senior UX/UI Architect & Brand Strategist"
    val displayAbout  = profile?.about.takeIf     { !it.isNullOrBlank() }
        ?: "Passionate about crafting digital experiences that bridge the gap between human needs and technological possibilities."
    val displayRate   = profile?.hourlyRate.takeIf { !it.isNullOrBlank() } ?: "8+ Years"
    val displayProjects = profile?.projects.takeIf { !it.isNullOrBlank() } ?: "312"
    val displayRating   = if ((profile?.rating ?: 0.0) > 0) profile!!.rating else 4.9
    val displayReviews  = if ((profile?.reviews ?: 0) > 0) profile!!.reviews else 124
    val initials = displayName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2).joinToString("")

    // Portfolio images from Firebase (real) + placeholder slots
    val portfolioImages = profile?.portfolioImages ?: emptyList()

    // ── Helper: open URL in browser ───────────────────────────────────────────
    fun openUrl(url: String) {
        if (url.isBlank()) return
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
    }

    Scaffold(
        containerColor = FPBg,
        topBar = {
            TopAppBar(
                title = {
                    Text("AfriConnect", fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = FPTextPri)
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.NotificationsNone,
                            contentDescription = "Notifications", tint = FPTextPri)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FPCard)
            )
        },
        bottomBar = {
            FreelancerBottomNavBar(
                selected = "Profile",
                onNavigate = { item ->
                    when (item) {
                        "Home"     -> navController.navigate("freelancer_home") { launchSingleTop = true }
                        "Map"      -> navController.navigate("map")             { launchSingleTop = true }
                        "Jobs"     -> navController.navigate("jobs")            { launchSingleTop = true }
                        "Messages" -> navController.navigate("messages")        { launchSingleTop = true }
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FPTeal)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ══ HERO ══════════════════════════════════════════════════════════
            Surface(color = FPCard, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF2D6A4F), FPTeal))
                                )
                                .border(3.dp, FPCard, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (!profile?.avatarUrl.isNullOrBlank()) {
                                // Load from Firebase Storage URL — persists across sessions
                                AsyncImage(
                                    model = File(profile!!.avatarUrl),
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(initials, color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 34.sp)
                            }
                        }
                        // Verified badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(FPTeal)
                                .border(2.dp, FPCard, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Verified, null,
                                tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Upload photo button (own profile only)
                    if (isOwnProfile) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { avatarPicker.launch("image/*") },
                            enabled = !isUploadingAvatar,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, null,
                                tint = FPTeal, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isUploadingAvatar) "Uploading…" else "Upload photo",
                                fontSize = 12.sp, color = FPTeal, fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }

                    Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = FPTextPri, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(displayTitle, fontSize = 13.sp, color = FPTextSec,
                        textAlign = TextAlign.Center, lineHeight = 18.sp)
                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null,
                            tint = FPStar, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$displayRating  ($displayReviews reviews)",
                            fontSize = 13.sp, color = FPTextSec)
                    }

                    Spacer(Modifier.height(20.dp))

                    if (!isOwnProfile) {
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FPTeal)
                        ) {
                            Text("Hire ${displayName.split(" ").first()}",
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, FPTeal),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FPTeal)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ══ STATS ═════════════════════════════════════════════════════════
            Surface(
                color = FPCard,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FPStat("EXPERIENCE", displayRate, FPTextPri)
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE5E7EB))
                    FPStat("PROJECTS", displayProjects, FPOrange)
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE5E7EB))
                    // Social presence icons — clickable
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SocialIcon(Icons.Default.Code,     profile?.github    ?: "") { openUrl(it) }
                            SocialIcon(Icons.Default.Link,     profile?.linkedin  ?: "") { openUrl(it) }
                            SocialIcon(Icons.Default.Language, profile?.dribbble  ?: "") { openUrl(it) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("PRESENCE", fontSize = 10.sp, color = FPTextSec,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ══ ABOUT ME ══════════════════════════════════════════════════════
            FPSection("About Me") {
                Text(displayAbout, fontSize = 14.sp, color = FPTextSec, lineHeight = 22.sp)
            }

            Spacer(Modifier.height(24.dp))

            // ══ EXPERTISE CHIPS ═══════════════════════════════════════════════
            FPSection("Expertise") {
                chips.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        row.forEach { chip ->
                            val selected = chip in selectedChips
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = if (selected) FPChipSel else FPChipUnsel,
                                modifier = Modifier.clickable {
                                    if (selected) selectedChips.remove(chip)
                                    else selectedChips.add(chip)
                                }
                            ) {
                                Text(chip, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                    color = if (selected) FPChipSelTxt else FPChipUnselTxt,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ══ PORTFOLIO ═════════════════════════════════════════════════════
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Portfolio", fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = FPTextPri)
                    if (isOwnProfile) {
                        TextButton(
                            onClick = { portfolioPicker.launch("image/*") },
                            enabled = !isUploadingPortfolio,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, null,
                                tint = FPTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                if (isUploadingPortfolio) "Uploading…" else "Add",
                                color = FPTeal, fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (portfolioImages.isEmpty()) {
                    // Show placeholder grid when no images uploaded yet
                    PortfolioPlaceholderGrid(isOwnProfile) {
                        portfolioPicker.launch("image/*")
                    }
                } else {
                    // Render real uploaded images
                    PortfolioGrid(
                        images = portfolioImages,
                        isOwnProfile = isOwnProfile,
                        onDelete = { url -> profileViewModel.deletePortfolioImage(url) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ══ SOCIAL LINKS ══════════════════════════════════════════════════
            FPSection("Social Presence") {
                Surface(
                    color = FPCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple("GitHub",    Icons.Default.Code,     profile?.github    ?: ""),
                            Triple("LinkedIn",  Icons.Default.Link,     profile?.linkedin  ?: ""),
                            Triple("Instagram", Icons.Default.CameraAlt,profile?.instagram ?: ""),
                            Triple("Dribbble",  Icons.Default.Language, profile?.dribbble  ?: "")
                        ).forEachIndexed { index, (platform, icon, handle) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = handle.isNotBlank()) { openUrl(handle) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0F4F3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null, tint = FPTeal,
                                        modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(platform, fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp, color = FPTextPri)
                                    Text(
                                        handle.ifBlank { "Not linked" },
                                        fontSize = 12.sp,
                                        color = if (handle.isNotBlank()) FPTeal else FPTextSec
                                    )
                                }
                                if (handle.isNotBlank()) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null,
                                        tint = FPTextSec, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (index < 3) HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ══ RECENT REVIEWS ════════════════════════════════════════════════
            FPSection("Recent Reviews") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    reviews.forEach { review -> FPReviewCard(review) }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ══ LOGOUT ════════════════════════════════════════════════════════
            if (isOwnProfile) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEEE))
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold, color = Color(0xFFCC0000))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Portfolio grid with real images ──────────────────────────────────────────
@Composable
private fun PortfolioGrid(
    images: List<String>,
    isOwnProfile: Boolean,
    onDelete: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left — first image tall
        val firstImage = images.firstOrNull()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A2940)),
            contentAlignment = Alignment.BottomStart
        ) {
            if (firstImage != null) {
                AsyncImage(
                    model = File(firstImage),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isOwnProfile && firstImage != null) {
                IconButton(
                    onClick = { onDelete(firstImage) },
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                ) {
                    Icon(Icons.Default.Close, null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Right — remaining images stacked
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            images.drop(1).take(2).forEach { url ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A1A))
                ) {
                    AsyncImage(
                        model = File(url),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isOwnProfile) {
                        IconButton(
                            onClick = { onDelete(url) },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, null,
                                tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Portfolio placeholder when empty ─────────────────────────────────────────
@Composable
private fun PortfolioPlaceholderGrid(isOwnProfile: Boolean, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A2940))
                .then(if (isOwnProfile) Modifier.clickable { onAdd() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (isOwnProfile) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, tint = Color.White.copy(0.7f),
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Add work", color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
            } else {
                Text("No portfolio yet", color = Color.White.copy(0.5f), fontSize = 12.sp)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A2A).copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("—", color = Color.White.copy(0.3f), fontSize = 18.sp)
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SocialIcon(icon: ImageVector, url: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (url.isNotBlank()) Color(0xFFE8F5F3) else Color(0xFFF3F4F6))
            .clickable(enabled = url.isNotBlank()) { onClick(url) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            tint = if (url.isNotBlank()) FPTeal else FPTextSec,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun FPStat(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color(0xFF9CA3AF),
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun FPSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FPTextPri)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun FPReviewCard(review: FPReview) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(review.avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(review.initials, color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(review.name, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, color = FPTextPri)
                Text(review.role, fontSize = 12.sp, color = FPTextSec)
            }
            Row {
                repeat(5) { i ->
                    Icon(
                        if (i < review.rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = FPStar,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(review.comment, fontSize = 13.sp, color = Color(0xFF4B5563), lineHeight = 20.sp)
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}