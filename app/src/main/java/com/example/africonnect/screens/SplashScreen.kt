package com.example.africonnect.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import com.example.africonnect.R

@Composable
fun SplashScreen(onComplete: () -> Unit) {

    val logoScale = remember { Animatable(0.75f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(14f) }
    val progress  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        delay(100)
        textOffset.animateTo(0f, tween(480, easing = EaseOut))
        textAlpha.animateTo(1f, tween(450))
        progress.animateTo(1f, tween(2000, easing = LinearEasing))
        delay(200)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Logo — reuse your existing ic_launcher
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFFF4F4F4)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "AfriConnect",// In your routes object/companion
                    modifier = Modifier.size(108.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Brand name
            Row(
                modifier = Modifier.offset(y = textOffset.value.dp).alpha(textAlpha.value)
            ) {
                Text(
                    "Afri",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2D2D2D),
                    fontFamily = FontFamily.Serif
                )
                Text(
                    "Connect",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF4A4A4A),
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(Modifier.height(7.dp))

            // — · — separator (matches the logo exactly)
            Row(
                modifier = Modifier.alpha(textAlpha.value),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.width(28.dp).height(1.dp).background(Color(0xFF9CA3AF)))
                Box(Modifier.size(4.dp).background(Color(0xFF9CA3AF), CircleShape))
                Box(Modifier.width(28.dp).height(1.dp).background(Color(0xFF9CA3AF)))
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "CONNECTING AFRICA'S TALENT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(44.dp))

            // Progress bar
            Box(
                Modifier
                    .width(140.dp)
                    .height(2.dp)
                    .background(Color(0xFFE0E0E0))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .background(Color(0xFF3D3D3D))
                )
            }
        }
    }
}