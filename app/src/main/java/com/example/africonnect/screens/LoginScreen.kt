package com.example.africonnect.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.africonnect.viewmodel.AuthState
import com.example.africonnect.viewmodel.AuthViewModel

@Composable

fun LoginScreen(
    onRegisterClick: () -> Unit,
    onSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // --- Top Bar ---
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "←",
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "AfriConnect",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TealColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Header ---
        Text(
            text = "Welcome Back",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Sign in to continue connecting with talent or finding your next project.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // --- Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Email
                Text("Email Address", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("your@email.com", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )
                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // Password
                Text("Password", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "🙈" else "👁")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )
                HorizontalDivider()

                // Forgot Password
                Text(
                    text = "Forgot Password?",
                    color = TealColor,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                        .clickable { onForgotPassword() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error
                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sign In Button
                Button(
                    onClick = { viewModel.loginWithEmail(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealColor),
                    shape = RoundedCornerShape(50.dp),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Sign In", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "  OR CONTINUE WITH  ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google & Apple
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("G  Google", color = Color.DarkGray)
                    }
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("iOS  Apple", color = Color.DarkGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Register link
        Row {
            Text("Don't have an account? ")
            Text(
                text = "Sign up",
                color = TealColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Footer
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Privacy Policy", fontSize = 12.sp, color = Color.Gray)
            Text("Terms of Service", fontSize = 12.sp, color = Color.Gray)
            Text("Help Center", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "© 2026 AfriConnect. All rights reserved.",
            fontSize = 11.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}