package com.example.africonnect.viewmodel


import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.africonnect.model.UserRole
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    private val _userRole = MutableStateFlow("")
    val userRole: StateFlow<String> = _userRole
    val authState: StateFlow<AuthState> = _authState

    fun registerWithEmail(email: String, password: String,role: UserRole) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Save user to Realtime Database
                val database = FirebaseDatabase.getInstance().reference
                val user = mapOf(
                    "email" to email,
                    "role" to role.name,
                    "createdAt" to System.currentTimeMillis()
                )
                database.child("users").child(uid).setValue(user)
                    .addOnSuccessListener {
                        _authState.value = AuthState.Success
                    }
                    .addOnFailureListener {
                        _authState.value = AuthState.Error(it.message ?: "Failed to save user")
                    }
            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Registration failed")
            }

    }
    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
        _userRole.value = ""
    }
    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                FirebaseDatabase.getInstance().reference
                    .child("users").child(uid).child("role")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val role = snapshot.getValue(String::class.java) ?: "FREELANCER"
                        _userRole.value = role
                        _authState.value = AuthState.Success
                    }
                    .addOnFailureListener {
                        _authState.value = AuthState.Error(it.message ?: "Login failed")
                    }


            }
            .addOnFailureListener {
                _authState.value = AuthState.Error(it.message ?: "Login failed")
            }

    }
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val isNewUser = result.additionalUserInfo?.isNewUser ?: false
                if (isNewUser) {
                    // Save new Google user to database
                    val database = FirebaseDatabase.getInstance().reference
                    val user = mapOf(
                        "email" to (result.user?.email ?: ""),
                        "name" to (result.user?.displayName ?: ""),
                        "role" to _userRole.value.ifEmpty { "FREELANCER" },
                        "createdAt" to System.currentTimeMillis()
                    )
                    database.child("users").child(uid).setValue(user)
                }
                _authState.value = AuthState.Success
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Google sign-in failed")
            }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}