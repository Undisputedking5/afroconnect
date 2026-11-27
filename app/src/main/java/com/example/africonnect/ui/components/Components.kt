package com.example.africonnect.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.africonnect.model.Job
import com.example.africonnect.model.JobStatus
import com.example.africonnect.model.UserProfile
import com.example.africonnect.ui.theme.*
import com.example.africonnect.util.timeAgo

val BrandGradient = Brush.linearGradient(listOf(TealDark, Teal, Color(0xFF14A08A)))
val SunsetGradient = Brush.linearGradient(listOf(Sunset, Color(0xFFF59E0B)))

private val avatarColors = listOf(
    Color(0xFF0E7C6B), Color(0xFFE8621A), Color(0xFF7C3AED), Color(0xFF2563EB),
    Color(0xFFDB2777), Color(0xFFCA8A04), Color(0xFF059669)
)

@Composable
fun Avatar(
    url: String,
    name: String,
    size: Dp = 48.dp,
    online: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size)) {
        val initials = name.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
        val bg = avatarColors[(name.hashCode() and 0x7fffffff) % avatarColors.size]
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
            if (url.isNotBlank()) {
                AsyncImage(
                    model = url, contentDescription = name, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        if (online) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(size * 0.28f).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface).padding(2.dp)
                    .clip(CircleShape).background(LocalExtraColors.current.online)
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.primary,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = container),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
        } else {
            if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    supporting: String? = null,
    prefix: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) ({ Text(placeholder) }) else null,
        leadingIcon = leadingIcon?.let { { Icon(it, null) } },
        trailingIcon = if (isPassword) ({
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Toggle password")
            }
        }) else null,
        prefix = prefix?.let { { Text(it) } },
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        singleLine = singleLine && minLines == 1,
        minLines = minLines,
        supportingText = supporting?.let { { Text(it) } },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(13.dp), tint = content); Spacer(Modifier.width(4.dp)) }
        Text(text, color = content, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun StatusPill(status: String) {
    val cs = MaterialTheme.colorScheme
    val (label, bg, fg) = when (status) {
        JobStatus.OPEN -> Triple("Open", cs.primaryContainer, cs.onPrimaryContainer)
        JobStatus.HIRED -> Triple("In progress", cs.tertiaryContainer, cs.onTertiaryContainer)
        JobStatus.COMPLETED -> Triple("Completed", Color(0xFFDCFCE7), Color(0xFF14532D))
        else -> Triple("Closed", cs.surfaceVariant, cs.onSurfaceVariant)
    }
    Pill(label, container = bg, content = fg)
}

@Composable
fun RatingRow(rating: Double, reviews: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, null, Modifier.size(15.dp), tint = Gold)
        Spacer(Modifier.width(3.dp))
        if (reviews == 0) {
            Text("New", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("%.1f".format(rating), style = MaterialTheme.typography.labelLarge)
            Text(" ($reviews)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun BadgedIconButton(icon: ImageVector, count: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = {
            if (count > 0) Badge(containerColor = Sunset) { Text(if (count > 99) "99+" else count.toString()) }
        }) { Icon(icon, description) }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, border = border, colors = colors) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, border = border, colors = colors) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillChips(skills: List<String>, max: Int = Int.MAX_VALUE, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        skills.take(max).forEach {
            Pill(it, container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), content = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        if (skills.size > max) Pill("+${skills.size - max}")
    }
}

@Composable
fun JobCard(
    job: Job,
    saved: Boolean,
    applied: Boolean,
    onClick: () -> Unit,
    onToggleSave: (() -> Unit)?,
    modifier: Modifier = Modifier,
    distance: String? = null,
) {
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (job.isUrgent) Pill("Urgent", container = Sunset.copy(alpha = 0.14f), content = Sunset, icon = Icons.Filled.Bolt)
                    if (job.category.isNotBlank()) Pill(job.category)
                    if (!job.isOpen) StatusPill(job.status)
                }
                Spacer(Modifier.height(10.dp))
                Text(job.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(job.postedByName.ifBlank { "Client" })
                        if (job.postedAt > 0) append(" · ${timeAgo(job.postedAt)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onToggleSave != null) {
                IconButton(onClick = onToggleSave, modifier = Modifier.offset(x = 8.dp, y = (-8).dp)) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        if (saved) "Unsave" else "Save",
                        tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (job.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                job.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (job.skills.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SkillChips(job.skills, max = 4)
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                job.budget.ifBlank { "Budget negotiable" },
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            MetaItem(if (job.workType == "Remote") Icons.Outlined.Public else Icons.Outlined.Place,
                distance ?: job.workType)
            Spacer(Modifier.width(12.dp))
            if (applied) {
                MetaItem(Icons.Filled.CheckCircle, "Applied", tint = Success)
            } else {
                MetaItem(Icons.Outlined.Groups, "${job.proposalCount}")
            }
        }
    }
}

@Composable
fun MetaItem(icon: ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(15.dp), tint = tint)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}

@Composable
fun TalentCard(p: UserProfile, onClick: () -> Unit, modifier: Modifier = Modifier, distance: String? = null) {
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.avatarUrl, p.displayName, 54.dp, online = p.isOnline)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (p.completedJobs > 0) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, "Has completed jobs", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    p.title.ifBlank { "Freelancer" }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingRow(p.rating, p.reviews)
                    if (p.location.isNotBlank() || distance != null) {
                        Spacer(Modifier.width(10.dp))
                        MetaItem(Icons.Outlined.Place, distance ?: p.location)
                    }
                }
            }
            if (p.hourlyRate.isNotBlank()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(p.hourlyRate, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("per hour", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (p.skills.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SkillChips(p.skills, max = 4)
        }
    }
}

@Composable
fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Column(
        modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large).padding(14.dp)
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = accent)
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = trailing ?: if (value.isNotEmpty()) ({
            IconButton(onClick = { onValueChange("") }) { Icon(Icons.Filled.Close, "Clear") }
        }) else null,
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)),
    )
}

@Composable
fun ErrorBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(message != null, modifier) {
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(8.dp))
            Text(message ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(message != null, modifier) {
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(message ?: "", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SegmentedTabs(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)
    ) {
        options.forEach { opt ->
            val sel = opt == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50))
                    .background(if (sel) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(opt) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    opt, style = MaterialTheme.typography.labelLarge,
                    color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
