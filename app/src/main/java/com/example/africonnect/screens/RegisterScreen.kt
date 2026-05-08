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
import com.example.africonnect.model.UserRole
import com.example.africonnect.viewmodel.AuthState
import com.example.africonnect.viewmodel.AuthViewModel

val TealColor = Color(0xFF1A7A6E)

@Composable
fun RegisterScreen(
    onSignInClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var selectedRole by remember { mutableStateOf(UserRole.CLIENT) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onSuccess()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- Header ---
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Join the ecosystem",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Connect with top-tier African talent or find world-class projects.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- Role Cards ---
        RoleCard(
            title = "I am a Client",
            subtitle = "Looking to hire verified experts for high-impact projects.",
            icon = "💼",
            isSelected = selectedRole == UserRole.CLIENT,
            onClick = { selectedRole = UserRole.CLIENT }
        )

        Spacer(modifier = Modifier.height(16.dp))

        RoleCard(
            title = "I am a Freelancer",
            subtitle = "I offer my professional services to global and local clients.",
            icon = "💻",
            isSelected = selectedRole == UserRole.FREELANCER,
            onClick = { selectedRole = UserRole.FREELANCER }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- Google & Apple Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* Google Sign In */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("G  Google")
            }
            OutlinedButton(
                onClick = { /* Apple Sign In */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("iOS  Apple")
            }
        }

        // --- Divider ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  OR CONTINUE WITH EMAIL  ",
                fontSize = 11.sp,
                color = Color.Gray
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        // --- Email Field ---
        Text(
            text = "Email Address",
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("name@company.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Password Field ---
        Text(
            text = "Password",
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = if (passwordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Create Account Button ---

        Button(
            onClick = { viewModel.registerWithEmail(email, password,selectedRole) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealColor),
            shape = RoundedCornerShape(8.dp),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Create Account", fontSize = 16.sp)
            }
        }
    Spacer(modifier = Modifier.height(16.dp))


        // --- Sign In Link ---
        Row {
            Text("Already have an account? ")
            Text(
                text = "Sign In",
                color = TealColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignInClick() }
            )
        }
    }
}

// --- Role Card Composable ---
@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TealColor else Color.LightGray
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEFF8F7) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSelected) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "✔",
                        color = TealColor,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }

}
//@Preview(showBackground= true, showSystemUi = true)
//@Composable
//fun RegisterScreenPreview(){
//    RegisterScreen(rememberNavController())
//}