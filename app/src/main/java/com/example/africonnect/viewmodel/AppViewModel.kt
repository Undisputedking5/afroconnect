package com.example.africonnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.africonnect.data.Repo
import com.example.africonnect.model.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Activity-scoped state shared by every screen: the signed-in user, the job feed,
 * saved/applied jobs, inbox and notifications. Each stream restarts when the
 * signed-in user changes, so logging out and in as someone else is clean.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel : ViewModel() {

    private val _uid = MutableStateFlow(Repo.uid)
    val uid: StateFlow<String> = _uid

    private val authListener = FirebaseAuth.AuthStateListener { a ->
        _uid.value = a.currentUser?.uid ?: ""
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        viewModelScope.launch {
            _uid.filter { it.isNotBlank() }.distinctUntilChanged().collect { Repo.setupPresence() }
        }
    }

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    private fun <T> perUser(empty: T, block: (String) -> Flow<T>): StateFlow<T> =
        _uid.flatMapLatest { id ->
            if (id.isBlank()) flowOf(empty)
            else block(id).catch { emit(empty) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, empty)

    /** null while loading or signed out. */
    val me: StateFlow<UserProfile?> = perUser(null) { Repo.observeUser(it) }

    private val _jobsLoaded = MutableStateFlow(false)
    val jobsLoaded: StateFlow<Boolean> = _jobsLoaded

    val jobs: StateFlow<List<Job>> = perUser(emptyList()) {
        Repo.observeAllJobs().map { s ->
            _jobsLoaded.value = true
            s.children.map { Job.from(it) }.sortedByDescending { it.postedAt }
        }
    }

    val myPostedJobs: StateFlow<List<Job>> = combine(jobs, _uid) { list, id ->
        list.filter { it.postedBy == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedIds: StateFlow<Set<String>> = perUser(emptySet()) {
        Repo.observeSaved().map { s -> s.children.mapNotNull { it.key }.toSet() }
    }

    /** jobId -> applied timestamp. Also counts jobs where the old app wrote only applicants/{uid}. */
    val appliedIds: StateFlow<Set<String>> = combine(
        perUser(emptySet()) { Repo.observeMyApplications().map { s -> s.children.mapNotNull { it.key }.toSet() } },
        jobs, _uid
    ) { fromIndex, list, id ->
        fromIndex + list.filter { id in it.applicants }.map { it.id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val conversations: StateFlow<List<Conversation>> = perUser(emptyList()) {
        Repo.observeConversations().map { s ->
            s.children.map { Conversation.from(it) }.sortedByDescending { it.lastMessageTime }
        }
    }

    val unreadMessages: StateFlow<Int> = conversations.map { c -> c.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val notifications: StateFlow<List<AppNotification>> = perUser(emptyList()) {
        Repo.observeNotifications().map { s ->
            s.children.map { AppNotification.from(it) }.sortedByDescending { it.createdAt }
        }
    }

    val unreadNotifications: StateFlow<Int> = notifications.map { n -> n.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val freelancers: StateFlow<List<UserProfile>> = perUser(emptyList()) { id ->
        Repo.observeFreelancers().map { s ->
            s.children.map { UserProfile.from(it) }.filter { it.uid != id }
                .sortedWith(compareByDescending<UserProfile> { it.rating }.thenByDescending { it.completion })
        }
    }

    fun toggleSaved(jobId: String) = Repo.toggleSaved(jobId, jobId in savedIds.value)

    fun logout() {
        Repo.goOffline()
        FirebaseAuth.getInstance().signOut()
    }
}
