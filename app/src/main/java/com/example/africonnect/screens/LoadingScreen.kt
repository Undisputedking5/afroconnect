package com.example.africonnect.screens


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun LoadingScreen(
    onClientReady: () -> Unit,
    onFreelancerReady: () -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        if (uid == null) {
            onFreelancerReady() // fallback
            return@LaunchedEffect
        }
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.getValue(String::class.java) ?: "FREELANCER"
                if (role == "CLIENT") onClientReady()
                else onFreelancerReady()
            }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TealColor)
    }
}