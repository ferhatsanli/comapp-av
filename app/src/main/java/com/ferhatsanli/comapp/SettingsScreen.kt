package com.ferhatsanli.comapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ferhatsanli.comapp.ui.glass.GlassButton
import com.ferhatsanli.comapp.ui.glass.GlassCard
import com.ferhatsanli.comapp.ui.glass.GlassColors
import com.ferhatsanli.comapp.ui.glass.GlassIconAction
import com.ferhatsanli.comapp.ui.glass.GlassSectionHeader
import com.ferhatsanli.comapp.ui.glass.GlassSwitchRow
import com.ferhatsanli.comapp.ui.glass.GlassTextField
import com.ferhatsanli.comapp.ui.glass.LocalGlassPalette

/** How far a finger must travel on a grab handle before it opens or closes the sheet. */
internal val SETTINGS_SWIPE_DISTANCE = 40.dp

/**
 * A swipe-up detector for the bottom bar: a short upward swipe calls [onOpen]. Taps still go
 * to the tabs underneath; only a vertical drag is taken.
 */
@Composable
internal fun Modifier.swipeUpToOpen(onOpen: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { SETTINGS_SWIPE_DISTANCE.toPx() }
    return pointerInput(onOpen) {
        var travelled = 0f
        detectVerticalDragGestures(
            onDragStart = { travelled = 0f },
            onVerticalDrag = { change, amount ->
                travelled += amount
                change.consume()
            },
            onDragEnd = { if (travelled < -threshold) onOpen() },
        )
    }
}

/** The small pill at the top of the bottom bar that hints "swipe up for Settings". */
@Composable
internal fun SettingsHandle(onOpen: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clickable(onClickLabel = "Open settings", onClick = onOpen)
            .semantics { contentDescription = "Settings: swipe up or tap" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LocalGlassPalette.current.inkFaint.copy(alpha = 0.55f)),
        )
    }
}

/** What Settings shows and changes; the screen owns the state, the sheet only draws it. */
internal class SettingsModel(
    val favoriteRows: Int,
    val onFavoriteRows: (Int) -> Unit,
    val deviceNameCount: Int,
    /** Saves the names for this computer; calls back with a line saying how it went. */
    val onImportNames: (Map<String, String>, (String) -> Unit) -> Unit,
    val appVersion: String,
    val computerName: String,
    val serverVersion: String?,
    val developerMode: Boolean = false,
    val onDeveloperMode: (Boolean) -> Unit = {},
)

/** Settings, sliding up from under the bottom bar over the whole screen. */
@Composable
internal fun SettingsSheet(visible: Boolean, onClose: () -> Unit, model: SettingsModel) {
    BackHandler(enabled = visible, onBack = onClose)
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160)),
    ) {
        val palette = LocalGlassPalette.current
        val threshold = with(LocalDensity.current) { SETTINGS_SWIPE_DISTANCE.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .background(palette.baseMid.copy(alpha = 0.97f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(Modifier.fillMaxSize()) {
                // The handle and title: a swipe down here closes the sheet again.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(onClose) {
                            var travelled = 0f
                            detectVerticalDragGestures(
                                onDragStart = { travelled = 0f },
                                onVerticalDrag = { change, amount ->
                                    travelled += amount
                                    change.consume()
                                },
                                onDragEnd = { if (travelled > threshold) onClose() },
                            )
                        }
                        .padding(horizontal = 16.dp),
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(palette.inkFaint.copy(alpha = 0.55f)),
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        GlassIconAction(Icons.Filled.Close, "Close settings", onClose)
                    }
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppsSettings(model)
                    DeviceNameSettings(model)
                    DeveloperSettings(model)
                    AboutSettings(model)
                }
            }
        }
    }
}

@Composable
private fun AppsSettings(model: SettingsModel) {
    val palette = LocalGlassPalette.current
    GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Indigo, cornerRadius = 22.dp) {
        Text("Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Favorite rows", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${model.favoriteRows * APPS_PER_ROW} favorites fit (${APPS_PER_ROW} per row).",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted,
                )
            }
            IconButton(
                enabled = model.favoriteRows > FavoriteApps.MIN_ROWS,
                onClick = { model.onFavoriteRows(model.favoriteRows - 1) },
            ) {
                // A minus drawn to match Material's "+" (the core icon set has no minus).
                val tint = if (model.favoriteRows > FavoriteApps.MIN_ROWS) palette.inkMuted else palette.inkFaint
                Canvas(
                    Modifier
                        .size(24.dp)
                        .semantics { contentDescription = "One row less" },
                ) {
                    val y = size.height / 2
                    drawLine(tint, Offset(size.width * 0.21f, y), Offset(size.width * 0.79f, y), 2.dp.toPx())
                }
            }
            Text(
                model.favoriteRows.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .width(24.dp)
                    .semantics { contentDescription = "${model.favoriteRows} rows" },
            )
            IconButton(
                enabled = model.favoriteRows < FavoriteApps.MAX_ROWS,
                onClick = { model.onFavoriteRows(model.favoriteRows + 1) },
            ) { Icon(Icons.Filled.Add, contentDescription = "One row more", tint = palette.inkMuted) }
        }
    }
}

@Composable
private fun DeviceNameSettings(model: SettingsModel) {
    val palette = LocalGlassPalette.current
    var pasted by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val parsed = remember(pasted) { DeviceNames.parse(pasted) }
    GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Neutral, cornerRadius = 22.dp) {
        Text("Device names", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Names for \"On the network\" on ${model.computerName}: ${model.deviceNameCount} saved. " +
                "Paste a list to add many at once, one MAC and name per line " +
                "(\"48:D3:43:4A:1D:60\": \"Modem\" works, so does 48:D3:43:4A:1D:60 = Modem).",
            style = MaterialTheme.typography.bodySmall,
            color = palette.inkMuted,
        )
        GlassSectionHeader("Paste a list")
        GlassTextField(
            value = pasted,
            onValueChange = {
                pasted = it.take(20_000)
                outcome = null
            },
            placeholder = "\"AA:BB:CC:DD:EE:FF\": \"Living Room TV\"",
            singleLine = false,
            minLines = 3,
            maxLines = 8,
            clearDescription = "Clear the list",
            modifier = Modifier.fillMaxWidth(),
        )
        GlassButton(
            text = if (parsed.isEmpty()) "Nothing to import yet" else "Import ${parsed.size} names",
            enabled = parsed.isNotEmpty() && !saving,
            loading = saving,
            accent = GlassColors.Blue,
            onClick = {
                saving = true
                model.onImportNames(parsed) { line ->
                    saving = false
                    outcome = line
                    if (line.startsWith("Saved")) pasted = ""
                }
            },
        )
        outcome?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = palette.inkMuted) }
    }
}

@Composable
private fun DeveloperSettings(model: SettingsModel) {
    GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Amber, cornerRadius = 22.dp) {
        GlassSwitchRow(
            title = "Developer Mode",
            subtitle = "Shows the Console, the Realtime Database and Latest command cards, and the app version.",
            checked = model.developerMode,
            onCheckedChange = model.onDeveloperMode,
            accent = GlassColors.Amber,
        )
    }
}

@Composable
private fun AboutSettings(model: SettingsModel) {
    val palette = LocalGlassPalette.current
    GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Green, cornerRadius = 22.dp) {
        Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("App ${model.appVersion}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Server on ${model.computerName}: " + (model.serverVersion?.let { "v$it" } ?: "version not reported"),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
        )
    }
}

/** Apps per row in the Apps card's grids (Favorites, Recent, All apps). */
internal const val APPS_PER_ROW = 4
