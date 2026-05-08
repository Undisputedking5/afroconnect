package com.example.africonnect.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.africonnect.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

class ProfileViewModel : ViewModel() {

    private val db      = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar

    private val _isUploadingPortfolio = MutableStateFlow(false)
    val isUploadingPortfolio: StateFlow<Boolean> = _isUploadingPortfolio

    // ── Load profile — works for both CLIENT and FREELANCER ──────────────────
    fun loadProfile(uid: String? = null) {
        val targetUid = uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isLoading.value = true

        db.child("users").child(targetUid).get()
            .addOnSuccessListener { snapshot ->
                val portfolioList = snapshot.child("portfolioImages").children
                    .mapNotNull { it.getValue(String::class.java) }
                val skillsList = snapshot.child("skills").children
                    .mapNotNull { it.getValue(String::class.java) }

                val profile = UserProfile(
                    uid             = targetUid,
                    name            = snapshot.child("name").getValue(String::class.java) ?: "",
                    email           = snapshot.child("email").getValue(String::class.java) ?: "",
                    role            = snapshot.child("role").getValue(String::class.java) ?: "",
                    title           = snapshot.child("title").getValue(String::class.java) ?: "",
                    location        = snapshot.child("location").getValue(String::class.java) ?: "",
                    rating          = snapshot.child("rating").getValue(Double::class.java) ?: 0.0,
                    reviews         = snapshot.child("reviews").getValue(Int::class.java) ?: 0,
                    experience      = snapshot.child("experience").getValue(String::class.java) ?: "",
                    hourlyRate      = snapshot.child("hourlyRate").getValue(String::class.java) ?: "",
                    projects        = snapshot.child("projects").getValue(String::class.java) ?: "",
                    about           = snapshot.child("about").getValue(String::class.java) ?: "",
                    skills          = skillsList,
                    github          = snapshot.child("github").getValue(String::class.java) ?: "",
                    instagram       = snapshot.child("instagram").getValue(String::class.java) ?: "",
                    linkedin        = snapshot.child("linkedin").getValue(String::class.java) ?: "",
                    dribbble        = snapshot.child("dribbble").getValue(String::class.java) ?: "",
                    avatarUrl       = snapshot.child("avatarUrl").getValue(String::class.java) ?: "",
                    portfolioImages = portfolioList,
                    showOnMap       = snapshot.child("showOnMap").getValue(Boolean::class.java) ?: false,
                    workType        = snapshot.child("workType").getValue(String::class.java) ?: "Remote",
                    latitude        = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
                    longitude       = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0,
                    // Client fields
                    company         = snapshot.child("company").getValue(String::class.java) ?: "",
                    projectsPosted  = snapshot.child("projectsPosted").getValue(Int::class.java) ?: 0,
                    totalSpent      = snapshot.child("totalSpent").getValue(String::class.java) ?: "",
                    memberSince     = snapshot.child("memberSince").getValue(String::class.java) ?: "",
                )
                _profile.value   = profile
                _isLoading.value = false
            }
            .addOnFailureListener {
                Log.e("ProfileVM", "loadProfile failed: ${it.message}")
                _isLoading.value = false
            }
    }

    // ── Save FREELANCER profile fields ────────────────────────────────────────
    fun saveFreelancerProfile(
        profile: UserProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _saveState.value = SaveState.Loading

        val data = mapOf(
            "name"       to profile.name,
            "title"      to profile.title,
            "location"   to profile.location,
            "experience" to profile.experience,
            "hourlyRate" to profile.hourlyRate,
            "projects"   to profile.projects,
            "about"      to profile.about,
            "skills"     to profile.skills,
            "github"     to profile.github,
            "instagram"  to profile.instagram,
            "linkedin"   to profile.linkedin,
            "dribbble"   to profile.dribbble,
            "showOnMap"  to profile.showOnMap,
            "workType"   to profile.workType
        )

        db.child("users").child(uid).updateChildren(data)
            .addOnSuccessListener {
                _saveState.value = SaveState.Success
                _profile.value = profile
                onSuccess()
            }
            .addOnFailureListener {
                _saveState.value = SaveState.Error(it.message ?: "Failed to save")
                onError(it.message ?: "Failed to save profile")
            }
    }

    // ── Save CLIENT profile fields ────────────────────────────────────────────
    fun saveClientProfile(
        profile: UserProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _saveState.value = SaveState.Loading

        val data = mapOf(
            "name"     to profile.name,
            "company"  to profile.company,
            "location" to profile.location,
            "about"    to profile.about,
            "linkedin" to profile.linkedin,
            "email"    to profile.email
        )

        db.child("users").child(uid).updateChildren(data)
            .addOnSuccessListener {
                _saveState.value = SaveState.Success
                _profile.value = profile
                onSuccess()
            }
            .addOnFailureListener {
                _saveState.value = SaveState.Error(it.message ?: "Failed to save")
                onError(it.message ?: "Failed to save profile")
            }
    }

    // ── Upload avatar to Firebase Storage (proper URL, not local path) ────────
    fun uploadAvatar(uri: Uri, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onError("Not logged in")
            return
        }
        _isUploadingAvatar.value = true

        viewModelScope.launch {
            try {
                val ref = storage.child("avatars/$uid.jpg")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                db.child("users").child(uid).child("avatarUrl")
                    .setValue(downloadUrl).await()

                _profile.value = _profile.value?.copy(avatarUrl = downloadUrl)
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileVM", "Avatar upload failed: ${e.message}")
                onError(e.message ?: "Upload failed")
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    // ── Upload portfolio image to Firebase Storage ────────────────────────────
    fun uploadPortfolioImage(
        uri: Uri,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isUploadingPortfolio.value = true

        viewModelScope.launch {
            try {
                val fileName = "portfolio_${uid}_${System.currentTimeMillis()}.jpg"
                val ref = storage.child("portfolio/$uid/$fileName")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                val currentList = _profile.value?.portfolioImages
                    ?.toMutableList() ?: mutableListOf()
                currentList.add(downloadUrl)

                db.child("users").child(uid).child("portfolioImages")
                    .setValue(currentList).await()

                _profile.value = _profile.value?.copy(portfolioImages = currentList)
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileVM", "Portfolio upload failed: ${e.message}")
                onError(e.message ?: "Upload failed")
            } finally {
                _isUploadingPortfolio.value = false
            }
        }
    }

    // ── Delete portfolio image ────────────────────────────────────────────────
    fun deletePortfolioImage(imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete().await()
                val updatedList = _profile.value?.portfolioImages
                    ?.filter { it != imageUrl } ?: emptyList()
                db.child("users").child(uid).child("portfolioImages")
                    .setValue(updatedList).await()
                _profile.value = _profile.value?.copy(portfolioImages = updatedList)
            } catch (e: Exception) {
                Log.e("ProfileVM", "Delete portfolio failed: ${e.message}")
            }
        }
    }

    // ── Map visibility toggle ─────────────────────────────────────────────────
    fun updateMapVisibility(showOnMap: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("users").child(uid).child("showOnMap").setValue(showOnMap)
        _profile.value = _profile.value?.copy(showOnMap = showOnMap)
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

sealed class SaveState {
    object Idle    : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}