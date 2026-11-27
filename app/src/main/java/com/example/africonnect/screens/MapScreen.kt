package com.example.africonnect.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.africonnect.model.Job
import com.example.africonnect.model.UserProfile
import com.example.africonnect.navigation.Nav
import com.example.africonnect.ui.components.*
import com.example.africonnect.ui.theme.Sunset
import com.example.africonnect.ui.theme.Teal
import com.example.africonnect.util.currentLocation
import com.example.africonnect.util.distanceKm
import com.example.africonnect.util.formatKm
import com.example.africonnect.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private sealed interface MapItem {
    val key: String; val lat: Double; val lng: Double; val title: String
    data class J(val job: Job) : MapItem {
        override val key = "j" + job.id; override val lat = job.latitude; override val lng = job.longitude; override val title = job.title
    }
    data class T(val p: UserProfile) : MapItem {
        override val key = "t" + p.uid; override val lat = p.latitude; override val lng = p.longitude; override val title = p.displayName
    }
}

private val NAIROBI = GeoPoint(-1.2921, 36.8219)

// CARTO basemaps (OpenStreetMap data). Free for apps with attribution; OSM's own tile
// servers block third-party apps.
private fun carto(style: String): OnlineTileSourceBase = XYTileSource(
    "carto-$style", 1, 19, 256, ".png",
    arrayOf("a", "b", "c", "d").map { "https://$it.basemaps.cartocdn.com/rastertiles/$style/" }.toTypedArray(),
    "© OpenStreetMap contributors © CARTO"
)
private val CartoLight = carto("voyager")
private val CartoDark = carto("dark_all")

@Composable
fun MapScreen(app: AppViewModel, nav: Nav, showBack: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me by app.me.collectAsState()
    val jobs by app.jobs.collectAsState()
    val freelancers by app.freelancers.collectAsState()
    val dark = MaterialTheme.colorScheme.background.toArgb().let { android.graphics.Color.luminance(it) < 0.3f }

    var mode by remember(me?.role) { mutableStateOf(if (me?.isClient == true) "Talent" else "Jobs") }
    var radius by remember { mutableIntStateOf(0) } // 0 = any
    var center by remember(me?.uid) {
        mutableStateOf(me?.takeIf { it.hasLocation }?.let { GeoPoint(it.latitude, it.longitude) })
    }
    var selected by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); message = null } }

    val items: List<MapItem> = remember(mode, jobs, freelancers, me?.uid) {
        if (mode == "Jobs") jobs.filter { it.isOpen && it.hasLocation && it.postedBy != me?.uid }.map { MapItem.J(it) }
        else freelancers.filter { it.showOnMap && it.hasLocation }.map { MapItem.T(it) }
    }
    val ref = center
    fun dist(i: MapItem) = ref?.let { distanceKm(it.latitude, it.longitude, i.lat, i.lng) }
    val visible = remember(items, radius, ref) {
        items.filter { radius == 0 || (dist(it) ?: 0.0) <= radius }.sortedBy { dist(it) ?: 0.0 }
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setTileSource(CartoLight)
            controller.setZoom(12.0)
            controller.setCenter(center ?: NAIROBI)
            minZoomLevel = 3.0
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) mapView.onResume()
            if (e == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); mapView.onDetach() }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            locating = true
            scope.launch {
                val p = currentLocation(context)
                if (p != null) {
                    center = GeoPoint(p.lat, p.lng)
                    mapView.controller.animateTo(center, 13.0, 600L)
                } else message = "Couldn't get your location. Turn on location services."
                locating = false
            }
        } else message = "Location permission is needed to show what's near you"
    }

    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        val idx = visible.indexOfFirst { it.key == selected }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0)) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    val source = if (dark) CartoDark else CartoLight
                    if (mv.tileProvider.tileSource.name() != source.name()) mv.setTileSource(source)
                    mv.overlays.removeAll { it is Marker }
                    ref?.let { c ->
                        mv.overlays.add(Marker(mv).apply {
                            position = c; title = "You"; icon = dot(mv, android.graphics.Color.parseColor("#2563EB"), 34)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        })
                    }
                    visible.forEach { item ->
                        mv.overlays.add(Marker(mv).apply {
                            position = GeoPoint(item.lat, item.lng)
                            title = item.title
                            val color = if (item is MapItem.J) Sunset.toArgb() else Teal.toArgb()
                            icon = dot(mv, color, if (item.key == selected) 64 else 46)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setOnMarkerClickListener { _, _ -> selected = item.key; true }
                        })
                    }
                    mv.invalidate()
                }
            )

            // ── Top controls ───────────────────────────────────────────────
            Column(Modifier.statusBarsPadding().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBack) {
                        FilledTonalIconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        Spacer(Modifier.width(8.dp))
                    }
                    Surface(shape = MaterialTheme.shapes.extraLarge, shadowElevation = 4.dp, modifier = Modifier.weight(1f)) {
                        SegmentedTabs(listOf("Jobs", "Talent"), mode, { mode = it; selected = null })
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(0, 5, 10, 25, 50)) { km ->
                        FilterChip(
                            selected = radius == km,
                            onClick = {
                                if (km != 0 && ref == null) permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                radius = km
                            },
                            label = { Text(if (km == 0) "Any distance" else "$km km") },
                            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp)
                        )
                    }
                }
            }

            Text(
                "© OpenStreetMap contributors © CARTO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 2.dp)
            )

            // ── Bottom: locate + results ───────────────────────────────────
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                SmallFloatingActionButton(
                    onClick = { permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.align(Alignment.End).padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.MyLocation, "My location", tint = MaterialTheme.colorScheme.primary)
                }
                if (visible.isEmpty()) {
                    Surface(Modifier.padding(12.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large, shadowElevation = 6.dp) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.TravelExplore, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (mode == "Jobs") "No on-site jobs pinned ${if (radius > 0) "within $radius km" else "yet"}. Remote jobs are on the Discover tab."
                                else "No freelancers on the map ${if (radius > 0) "within $radius km" else "yet"}. Freelancers can opt in from Edit profile.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visible, key = { it.key }) { item ->
                            val d = dist(item)?.let(::formatKm)
                            Surface(shape = MaterialTheme.shapes.large, shadowElevation = 6.dp, modifier = Modifier.width(280.dp)) {
                                when (item) {
                                    is MapItem.J -> MapJobCard(item.job, d) { nav.job(item.job.id) }
                                    is MapItem.T -> MapTalentCard(item.p, d) { nav.profile(item.p.uid) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapJobCard(job: Job, distance: String?, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(job.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(job.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.budget.ifBlank { "Negotiable" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                if (distance != null) MetaItem(Icons.Outlined.NearMe, distance)
            }
        }
    }
}

@Composable
private fun MapTalentCard(p: UserProfile, distance: String?, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.avatarUrl, p.displayName, 44.dp, online = p.isOnline)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.title.ifBlank { "Freelancer" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Row { RatingRow(p.rating, p.reviews); if (distance != null) { Spacer(Modifier.width(8.dp)); MetaItem(Icons.Outlined.NearMe, distance) } }
            }
        }
    }
}

/** A round map pin with a white ring. */
private fun dot(mv: MapView, color: Int, sizeDp: Int): BitmapDrawable {
    val sizePx = (sizeDp * mv.resources.displayMetrics.density / 2).toInt().coerceAtLeast(12)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.WHITE
    c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p)
    p.color = color
    c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - sizePx * 0.14f, p)
    return BitmapDrawable(mv.resources, bmp)
}
