package com.example.africonnect.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext
import com.example.africonnect.screens.*
import com.example.africonnect.viewmodel.AuthViewModel
import com.example.africonnect.viewmodel.AuthState
import com.example.africonnect.viewmodel.ProfileViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.africonnect.viewmodel.JobViewModel
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val ONBOARDING          = "onboarding"
    const val REGISTER            = "register"
    const val LOGIN               = "login"
    const val LOADING             = "loading"

    const val CLIENT_HOME         = "client_home"
    const val FREELANCER_HOME     = "freelancer_home"

    const val POST_JOB            = "post_job"
    const val JOB_DETAIL          = "job_detail/{jobId}"
    fun jobDetail(jobId: String)  = "job_detail/$jobId"

    const val PROFILE             = "profile"
    const val CLIENT_PROFILE      = "client_profile/{uid}"
    fun clientProfile(uid: String) = "client_profile/$uid"

    const val FREELANCER_PROFILE      = "freelancer_profile"
    const val FREELANCER_PROFILE_VIEW = "freelancer_profile/{uid}"
    fun freelancerProfile(uid: String) = "freelancer_profile/$uid"

    const val EDIT_PROFILE        = "edit_profile"
    const val MAP                 = "map"
    const val MESSAGES            = "messages"
    const val CHAT                = "chat/{otherUserId}/{userName}"
    fun chatRoute(otherUserId: String, userName: String) = "chat/$otherUserId/$userName"

    const val SETTINGS            = "settings"

}

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current

    val onboardingComplete = remember {
        context.getSharedPreferences("africonnect_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_complete", false)
    }

    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    val startDestination = remember {
        when {
            !onboardingComplete -> Routes.ONBOARDING
            isLoggedIn          -> Routes.LOADING
            else                -> Routes.REGISTER
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Loading ───────────────────────────────────────────────────────────
        composable(Routes.LOADING) {
            LoadingScreen(
                onClientReady = {
                    navController.navigate(Routes.CLIENT_HOME) {
                        popUpTo(Routes.LOADING) { inclusive = true }
                    }
                },
                onFreelancerReady = {
                    navController.navigate(Routes.FREELANCER_HOME) {
                        popUpTo(Routes.LOADING) { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(Routes.REGISTER) {
            RegisterScreen(
                onSignInClick = { navController.navigate(Routes.LOGIN) },
                onSuccess     = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel()
            val userRole  by authViewModel.userRole.collectAsState()
            val authState by authViewModel.authState.collectAsState()

            LaunchedEffect(authState, userRole) {
                if (authState is AuthState.Success && userRole.isNotEmpty()) {
                    when (userRole) {
                        "CLIENT"     -> navController.navigate(Routes.CLIENT_HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                        "FREELANCER" -> navController.navigate(Routes.FREELANCER_HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
            }

            LoginScreen(
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onSuccess       = {},
                viewModel       = authViewModel
            )
        }

        // ── Client Home ───────────────────────────────────────────────────────
        composable(Routes.CLIENT_HOME) {
            ClientHomeScreen(
                onPostJob       = { navController.navigate(Routes.POST_JOB) },
                onProfile       = { navController.navigate(Routes.PROFILE) },
                onViewApplicant = { uid -> navController.navigate(Routes.freelancerProfile(uid)) }
            )
        }

        // ── Freelancer Home ───────────────────────────────────────────────────
        composable(Routes.FREELANCER_HOME) {
            FreelancerHomeScreen(
                navController = navController,
                onProfile     = { navController.navigate(Routes.FREELANCER_PROFILE) },
                onMap         = { navController.navigate(Routes.MAP) },
                onJobClick    = { jobId -> navController.navigate(Routes.jobDetail(jobId)) },
                isViewedByClient = false
            )
        }

        // ── Post Job ──────────────────────────────────────────────────────────
        composable(Routes.POST_JOB) {
            PostJobScreen(
                onJobPosted = {
                    navController.navigate(Routes.CLIENT_HOME) {
                        popUpTo(Routes.POST_JOB) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Job Detail ────────────────────────────────────────────────────────
        composable(
            route     = Routes.JOB_DETAIL,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId: String       = backStackEntry.arguments?.getString("jobId") ?: ""
            val jobViewModel: JobViewModel = viewModel()
            JobDetailScreen(
                navController = navController,
                jobViewModel  = jobViewModel,
                jobId         = jobId
            )
        }

        // ── Client Profile — own ──────────────────────────────────────────────
        composable(Routes.PROFILE) {
            val authViewModel: AuthViewModel = viewModel()
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            ClientProfileScreen(
                navController = navController,
                uid           = uid,
                isOwnProfile  = true,
                onEdit        = { navController.navigate(Routes.EDIT_PROFILE) },
                onLogout      = {
                    authViewModel.logout()
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Client Profile — viewed by others ─────────────────────────────────
        composable(
            route     = Routes.CLIENT_PROFILE,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            ClientProfileScreen(
                navController = navController,
                uid           = uid,
                isOwnProfile  = false
            )
        }

        // ── Freelancer Profile — own ──────────────────────────────────────────
        composable(Routes.FREELANCER_PROFILE) {
            val profileViewModel: ProfileViewModel = viewModel()
            val authViewModel: AuthViewModel       = viewModel()
            FreelancerProfileScreen(
                navController    = navController,
                profileViewModel = profileViewModel,
                isOwnProfile     = true,
                onEdit           = { navController.navigate(Routes.EDIT_PROFILE) },
                onLogout         = {
                    authViewModel.logout()
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Freelancer Profile — viewed by client ─────────────────────────────
        composable(
            route     = Routes.FREELANCER_PROFILE_VIEW,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            val profileViewModel: ProfileViewModel = viewModel()
            LaunchedEffect(uid) { profileViewModel.loadProfile(uid) }
            FreelancerProfileScreen(
                navController    = navController,
                profileViewModel = profileViewModel,
                isOwnProfile     = false
            )
        }

        // ── Edit Profile ──────────────────────────────────────────────────────
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack  = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ── Map ───────────────────────────────────────────────────────────────
        composable(Routes.MAP) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            MapScreen(
                navController = navController,
                currentUserId = currentUserId
            )
        }

        // ── Messages ──────────────────────────────────────────────────────────
        composable(Routes.MESSAGES) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            MessagesScreen(
                navController = navController,
                currentUserId = currentUserId
            )
        }

        // ── Chat ──────────────────────────────────────────────────────────────
        composable(
            route     = Routes.CHAT,
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("userName")    { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val userName    = backStackEntry.arguments?.getString("userName")    ?: ""
            ChatScreen(
                navController = navController,
                otherUserId   = otherUserId,
                otherUserName = userName
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                onLogout      = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}