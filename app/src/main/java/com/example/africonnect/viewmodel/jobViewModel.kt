package com.example.africonnect.viewmodel

import androidx.lifecycle.ViewModel
import com.example.africonnect.model.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ── NEW: single job for JobDetailScreen ───────────────────────────────────
    private val _selectedJob = MutableStateFlow<Job?>(null)
    val selectedJob: StateFlow<Job?> = _selectedJob

    private val _applySuccess = MutableStateFlow(false)
    val applySuccess: StateFlow<Boolean> = _applySuccess

    // Add these to JobViewModel

    private val _applyError = MutableStateFlow<String?>(null)
    val applyError: StateFlow<String?> = _applyError

    private val _appliedJobIds = MutableStateFlow<Set<String>>(emptySet())
    val appliedJobIds: StateFlow<Set<String>> = _appliedJobIds


    init {
        fetchJobs()
    }

    // ── Existing: real-time listener for all jobs ─────────────────────────────
    private fun fetchJobs() {
        db.child("jobs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jobList = mutableListOf<Job>()
                for (child in snapshot.children) {
                    val job = child.getValue(Job::class.java)
                    if (job != null) jobList.add(job.copy(id = child.key ?: ""))
                }
                _jobs.value = jobList
                _isLoading.value = false
            }
            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        })
    }

    // ── Existing ──────────────────────────────────────────────────────────────
    fun fetchAllJobs() {
        db.child("jobs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jobList = mutableListOf<Job>()
                for (child in snapshot.children) {
                    val job = child.getValue(Job::class.java)
                    if (job != null) jobList.add(job.copy(id = child.key ?: ""))
                }
                _jobs.value = jobList
                _isLoading.value = false
            }
            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        })
    }

    // ── Existing ──────────────────────────────────────────────────────────────
    fun fetchJobsForClient(clientUid: String) {
        db.child("jobs").orderByChild("postedBy").equalTo(clientUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val jobList = mutableListOf<Job>()
                    for (child in snapshot.children) {
                        val job = child.getValue(Job::class.java)
                        if (job != null) jobList.add(job.copy(id = child.key ?: ""))
                    }
                    _jobs.value = jobList
                    _isLoading.value = false
                }
                override fun onCancelled(error: DatabaseError) {
                    _isLoading.value = false
                }
            })
    }

    // ── Existing ──────────────────────────────────────────────────────────────
    fun postJob(
        title: String,
        rate: String,
        description: String,
        skills: List<String>,
        category: String,
        location: String,
        isUrgent: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val jobRef = db.child("jobs")
        val jobId = jobRef.push().key ?: return

        val job = mapOf(
            "id"          to jobId,
            "title"       to title,
            "rate"        to rate,
            "description" to description,
            "skills"      to skills,
            "category"    to category,
            "location"    to location,
            "isUrgent"    to isUrgent,
            "postedBy"    to uid,
            "rating"      to 0.0,
            "reviews"     to 0,
            "postedAt"    to System.currentTimeMillis()
        )

        jobRef.child(jobId).setValue(job)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Failed to post job") }
    }

    // ── NEW: load single job by ID for JobDetailScreen ────────────────────────
    fun getJobById(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedJob.value = null
            try {
                val snapshot = db.child("jobs").child(jobId).get().await()
                val job = snapshot.getValue(Job::class.java)
                _selectedJob.value = job?.copy(id = snapshot.key ?: jobId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Existing: updated to use callbacks ────────────────────────────────────

    fun applyForJob(
        jobId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _applyError.value = null  // clear previous error
        db.child("jobs").child(jobId)
            .child("applicants").child(uid)
            .setValue(true)
            .addOnSuccessListener {
                _applySuccess.value = true
                _appliedJobIds.value = _appliedJobIds.value + jobId  // optimistic update
                onSuccess()
            }
            .addOnFailureListener {
                val msg = it.message ?: "Failed to apply"
                _applyError.value = msg
                onError(msg)
            }
    }

    fun resetApplyError() {
        _applyError.value = null
    }
    fun resetApplySuccess() {
        _applySuccess.value = false
    }
    fun loadAppliedJobs() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("jobs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val applied = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val jobId = child.key ?: continue
                    if (child.child("applicants").child(uid).exists()) {
                        applied.add(jobId)
                    }
                }
                _appliedJobIds.value = applied
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    // In JobViewModel
    fun hireFreelancer(
        jobId: String,
        jobTitle: String,
        freelancerUid: String,
        freelancerName: String,
        clientUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val updates = mapOf(
            "jobs/$jobId/hiredFreelancerId" to freelancerUid,
            "jobs/$jobId/hiredFreelancerName" to freelancerName,
            "jobs/$jobId/status" to "hired"
        )
        db.updateChildren(updates)
            .addOnSuccessListener {
                // Send system message to freelancer
                val chatId = listOf(clientUid, freelancerUid).sorted().joinToString("_")
                val msgId = db.child("chats/$chatId/messages").push().key ?: return@addOnSuccessListener
                val msg = mapOf(
                    "id" to msgId,
                    "senderId" to "system",
                    "text" to "🎉 Congratulations! You've been hired for \"$jobTitle\".",
                    "timestamp" to System.currentTimeMillis(),
                    "type" to "TEXT",
                    "isRead" to false
                )
                db.child("chats/$chatId/messages/$msgId").setValue(msg)
                // Update conversations node for both users
                val convUpdate = mapOf(
                    "lastMessage" to "🎉 You've been hired for \"$jobTitle\"!",
                    "lastMessageTime" to System.currentTimeMillis()
                )
                db.child("conversations/$freelancerUid/$clientUid").updateChildren(convUpdate)
                db.child("conversations/$clientUid/$freelancerUid").updateChildren(convUpdate)
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Failed") }
    }

}