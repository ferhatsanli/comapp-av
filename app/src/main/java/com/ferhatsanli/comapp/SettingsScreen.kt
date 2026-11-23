package com.ferhatsanli.comapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

/**
 * Where the Settings sheet is: [progress] 0 = closed, 1 = open. A finger moves it directly
 * (swiping up on the bottom bar, or down on the sheet), so the main screen shows above the sheet
 * while it moves; letting go settles it open or closed by position and speed.
 */
@Stable
internal class SettingsSheetState(private val scope: CoroutineScope, initiallyOpen: Boolean) {
    var progress by mutableFloatStateOf(if (initiallyOpen) 1f else 0f)
        private set

    /** Where the sheet is heading: true once it is opening or open. */
    var targetOpen by mutableStateOf(initiallyOpen)
        private set

    /** The sheet's travel: the height of the area it covers, measured by [SettingsSheet]. */
    internal var heightPx by mutableFloatStateOf(0f)

    private var settling: Job? = null

    val visible: Boolean get() = progress > 0f || targetOpen

    fun open() = settle(open = true)

    fun close() = settle(open = false)

    /** Moves the sheet with a finger; [deltaPx] > 0 is downwards (closing). */
    fun dragBy(deltaPx: Float) {
        if (heightPx <= 0f) return
        settling?.cancel()
        progress = (progress - deltaPx / heightPx).coerceIn(0f, 1f)
    }

    /** The finger let go at [velocityPx] px/s (> 0 downwards): open or close from here. */
    fun release(velocityPx: Float) {
        val open = when {
            velocityPx < -FLING_PX_PER_SECOND -> true
            velocityPx > FLING_PX_PER_SECOND -> false
            else -> progress > 0.5f
        }
        settle(open, velocityPx)
    }

    private fun settle(open: Boolean, velocityPx: Float = 0f) {
        targetOpen = open
        settling?.cancel()
        val target = if (open) 1f else 0f
        val velocity = if (heightPx > 0f) -velocityPx / heightPx else 0f
        settling = scope.launch {
            animate(progress, target, velocity, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                progress = value.coerceIn(0f, 1f)
            }
        }
    }

    private companion object {
        const val FLING_PX_PER_SECOND = 1200f
    }
}

@Composable
internal fun rememberSettingsSheetState(initiallyOpen: Boolean = false): SettingsSheetState {
    val scope = rememberCoroutineScope()
    return remember { SettingsSheetState(scope, initiallyOpen) }
}

/**
 * The bottom bar's swipe-up: the sheet follows the finger from the first pixel. Taps still go
 * to the tabs underneath; only a vertical drag (past the touch slop) is taken.
 */
internal fun Modifier.swipeUpToOpen(sheet: SettingsSheetState): Modifier = composed {
    draggable(
        state = rememberDraggableState { delta -> sheet.dragBy(delta) },
        orientation = Orientation.Vertical,
        onDragStopped = { velocity -> sheet.release(velocity) },
    )
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

/**
 * Settings, sliding up from under the bottom bar over the whole screen. It is drawn where the
 * finger puts it, with the main screen dimmed a little above it; tapping that part closes it.
 */
@Composable
internal fun SettingsSheet(state: SettingsSheetState, model: SettingsModel) {
    BackHandler(enabled = state.targetOpen, onBack = state::close)
    val palette = LocalGlassPalette.current
    val scroll = rememberScrollState()
    // A swipe down on the list itself closes the sheet once the list is at its top.
    val closeFromList = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Pushing back up while the sheet is partly pulled down moves the sheet first.
                if (available.y < 0f && state.progress < 1f && source == NestedScrollSource.UserInput) {
                    state.dragBy(available.y)
                    return available
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    state.dragBy(available.y)
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (state.progress < 1f) {
                    state.release(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    // Always laid out (it takes no touches of its own) so the sheet knows its travel before it
    // is first pulled up.
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { state.heightPx = it.height.toFloat() },
    ) {
        if (!state.visible) return@Box
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = state.progress }
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(interactionSource = null, indication = null, onClickLabel = "Close settings") { state.close() },
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = (1f - state.progress) * size.height }
                .background(palette.baseMid.copy(alpha = 0.97f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(Modifier.fillMaxSize()) {
                // The handle and title: the sheet follows a finger dragging them down.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .draggable(
                            state = rememberDraggableState { delta -> state.dragBy(delta) },
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> state.release(velocity) },
                        )
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
                        GlassIconAction(Icons.Filled.Close, "Close settings", state::close)
                    }
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(closeFromList)
                        .verticalScroll(scroll)
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
