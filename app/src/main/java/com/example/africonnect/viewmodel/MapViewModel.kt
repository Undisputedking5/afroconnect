package com.example.africonnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.africonnect.model.UserProfile
import com.example.africonnect.model.Job
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.pow

data class MapUiState(
    val freelancers: List<UserProfile> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val selectedFreelancer: UserProfile? = null,
    val searchQuery: String = "",
    val selectedSkills: Set<String> = emptySet(),
    val radiusKm: Int = 15,
    val workTypeFilter: String = "Nearby", // "Nearby" or "Remote"
    val isLoading: Boolean = false,
    val error: String? = null
)

class MapViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Derived filtered list
    val filteredFreelancers: StateFlow<List<UserProfile>> = _uiState.map { state ->
        state.freelancers.filter { profile ->
            // Search filter
            val matchesSearch = state.searchQuery.isBlank() ||
                    profile.name.contains(state.searchQuery, ignoreCase = true) ||
                    profile.title.contains(state.searchQuery, ignoreCase = true)

            // Skill filter
            val matchesSkills = state.selectedSkills.isEmpty() ||
                    profile.skills.any { it in state.selectedSkills }

            // Work type filter
            val matchesWorkType = state.workTypeFilter == "Remote" ||
                    profile.workType == "Remote" ||
                    (state.workTypeFilter == "Nearby" && profile.showOnMap)

            // Radius filter (only applies to Nearby with real coordinates)
            val matchesRadius = if (state.workTypeFilter == "Nearby" && profile.showOnMap) {
                // Simple Euclidean approximation — swap for Haversine in prod
                val distKm = haversineKm(
                    lat1 = -1.2921, lon1 = 36.8219, // Nairobi center (replace with user's actual coords)
                    lat2 = profile.latitude,
                    lon2 = profile.longitude
                )
                distKm <= state.radiusKm
            } else true

            matchesSearch && matchesSkills && matchesWorkType && matchesRadius
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredJobs: StateFlow<List<Job>> = _uiState.map { state ->
        state.jobs.filter { job ->
            val matchesWorkType = state.workTypeFilter == "Remote" ||
                    job.workType == state.workTypeFilter
            val matchesSkills = state.selectedSkills.isEmpty() ||
                    job.skills.any { it in state.selectedSkills }
            matchesWorkType && matchesSkills
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFreelancersOnMap()
        loadJobsOnMap()
    }

    // ── Load freelancers who opted into map visibility ────────────────────────
    private fun loadFreelancersOnMap() {
        _uiState.update { it.copy(isLoading = true) }
        db.child("users")
            .orderByChild("showOnMap")
            .equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profiles = snapshot.children.mapNotNull { child ->
                        child.getValue(UserProfile::class.java)
                    }
                    _uiState.update { it.copy(freelancers = profiles, isLoading = false) }
                }
                override fun onCancelled(error: DatabaseError) {
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            })
    }

    // ── Load jobs that have location set ─────────────────────────────────────
    private fun loadJobsOnMap() {
        db.child("jobs")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jobs = snapshot.children
                        .mapNotNull { it.getValue(Job::class.java) }
                        .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                    _uiState.update { it.copy(jobs = jobs) }
                }
                override fun onCancelled(error: DatabaseError) {
                    _uiState.update { it.copy(error = error.message) }
                }
            })
    }

    // ── UI Event handlers ─────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onWorkTypeChange(type: String) {
        _uiState.update { it.copy(workTypeFilter = type) }
    }

    fun onSkillToggle(skill: String) {
        _uiState.update { state ->
            val skills = state.selectedSkills.toMutableSet()
            if (skill in skills) skills.remove(skill) else skills.add(skill)
            state.copy(selectedSkills = skills)
        }
    }

    fun onRadiusChange(km: Int) {
        _uiState.update { it.copy(radiusKm = km) }
    }

    fun onSelectFreelancer(profile: UserProfile?) {
        _uiState.update { it.copy(selectedFreelancer = profile) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Haversine distance formula ────────────────────────────────────────────
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}