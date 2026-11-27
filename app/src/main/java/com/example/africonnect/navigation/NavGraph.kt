package com.example.africonnect.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.africonnect.data.Repo
import com.example.africonnect.screens.*
import com.example.africonnect.ui.components.LoadingBox
import com.example.africonnect.ui.theme.Sunset
import com.example.africonnect.util.Prefs
import com.example.africonnect.viewmodel.AppViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val JOB = "job/{id}"
    const val POST_JOB = "post_job?edit={edit}"
    const val PROPOSALS = "proposals/{id}"
    const val PROFILE = "profile/{uid}"
    const val EDIT_PROFILE = "edit_profile"
    const val CHAT = "chat/{uid}"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val SAVED = "saved"
    const val APPLICATIONS = "applications"
    const val MAP = "map"

    fun job(id: String) = "job/$id"
    fun postJob(editId: String = "") = "post_job?edit=$editId"
    fun proposals(id: String) = "proposals/$id"
    fun profile(uid: String) = "profile/$uid"
    fun chat(uid: String) = "chat/$uid"
}

/** Navigation actions handed to screens so they don't depend on NavController directly. */
class Nav(private val nav: NavHostController) {
    fun back() { nav.popBackStack() }
    fun job(id: String) = nav.navigate(Routes.job(id)) { launchSingleTop = true }
    fun postJob(editId: String = "") = nav.navigate(Routes.postJob(editId))
    fun proposals(id: String) = nav.navigate(Routes.proposals(id))
    fun profile(uid: String) = nav.navigate(Routes.profile(uid)) { launchSingleTop = true }
    fun editProfile() = nav.navigate(Routes.EDIT_PROFILE)
    fun chat(uid: String) = nav.navigate(Routes.chat(uid)) { launchSingleTop = true }
    fun settings() = nav.navigate(Routes.SETTINGS)
    fun notifications() = nav.navigate(Routes.NOTIFICATIONS)
    fun saved() = nav.navigate(Routes.SAVED)
    fun applications() = nav.navigate(Routes.APPLICATIONS)
    fun map() = nav.navigate(Routes.MAP)
    fun register() = nav.navigate(Routes.REGISTER)
    fun login() = nav.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true } }
    fun toMain() = nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
    fun toLogin() = nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
}

@Composable
fun NavGraph(navController: NavHostController, app: AppViewModel) {
    val context = LocalContext.current
    val nav = remember(navController) { Nav(navController) }
    val start = remember {
        when {
            !Prefs.onboardingDone(context) -> Routes.ONBOARDING
            Repo.auth.currentUser != null -> Routes.MAIN
            else -> Routes.LOGIN
        }
    }

    // If the session ends anywhere (logout, account deleted), return to login.
    val uid by app.uid.collectAsState()
    LaunchedEffect(uid) {
        val route = navController.currentDestination?.route
        if (uid.isBlank() && route != null && route !in setOf(Routes.ONBOARDING, Routes.LOGIN, Routes.REGISTER)) {
            nav.toLogin()
        }
    }

    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = { slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { slideOutHorizontally(tween(240)) { it / 6 } + fadeOut(tween(200)) },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                Prefs.setOnboardingDone(context)
                navController.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.LOGIN) { LoginScreen(onRegister = nav::register, onSuccess = nav::toMain) }
        composable(Routes.REGISTER) { RegisterScreen(onLogin = nav::login, onSuccess = nav::toMain) }
        composable(Routes.MAIN) { MainShell(app, nav) }
        composable(Routes.JOB, listOf(navArgument("id") { type = NavType.StringType })) {
            JobDetailScreen(it.arguments?.getString("id").orEmpty(), app, nav)
        }
        composable(
            Routes.POST_JOB,
            listOf(navArgument("edit") { type = NavType.StringType; defaultValue = "" })
        ) { PostJobScreen(it.arguments?.getString("edit").orEmpty(), app, nav) }
        composable(Routes.PROPOSALS, listOf(navArgument("id") { type = NavType.StringType })) {
            ProposalsScreen(it.arguments?.getString("id").orEmpty(), app, nav)
        }
        composable(Routes.PROFILE, listOf(navArgument("uid") { type = NavType.StringType })) {
            ProfileScreen(it.arguments?.getString("uid").orEmpty(), app, nav)
        }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen(app, nav) }
        composable(Routes.CHAT, listOf(navArgument("uid") { type = NavType.StringType })) {
            ChatScreen(it.arguments?.getString("uid").orEmpty(), app, nav)
        }
        composable(Routes.SETTINGS) { SettingsScreen(app, nav) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(app, nav) }
        composable(Routes.SAVED) { SavedJobsScreen(app, nav) }
        composable(Routes.APPLICATIONS) { ApplicationsScreen(app, nav) }
        composable(Routes.MAP) { MapScreen(app, nav, showBack = true) }
    }
}

private data class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Composable
private fun MainShell(app: AppViewModel, nav: Nav) {
    val me by app.me.collectAsState()
    val unread by app.unreadMessages.collectAsState()
    val user = me
    if (user == null) {
        Box(Modifier.fillMaxSize()) { LoadingBox(Modifier.fillMaxSize()) }
        return
    }
    val tabs = if (user.isClient) listOf(
        Tab("Home", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
        Tab("Talent", Icons.Outlined.Groups, Icons.Filled.Groups),
        Tab("Messages", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
        Tab("Profile", Icons.Outlined.Person, Icons.Filled.Person),
    ) else listOf(
        Tab("Discover", Icons.Outlined.Explore, Icons.Filled.Explore),
        Tab("Nearby", Icons.Outlined.Map, Icons.Filled.Map),
        Tab("Messages", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
        Tab("Profile", Icons.Outlined.Person, Icons.Filled.Person),
    )
    var selected by rememberSaveable(user.role) { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = {
                            BadgedBox(badge = {
                                if (tab.label == "Messages" && unread > 0) Badge(containerColor = Sunset) { Text("$unread") }
                            }) { Icon(if (selected == i) tab.selectedIcon else tab.icon, tab.label) }
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Crossfade(targetState = selected, animationSpec = tween(200), label = "tab") { tab ->
                when (tab) {
                    0 -> if (user.isClient) ClientHomeScreen(user, app, nav) else DiscoverScreen(user, app, nav)
                    1 -> if (user.isClient) TalentScreen(app, nav) else MapScreen(app, nav, showBack = false)
                    2 -> MessagesScreen(app, nav)
                    else -> ProfileScreen(user.uid, app, nav, isTab = true)
                }
            }
        }
    }
}
