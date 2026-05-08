package com.example.africonnect.model

data class Job(
    val id: String = "",
    val title: String = "",
    val rate: String = "",
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val description: String = "",
    val skills: List<String> = emptyList(),
    val category: String = "",
    val isUrgent: Boolean = false,
    val location: String = "",
    val postedAt: Long = 0L,
    val postedBy: String = "",           // ← who posted this job
    val applicants: Map<String, Boolean> = emptyMap() ,
    val budget:String = "",
    // GeoMatch fields
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationLabel: String = "",
    val radiusKm: Int = 10,
    val workType: String = "Remote",
    val hiredFreelancerId: String = "",
    val hiredFreelancerName: String = "",
    val status: String = "open"



)