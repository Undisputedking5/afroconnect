package com.example.africonnect.screens


import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.africonnect.R

val OrangeColor = Color(0xFFE8621A)

data class OnboardingPage(
    val title: String,
    val titleHighlight: String = "",
    val subtitle: String,
    val imageRes:Int,
    val isLastPage: Boolean = false
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Hire African Talent Globally",
        subtitle = "Access a pool of verified experts across Africa for your next big project.",
        imageRes = R.drawable.slide1
    ),
    OnboardingPage(
        title = "Find Your Next\nBig ",
        titleHighlight = "Opportunity",
        subtitle = "Connect with global clients and work on exciting projects.",
        imageRes = R.drawable.slide2

    ),
    OnboardingPage(
        title = "Secure & Seamless Payments",
        subtitle = "Get paid on time, every time, with our secure global payment system.",
        imageRes = R.drawable.slide3,

        isLastPage = true
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val page = onboardingPages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Top Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AfriConnect",
                    color = TealColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (!page.isLastPage) {
                    TextButton(onClick = { finishOnboarding(context, onFinished) }) {
                        Text("SKIP", color = TealColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Image Placeholder ---
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Title ---
            if (page.titleHighlight.isEmpty()) {
                Text(
                    text = page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            } else {
                // Two-color title for slide 2
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = page.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = page.titleHighlight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Subtitle ---
            Text(
                text = page.subtitle,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- Dot Indicators ---
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                onboardingPages.forEachIndexed { index, _ ->
                    val width by animateDpAsState(
                        targetValue = if (index == currentPage) 24.dp else 8.dp,
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPage) TealColor else Color.LightGray
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Buttons ---
            if (page.isLastPage) {
                // Last page: Get Started full width
                Button(
                    onClick = { finishOnboarding(context, onFinished) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Get Started →", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { currentPage-- }) {
                    Text("Back to overview", color = Color.Gray)
                }
            } else if (currentPage == 0) {
                // First page: Back (disabled) + Next (orange)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("←", color = Color.LightGray)
                    }
                    Button(
                        onClick = { currentPage++ },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeColor),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("Next", fontSize = 16.sp)
                    }
                }
            } else {
                // Middle page: Continue full width
                Button(
                    onClick = { currentPage++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue →", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

fun finishOnboarding(context: Context, onFinished: () -> Unit) {
    context.getSharedPreferences("africonnect_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_complete", true)
        .apply()
    onFinished()
}