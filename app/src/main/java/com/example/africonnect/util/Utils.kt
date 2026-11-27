package com.example.africonnect.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ── Time ────────────────────────────────────────────────────────────────────
fun timeAgo(ts: Long): String {
    if (ts <= 0) return ""
    val d = System.currentTimeMillis() - ts
    return when {
        d < 60_000 -> "just now"
        d < 3_600_000 -> "${d / 60_000}m ago"
        d < 86_400_000 -> "${d / 3_600_000}h ago"
        d < 7 * 86_400_000L -> "${d / 86_400_000}d ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))
    }
}

fun clockTime(ts: Long): String =
    if (ts <= 0) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

fun inboxTime(ts: Long): String {
    if (ts <= 0) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ts }
    return when {
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) -> clockTime(ts)
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))
    }
}

fun dayLabel(ts: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ts }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val diff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && diff == 0 -> "Today"
        sameYear && diff == 1 -> "Yesterday"
        else -> SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(ts))
    }
}

fun memberSince(ts: Long): String =
    if (ts <= 0) "" else SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(ts))

// ── Money ───────────────────────────────────────────────────────────────────
fun formatMoney(amount: Double, currency: String): String {
    val n = if (amount % 1.0 == 0.0) String.format(Locale.US, "%,.0f", amount)
    else String.format(Locale.US, "%,.2f", amount)
    return "$currency $n"
}

// ── Geo ─────────────────────────────────────────────────────────────────────
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun formatKm(km: Double): String = if (km < 1) "${(km * 1000).roundToInt()} m" else "%.1f km".format(km)

data class GeoPoint(val lat: Double, val lng: Double, val label: String)

/** Caller must hold a location permission. Returns null when location is off. */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): GeoPoint? = runCatching {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
        ?: client.lastLocation.await()
        ?: return null
    GeoPoint(loc.latitude, loc.longitude, placeName(context, loc.latitude, loc.longitude))
}.getOrNull()

@Suppress("DEPRECATION")
suspend fun placeName(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
    runCatching {
        val a = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)?.firstOrNull()
        listOfNotNull(a?.subLocality ?: a?.locality, a?.countryName).joinToString(", ")
    }.getOrDefault("").ifBlank { "%.3f, %.3f".format(lat, lng) }
}

// ── Intents ─────────────────────────────────────────────────────────────────
fun openUrl(context: Context, raw: String) {
    if (raw.isBlank()) return
    val url = if (raw.startsWith("http")) raw else "https://$raw"
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

fun shareText(context: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    runCatching { context.startActivity(Intent.createChooser(i, "Share via")) }
}

fun String.splitSkills(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

// ── App preferences ─────────────────────────────────────────────────────────
object Prefs {
    private const val FILE = "africonnect_prefs"
    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme

    fun init(context: Context) {
        _theme.value = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("theme", "system") ?: "system"
    }

    fun setTheme(context: Context, value: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("theme", value).apply()
        _theme.value = value
    }

    fun onboardingDone(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("onboarding_complete", false)

    fun setOnboardingDone(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean("onboarding_complete", true).apply()
}
