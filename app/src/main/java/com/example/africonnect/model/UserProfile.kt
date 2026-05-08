package com.example.africonnect.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val title: String = "",
    val location: String = "",
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val experience: String = "",
    val hourlyRate: String = "",
    val projects: String = "",
    val about: String = "",
    val skills: List<String> = emptyList(),
    val github: String = "",
    val instagram: String = "",
    val linkedin: String = "",          // single field — removed duplicate linkedIn
    val dribbble: String = "",

    // GeoMatch fields
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val showOnMap: Boolean = false,
    val workType: String = "Remote",
    val availabilityRadius: Int = 10,
    val isAvailable: Boolean = true,

    // Media — Firebase Storage download URLs
    val avatarUrl: String = "",
    val portfolioImages: List<String> = emptyList(),

    // Client-specific fields
    val company: String = "",
    val projectsPosted: Int = 0,
    val totalSpent: String = "",
    val memberSince: String = "",
)