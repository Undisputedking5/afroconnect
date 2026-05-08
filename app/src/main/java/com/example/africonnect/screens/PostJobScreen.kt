package com.example.africonnect.screens


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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.africonnect.viewmodel.JobViewModel

@Composable
fun PostJobScreen(
    onJobPosted: () -> Unit,
    onBack: () -> Unit,
    viewModel: JobViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var title by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val categories = listOf(
        "UI/UX Design", "Frontend Dev", "Backend Dev",
        "Mobile Dev", "Copywriting", "Data Science"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // --- Top Bar ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = TealColor)
            }
            Text(
                text = "Post a Job",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Job Title ---
        Text("Job Title", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("e.g. Senior UI/UX Designer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Rate ---
        Text("Rate ($/hr)", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            placeholder = { Text("e.g. 50") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Category ---
        Text("Category", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        categories.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Description ---
        Text("Description", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Describe the role and requirements...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(8.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Skills ---
        Text("Required Skills", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = skills,
            onValueChange = { skills = it },
            placeholder = { Text("e.g. Figma, React Native, UI Design") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        Text(
            "Separate skills with commas",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Location ---
        Text("Location", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            placeholder = { Text("e.g. Remote, Lagos") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Urgent Toggle ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Urgent Hiring", fontWeight = FontWeight.Medium)
                Text(
                    "Mark as urgent to attract faster applicants",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = isUrgent,
                onCheckedChange = { isUrgent = it },
                colors = SwitchDefaults.colors(checkedThumbColor = TealColor)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Error ---
        if (error.isNotEmpty()) {
            Text(error, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- Post Button ---
        Button(
            onClick = {
                if (title.isBlank() || rate.isBlank() || description.isBlank()) {
                    error = "Please fill in all required fields"
                    return@Button
                }
                isLoading = true
                val skillsList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                viewModel.postJob(
                    title = title,
                    rate = "$$rate/HR",
                    description = description,
                    skills = skillsList,
                    category = category,
                    location = location,
                    isUrgent = isUrgent,
                    onSuccess = {
                        isLoading = false
                        onJobPosted()
                    },
                    onError = {
                        isLoading = false
                        error = it
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeColor),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Post Job", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}