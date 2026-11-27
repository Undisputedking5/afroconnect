package com.example.africonnect.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.africonnect.R
import com.example.africonnect.model.Roles
import com.example.africonnect.ui.components.*
import com.example.africonnect.ui.theme.Sunset
import com.example.africonnect.viewmodel.AuthState
import com.example.africonnect.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private data class Slide(val image: Int, val title: String, val highlight: String, val body: String)

private val slides = listOf(
    Slide(R.drawable.slide1, "Hire Africa's ", "best talent", "Find verified designers, developers and creatives across the continent — remote or right around the corner."),
    Slide(R.drawable.slide2, "Find your next ", "big opportunity", "Browse jobs, send proposals and get hired by clients who value your skills."),
    Slide(R.drawable.slide3, "Work together, ", "seamlessly", "Chat in real time, share files, and build your reputation with reviews."),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.weight(1f))
            if (!last) TextButton(onClick = onFinished) { Text("Skip") }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            val s = slides[page]
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.extraLarge)) {
                    Image(painterResource(s.image), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)))))
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    buildAnnotatedString {
                        append(s.title)
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(s.highlight) }
                    },
                    style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    s.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(slides.size) { i ->
                val w by animateDpAsState(if (i == pager.currentPage) 28.dp else 8.dp, label = "dot")
                Box(
                    Modifier.padding(horizontal = 4.dp).height(8.dp).width(w).clip(CircleShape)
                        .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
            }
        }
        PrimaryButton(
            text = if (last) "Get started" else "Next",
            icon = if (last) null else Icons.AutoMirrored.Filled.ArrowForward,
            onClick = { if (last) onFinished() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.padding(horizontal = 24.dp),
            container = if (last) Sunset else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun BrandMark(light: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (light) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) { Text("A", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
        Spacer(Modifier.width(8.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("Afro") }
                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append("Connect") }
            },
            fontSize = 20.sp,
            color = if (light) Color.White else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Box(
        Modifier.fillMaxWidth().background(BrandGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 36.dp)
    ) {
        Column {
            BrandMark(light = true)
            Spacer(Modifier.height(28.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun LoginScreen(onRegister: () -> Unit, onSuccess: () -> Unit, vm: AuthViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) { if (state is AuthState.Success) onSuccess() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        AuthHeader("Welcome back", "Sign in to keep connecting with talent and opportunities.")
        Column(Modifier.padding(24.dp)) {
            ErrorBanner((state as? AuthState.Error)?.message)
            InfoBanner((state as? AuthState.Info)?.message)
            Spacer(Modifier.height(8.dp))
            AppTextField(email, { email = it; vm.clear() }, "Email", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            AppTextField(password, { password = it; vm.clear() }, "Password", leadingIcon = Icons.Outlined.Lock, isPassword = true)
            TextButton(onClick = { vm.resetPassword(email) }, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?")
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Sign in", { vm.login(email, password) }, loading = state is AuthState.Loading)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("New to AfroConnect?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRegister) { Text("Create an account", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun RegisterScreen(onLogin: () -> Unit, onSuccess: () -> Unit, vm: AuthViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var role by remember { mutableStateOf(Roles.FREELANCER) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LaunchedEffect(state) { if (state is AuthState.Success) onSuccess() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        AuthHeader("Join AfroConnect", "Create your free account in under a minute.")
        Column(Modifier.padding(24.dp)) {
            Text("I want to…", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoleOption("Find work", "I'm a freelancer", Icons.Outlined.Laptop, role == Roles.FREELANCER, Modifier.weight(1f)) { role = Roles.FREELANCER }
                RoleOption("Hire talent", "I'm a client", Icons.Outlined.BusinessCenter, role == Roles.CLIENT, Modifier.weight(1f)) { role = Roles.CLIENT }
            }
            Spacer(Modifier.height(20.dp))
            ErrorBanner((state as? AuthState.Error)?.message)
            Spacer(Modifier.height(8.dp))
            AppTextField(name, { name = it; vm.clear() }, "Full name", leadingIcon = Icons.Outlined.Person)
            Spacer(Modifier.height(12.dp))
            AppTextField(email, { email = it; vm.clear() }, "Email", leadingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            AppTextField(password, { password = it; vm.clear() }, "Password", leadingIcon = Icons.Outlined.Lock, isPassword = true, supporting = "At least 6 characters")
            Spacer(Modifier.height(4.dp))
            AppTextField(confirm, { confirm = it; vm.clear() }, "Confirm password", leadingIcon = Icons.Outlined.Lock, isPassword = true)
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Create account", { vm.register(name, email, password, confirm, role) }, loading = state is AuthState.Loading)
            Spacer(Modifier.height(12.dp))
            Text(
                "By creating an account you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onLogin) { Text("Sign in", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun RoleOption(title: String, subtitle: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = if (selected) cs.primaryContainer.copy(alpha = 0.5f) else cs.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Icon(icon, null, tint = if (selected) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(28.dp))
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Filled.CheckCircle, null, tint = cs.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}
