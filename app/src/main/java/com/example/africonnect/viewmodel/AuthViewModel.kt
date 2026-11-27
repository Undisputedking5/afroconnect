package com.example.africonnect.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.africonnect.data.Repo
import com.example.africonnect.model.Roles
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
    data class Info(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun clear() { _state.value = AuthState.Idle }

    fun register(name: String, email: String, password: String, confirm: String, role: String) {
        val e = email.trim()
        when {
            name.isBlank() -> return fail("Please enter your full name")
            !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> return fail("Enter a valid email address")
            password.length < 6 -> return fail("Password must be at least 6 characters")
            password != confirm -> return fail("Passwords don't match")
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = Repo.auth.createUserWithEmailAndPassword(e, password).await().user
                    ?: return@launch fail("Registration failed")
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())
                withTimeoutOrNull(20_000) {
                    Repo.db.child("users").child(user.uid).setValue(
                        mapOf(
                            "name" to name.trim(),
                            "email" to e,
                            "role" to role,
                            "createdAt" to ServerValue.TIMESTAMP,
                            "isAvailable" to true,
                            "workType" to "Remote",
                        )
                    ).await()
                } ?: return@launch fail("Account created, but we couldn't reach the database. Check your connection and sign in again.")
                _state.value = AuthState.Success(role)
            } catch (t: Throwable) {
                fail(friendly(t))
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return fail("Enter your email and password")
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = Repo.auth.signInWithEmailAndPassword(email.trim(), password).await().user
                    ?: return@launch fail("Login failed")
                val snap = withTimeoutOrNull(20_000) { Repo.db.child("users").child(user.uid).get().await() }
                    ?: return@launch fail("Couldn't reach the database. Check your connection and try again.")
                if (!snap.exists()) {
                    // Account exists in Auth but its profile was never written — repair it.
                    Repo.db.child("users").child(user.uid).setValue(
                        mapOf(
                            "name" to (user.displayName ?: ""), "email" to (user.email ?: ""),
                            "role" to Roles.FREELANCER, "createdAt" to ServerValue.TIMESTAMP,
                        )
                    ).await()
                }
                val role = snap.child("role").getValue(String::class.java) ?: Roles.FREELANCER
                _state.value = AuthState.Success(role)
            } catch (t: Throwable) {
                fail(friendly(t))
            }
        }
    }

    fun resetPassword(email: String) {
        val e = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) return fail("Enter your email above first")
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                Repo.auth.sendPasswordResetEmail(e).await()
                _state.value = AuthState.Info("Password reset link sent to $e")
            } catch (t: Throwable) {
                fail(friendly(t))
            }
        }
    }

    private fun fail(msg: String) { _state.value = AuthState.Error(msg) }

    companion object {
        fun friendly(t: Throwable): String = when (t) {
            is FirebaseNetworkException -> "No internet connection. Check your network and try again."
            is FirebaseAuthWeakPasswordException -> "Password is too weak — use at least 6 characters."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Sign in instead."
            is FirebaseAuthInvalidUserException -> "No account found with that email."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            else -> t.message ?: "Something went wrong. Please try again."
        }
    }
}
