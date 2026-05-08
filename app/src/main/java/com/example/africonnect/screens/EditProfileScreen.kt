package com.example.africonnect.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.africonnect.model.UserProfile
import com.example.africonnect.ui.theme.TealColor
import com.example.africonnect.viewmodel.ProfileViewModel

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isFreelancer = profile?.role?.lowercase() == "freelancer"

    // Pre-fill fields with existing data
    var name       by remember { mutableStateOf(profile?.name ?: "") }
    var title      by remember { mutableStateOf(profile?.title ?: "") }
    var location   by remember { mutableStateOf(profile?.location ?: "") }
    var experience by remember { mutableStateOf(profile?.experience ?: "") }
    var hourlyRate by remember { mutableStateOf(profile?.hourlyRate ?: "") }
    var projects   by remember { mutableStateOf(profile?.projects ?: "") }
    var about      by remember { mutableStateOf(profile?.about ?: "") }
    var skills     by remember { mutableStateOf(profile?.skills?.joinToString(", ") ?: "") }
    var github     by remember { mutableStateOf(profile?.github ?: "") }
    var instagram  by remember { mutableStateOf(profile?.instagram ?: "") }
    var linkedin   by remember { mutableStateOf(profile?.linkedin ?: "") }
    var dribbble   by remember { mutableStateOf(profile?.dribbble ?: "") }
    var company    by remember { mutableStateOf(profile?.company ?: "") }
    var isLoading  by remember { mutableStateOf(false) }
    var error      by remember { mutableStateOf("") }

    // Update fields when profile loads
    LaunchedEffect(profile) {
        profile?.let {
            name       = it.name
            title      = it.title
            location   = it.location
            experience = it.experience
            hourlyRate = it.hourlyRate
            projects   = it.projects
            about      = it.about
            skills     = it.skills.joinToString(", ")
            github     = it.github
            instagram  = it.instagram
            linkedin   = it.linkedin
            dribbble   = it.dribbble
            company    = it.company ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TealColor
                )
            }
            Text(
                "Edit Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Basic Info ---
        SectionTitle("Basic Info")
        EditField("Full Name", name) { name = it }
        EditField("Location", location, "e.g. Nairobi, Kenya") { location = it }

        if (isFreelancer) {
            EditField("Professional Title", title, "e.g. Senior UI/UX Designer") { title = it }
        } else {
            EditField("Company", company, "e.g. Safaricom PLC") { company = it }
            EditField("LinkedIn", linkedin, "linkedin.com/in/username") { linkedin = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- About ---
        SectionTitle("About")
        OutlinedTextField(
            value = about,
            onValueChange = { about = it },
            placeholder = { Text(if (isFreelancer) "Tell clients about yourself..." else "Tell freelancers about your company...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(8.dp),
            maxLines = 5
        )

        if (isFreelancer) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Stats (freelancer only) ---
            SectionTitle("Stats")
            EditField("Experience", experience, "e.g. 3+ Years") { experience = it }
            EditField("Hourly Rate", hourlyRate, "e.g. \$30") { hourlyRate = it }
            EditField("Projects Completed", projects, "e.g. 20+") { projects = it }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Skills ---
            SectionTitle("Skills")
            EditField("Skills", skills, "e.g. Figma, Kotlin, Firebase") { skills = it }
            Text("Separate skills with commas", fontSize = 11.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            // --- Social ---
            SectionTitle("Social Presence")
            EditField("GitHub", github, "github.com/username") { github = it }
            EditField("Instagram", instagram, "@username") { instagram = it }
            EditField("LinkedIn", linkedin, "linkedin.com/in/username") { linkedin = it }
            EditField("Dribbble", dribbble, "dribbble.com/username") { dribbble = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (error.isNotEmpty()) {
            Text(error, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Save Button
        Button(
            onClick = {
                if (name.isBlank()) {
                    error = "Name cannot be empty"
                    return@Button
                }
                isLoading = true
                error = ""

                val updatedProfile = (profile ?: UserProfile()).copy(
                    name       = name,
                    title      = title,
                    location   = location,
                    experience = experience,
                    hourlyRate = hourlyRate,
                    projects   = projects,
                    about      = about,
                    skills     = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    github     = github,
                    instagram  = instagram,
                    linkedin   = linkedin,
                    dribbble   = dribbble,
                    company    = company
                )

                val onSuccess = {
                    isLoading = false
                    onSaved()
                }
                val onError = { msg: String ->
                    isLoading = false
                    error = msg
                }

                if (isFreelancer) {
                    viewModel.saveFreelancerProfile(updatedProfile, onSuccess, onError)
                } else {
                    viewModel.saveClientProfile(updatedProfile, onSuccess, onError)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealColor),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TealColor,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun EditField(
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.LightGray) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
}