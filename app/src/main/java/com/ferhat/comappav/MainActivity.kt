package com.ferhat.comappav

import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.Canvas
import com.ferhat.comappav.ui.glass.GlassContentButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ferhat.comappav.ui.glass.GlassBottomNavItem
import com.ferhat.comappav.ui.glass.GlassButton
import com.ferhat.comappav.ui.glass.GlassCard
import com.ferhat.comappav.ui.glass.GlassChip
import com.ferhat.comappav.ui.glass.GlassColors
import com.ferhat.comappav.ui.glass.GlassCommandCard
import com.ferhat.comappav.ui.glass.GlassFolderRow
import com.ferhat.comappav.ui.glass.GlassIconButton
import com.ferhat.comappav.ui.glass.GlassFieldShape
import com.ferhat.comappav.ui.glass.GlassNavSurface
import com.ferhat.comappav.ui.glass.GlassProgressBar
import com.ferhat.comappav.ui.glass.ChartPoint
import com.ferhat.comappav.ui.glass.GlassLineChart
import com.ferhat.comappav.ui.glass.GlassSwitchRow
import com.ferhat.comappav.ui.glass.GlassScreen
import com.ferhat.comappav.ui.glass.LocalGlassPalette
import com.ferhat.comappav.ui.glass.ReorderableColumn
import com.ferhat.comappav.ui.glass.drawCommandsIcon
import com.ferhat.comappav.ui.glass.drawConsoleIcon
import com.ferhat.comappav.ui.glass.drawDownloadIcon
import com.ferhat.comappav.ui.glass.drawNetworkIcon
import com.ferhat.comappav.ui.glass.drawPowerIcon
import com.ferhat.comappav.ui.glass.drawReorderIcon
import com.ferhat.comappav.ui.glass.glassTextFieldColors
import com.ferhat.comappav.ui.theme.ComAppAVTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private data class SleepRequest(val minutes: Int?)

private data class NetworkDevice(val mac: String, val ip: String, val name: String)

/**
 * What a card is for, which decides where it goes in landscape:
 * - [INTERACTIVE]: something to tap, type into or switch (a command, a setting). Shown in the
 *   left pane, in the open category.
 * - [INFO]: only shows information. Never in the left pane: the category lists a button for
 *   it there, and the card itself opens in the right pane, under the connection card.
 *
 * Every new card gets one of these - decide it when the card is made.
 */
private enum class CardKind { INTERACTIVE, INFO }

/** Every card that can be moved around in reorder mode; the names are what gets saved. */
private enum class CardId(val kind: CardKind) {
    PING(CardKind.INTERACTIVE),
    SPEAK(CardKind.INTERACTIVE),
    SLEEP(CardKind.INTERACTIVE),
    SHUTDOWN(CardKind.INTERACTIVE),
    POWER_MODE(CardKind.INTERACTIVE),
    SCAN(CardKind.INTERACTIVE),
    DEVICES(CardKind.INFO),
    DOWNLOAD_ADD(CardKind.INTERACTIVE),
    DOWNLOAD_LIST(CardKind.INFO),
    DOWNLOAD_LIMIT(CardKind.INTERACTIVE),
    CONSOLE(CardKind.INTERACTIVE), // the Console category has its own tool list in landscape
    TAILSCALE(CardKind.INTERACTIVE),
    SYSTEM_UPDATE(CardKind.INTERACTIVE),
    POWER_HISTORY(CardKind.INFO),
}

/**
 * Card groups, in their default order. The user can reorder both the categories and the cards
 * inside each one; [cards] is only the starting order.
 */
private enum class Category(
    val label: String,
    val contents: String,
    val accent: Color,
    val cards: List<CardId>,
    val icon: DrawScope.(Color) -> Unit,
) {
    COMMANDS(
        "Commands", "Speak · Update server system", GlassColors.Blue, listOf(CardId.SPEAK, CardId.SYSTEM_UPDATE),
        { tint -> drawCommandsIcon(tint) },
    ),
    POWER(
        "Power",
        "Sleep · Shutdown · Power mode · History",
        GlassColors.Amber,
        listOf(CardId.SLEEP, CardId.SHUTDOWN, CardId.POWER_MODE, CardId.POWER_HISTORY),
        { tint -> drawPowerIcon(tint) },
    ),
    NETWORK(
        "Network",
        "Ping · Scan · Tailscale · Devices",
        GlassColors.Neutral,
        listOf(CardId.PING, CardId.SCAN, CardId.TAILSCALE, CardId.DEVICES),
        { tint -> drawNetworkIcon(tint) },
    ),
    DOWNLOADS(
        "Downloads",
        "Torrents · links · speed limit",
        GlassColors.Indigo,
        listOf(CardId.DOWNLOAD_ADD, CardId.DOWNLOAD_LIST, CardId.DOWNLOAD_LIMIT),
        { tint -> drawDownloadIcon(tint) },
    ),
    CONSOLE(
        "Console", "App log · errors · crashes", GlassColors.Green, listOf(CardId.CONSOLE),
        { tint -> drawConsoleIcon(tint) },
    ),
}

/**
 * Tools inside the Console folder in landscape. The list sits on the left; the selected tool's
 * window fills the right-hand pane. Add a tool here (and its window in the right pane) to have
 * it show up under Console.
 */
private enum class ConsoleTool(val label: String, val description: String) {
    LOGS("Logs", "Recent events, errors and crashes"),
}

/**
 * What's open in the landscape right pane besides the usual info cards: a Console tool (which
 * takes the place of the info cards), or one info card of the open category (on top of them).
 * The connection card stays above all of it.
 */
private sealed interface RightPaneView {
    data object Logs : RightPaneView
    data class Info(val card: CardId) : RightPaneView
}

/** Where the Speak language choice is kept on the phone. */
private const val SPEECH_PREFS = "speech"
private const val SPEECH_LANGUAGE_KEY = "language"

/** How long to wait for the keyboard to slide in before scrolling the Speak field above it. */
private const val SPEAK_FIELD_SCROLL_DELAY_MILLIS = 300L

/** Key of the category order in [CardOrderStore]; each category's cards are saved under its name. */
private const val CATEGORY_ORDER_KEY = "categories"

/** One entry of /downloads/latest (see the agent's downloads.py). */
private data class DownloadItem(
    val id: String,
    val name: String,
    val kind: String,
    val state: String,
    val totalBytes: Long,
    val doneBytes: Long,
    val downBps: Long,
    val upBps: Long,
    val etaSec: Long?,
    val error: String?,
)

private data class PowerSnapshot(
    val reportedAt: Long?,
    val usagePercent: Double?,
    val tempC: Long?,
    val packageWatts: Double?,
    val coreFreqMhz: List<Long>,
    val batteryPercent: Long?,
    val fanRpm: Long?,
    val ecTempC: Long?,
    val powerMode: String?,
    val tailscale: String?,
)

/** One /power/history sample: CPU usage (%) and package power (W), either may be missing. */
private data class PowerSample(val timeMs: Long, val cpu: Float?, val watts: Float?)

/** The ranges the Power history charts can show. */
private enum class HistoryRange(val label: String, val millis: Long) {
    HOUR("1 h", 60 * 60_000L),
    SIX_HOURS("6 h", 6 * 60 * 60_000L),
    DAY("24 h", 24 * 60 * 60_000L),
}

/** How long the Apply button keeps its light while waiting for the server to pick up the new limit. */
private const val LIMIT_APPLY_WAIT_MILLIS = 30_000L

private const val DATABASE_URL =
    "https://comapp-av-default-rtdb.europe-west1.firebasedatabase.app"

// Rough, all-in Dutch household electricity price (energy tax + VAT included),
// checked 2026-10-27 against the daily average on energievergelijk.nl. This is
// only for a ballpark "what does this cost" estimate next to the live reading,
// not a real tariff — edit this constant if you want a sharper number.
private const val ELECTRICITY_PRICE_EUR_PER_KWH = 0.324

private const val PENDING_WARNING_MILLIS = 15_000L

// Portrait tab swipe: a drag over this share of the screen width, or a fling faster than this,
// switches to the neighbouring tab. While dragging, the cards follow the finger at a reduced
// rate (less at the first/last tab, where there is nothing to switch to).
private const val TAB_SWIPE_DISTANCE_FRACTION = 0.22f
private const val TAB_SWIPE_VELOCITY_DP_PER_S = 400f
private const val TAB_SWIPE_FOLLOW = 0.35f
private const val TAB_SWIPE_FOLLOW_AT_EDGE = 0.12f

/** How many log lines the Console draws; "Copy all" always includes every saved entry. */
private const val CONSOLE_VISIBLE_ENTRIES = 150

/** Running downloads first, finished ones last. */
private fun downloadStateOrder(state: String): Int = when (state) {
    "DOWNLOADING" -> 0
    "SEEDING" -> 1
    "QUEUED" -> 2
    "PAUSED" -> 3
    "FAILED" -> 4
    else -> 5
}

/** The last characters of a Firebase push ID - enough to tell commands apart in the log. */
private fun shortId(id: String): String = id.takeLast(6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppLog.init(this)) {
            AppLog.info("App", "Started v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        }
        enableEdgeToEdge()
        // A rotation recreates the activity with the same intent - it was already handled.
        if (savedInstanceState == null) IncomingDownloads.accept(this, intent)
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        setContent {
            ComAppAVTheme {
                GlassScreen {
                    ComAppScreen(auth = auth, database = database)
                }
            }
        }
    }

    // singleTask: a link shared while the app is already open arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        IncomingDownloads.accept(this, intent)
    }
}

@Composable
private fun ComAppScreen(auth: FirebaseAuth, database: FirebaseDatabase) {
    var user by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (user == null) {
        SignInScreen(auth)
    } else {
        PingScreen(user = user!!, auth = auth, database = database)
    }
}

@Composable
private fun SignInScreen(auth: FirebaseAuth) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = Color.Transparent, contentColor = LocalGlassPalette.current.ink) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalEdgeFade(scrollState)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Blue) {
                Text(
                    "ComApp AV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Sign in with your Firebase Email/Password account to send commands.",
                    color = LocalGlassPalette.current.inkMuted,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = GlassFieldShape,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = GlassFieldShape,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                GlassButton(
                    text = "Sign in",
                    accent = GlassColors.Blue,
                    loading = isLoading,
                    enabled = !isLoading,
                    onClick = {
                        if (email.isBlank() || password.isEmpty()) {
                            error = "Enter your email and password."
                        } else {
                            isLoading = true
                            error = null
                            auth.signInWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        password = ""
                                        AppLog.info("Auth", "Signed in")
                                    } else {
                                        error = task.exception?.localizedMessage
                                            ?: "Sign-in failed. Check the account and connection."
                                        AppLog.error("Auth", "Sign-in failed: $error")
                                    }
                                }
                        }
                    },
                )
            }
            Text(
                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = LocalGlassPalette.current.inkFaint,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PingScreen(user: FirebaseUser, auth: FirebaseAuth, database: FirebaseDatabase) {
    var connected by remember { mutableStateOf<Boolean?>(null) }
    var commandId by remember { mutableStateOf<String?>(null) }
    var commandStatus by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var commandAction by remember { mutableStateOf(PingProtocol.ACTION) }
    var speakText by rememberSaveable { mutableStateOf("") }
    var sleepRequest by remember { mutableStateOf<SleepRequest?>(null) }
    var shutdownRequested by remember { mutableStateOf(false) }
    var signOutRequested by remember { mutableStateOf(false) }
    var requestedPowerMode by remember { mutableStateOf<String?>(null) }
    // Portrait: which bottom-nav tab is active.
    var selectedCategory by rememberSaveable { mutableStateOf(Category.COMMANDS) }
    // How far the tab content is pulled sideways by a swipe that's still in progress.
    val tabSwipeOffset = remember { Animatable(0f) }
    // Landscape: the category "folder" that's open in the left pane, or null for the category
    // list itself. [openProgress] is 1 when that folder is fully on screen and 0 when it's slid
    // away to the right; dragging it with a swipe-back gesture moves it in between.
    var openCategory by rememberSaveable { mutableStateOf<Category?>(null) }
    // Console: show only warnings and errors.
    var consoleErrorsOnly by rememberSaveable { mutableStateOf(false) }
    // Landscape Console folder: the tool whose window is shown in the right pane, if any.
    var consoleTool by rememberSaveable { mutableStateOf<ConsoleTool?>(null) }
    // Landscape: the open category's info card shown in the right pane, if any.
    var openInfoCard by rememberSaveable { mutableStateOf<CardId?>(null) }

    // Reorder mode, and the user's saved order of categories and of the cards in each one.
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val orderStore = remember { CardOrderStore(context) }
    // Speak: the language the laptop reads the text in, remembered on the phone.
    val speechPrefs = remember { context.getSharedPreferences(SPEECH_PREFS, Context.MODE_PRIVATE) }
    var speakLanguage by remember {
        mutableStateOf(
            if (speechPrefs.getString(SPEECH_LANGUAGE_KEY, null) == PingProtocol.LANGUAGE_EN) {
                PingProtocol.LANGUAGE_EN
            } else {
                PingProtocol.LANGUAGE_TR
            },
        )
    }
    val savedOrders = remember { orderStore.load() }
    var reordering by remember { mutableStateOf(false) }
    var categoryOrder by remember {
        mutableStateOf(CardOrder.apply(savedOrders[CATEGORY_ORDER_KEY], Category.entries.toList()) { it.name })
    }
    val cardOrders = remember {
        mutableStateMapOf<Category, List<CardId>>().apply {
            Category.entries.forEach { category ->
                put(category, CardOrder.apply(savedOrders[category.name], category.cards) { it.name })
            }
        }
    }
    val openProgress = remember { Animatable(if (openCategory != null) 1f else 0f) }
    var sleepHours by rememberSaveable { mutableStateOf("0") }
    var sleepMinutes by rememberSaveable { mutableStateOf("30") }
    var networkDevices by remember { mutableStateOf<List<NetworkDevice>>(emptyList()) }
    var networkScannedAt by remember { mutableStateOf<Long?>(null) }
    var powerSnapshot by remember { mutableStateOf<PowerSnapshot?>(null) }
    // Downloads: the server computer's report, the saved speed limit, and the limit picked but not applied yet.
    var downloadItems by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }
    var downloadsReportedAt by remember { mutableStateOf<Long?>(null) }
    var appliedLimitMbps by remember { mutableStateOf<Int?>(null) }
    var savedLimitMbps by remember { mutableStateOf<Int?>(null) }
    var chosenLimitMbps by rememberSaveable { mutableStateOf<Int?>(null) }
    var applyingLimit by remember { mutableStateOf(false) }
    // The limit was saved and the server hasn't confirmed it yet (lights the Apply button).
    var limitPending by remember { mutableStateOf(false) }
    // Which button sent the command that's on its way, so only that one shows the light.
    var activeButton by remember { mutableStateOf<String?>(null) }
    // Tailscale: the state asked for while its command is on its way.
    var requestedTailscale by remember { mutableStateOf<Boolean?>(null) }
    var powerHistory by remember { mutableStateOf<List<PowerSample>>(emptyList()) }
    var historyRange by rememberSaveable { mutableStateOf(HistoryRange.SIX_HOURS) }
    var systemUpdate by remember { mutableStateOf<SystemUpdateStatus?>(null) }
    var downloadLink by rememberSaveable { mutableStateOf("") }
    val incomingDownload by IncomingDownloads.pending
    val sleepTotal = (sleepHours.toIntOrNull() ?: 0) * 60 + (sleepMinutes.toIntOrNull() ?: 0)
    val sleepTotalValid = sleepTotal in 1..PingProtocol.MAX_SLEEP_MINUTES
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val authorized = user.uid == PingProtocol.ANDROID_UID
    // A command is on its way or being carried out by the agent.
    val commandPending = sending || commandStatus == "PENDING" || commandStatus == "PROCESSING"
    val canSend = authorized &&
        connected == true &&
        !sending &&
        commandStatus !in setOf("PENDING", "PROCESSING")

    // Pull-to-refresh: drop and re-open the Realtime Database connection, then wait until
    // it reports connected again (or give up after a few seconds).
    fun refreshConnection() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            error = null
            database.goOffline()
            database.goOnline()
            delay(700)
            withTimeoutOrNull(8_000) { snapshotFlow { connected }.first { it == true } }
            refreshing = false
        }
    }

    // Landscape drill-down: going into a category slides its cards in from the right; going
    // back slides them out to the right again and the category list returns from the left.
    fun openCategoryFolder(category: Category) {
        consoleTool = null
        openInfoCard = null
        openCategory = category
        selectedCategory = category // keep portrait's tab in sync if the phone is rotated
        scope.launch { openProgress.animateTo(1f, tween(durationMillis = 300)) }
    }

    // Portrait: step to the next (+1) or previous (-1) tab, in the user's category order.
    fun switchTab(step: Int) {
        val target = categoryOrder.indexOf(selectedCategory) + step
        if (target in categoryOrder.indices) selectedCategory = categoryOrder[target]
    }

    fun saveOrders() {
        orderStore.save(
            mapOf(CATEGORY_ORDER_KEY to categoryOrder.map { it.name }) +
                cardOrders.map { (category, cards) -> category.name to cards.map { it.name } },
        )
    }

    // Entering reorder mode drops the keyboard (nothing inside a card can be typed into while
    // reordering); leaving it saves the order on the phone.
    fun toggleReordering() {
        if (reordering) {
            reordering = false
            saveOrders()
        } else {
            focusManager.clearFocus()
            reordering = true
            AppLog.info("Layout", "Reorder mode on")
        }
    }

    fun closeCategoryFolder() {
        consoleTool = null // the right pane goes back to the info cards right away
        openInfoCard = null
        scope.launch {
            openProgress.animateTo(0f, tween(durationMillis = 250))
            openCategory = null
        }
    }

    // [upload], if given, is written to uploads/<commandId> first; the Rules only accept a
    // torrent-file command once its upload is there.
    fun sendCommand(action: String, payload: Map<String, Any>, upload: Map<String, Any>? = null) {
        val newCommand = database.getReference("commands").push()
        val id = newCommand.key
        if (id == null) {
            error = "Could not allocate a command ID."
            AppLog.error("Command", "$action: $error")
        } else {
            commandStatus = null
            errorCode = null
            error = null
            sending = true
            val onFailure = { exception: Exception ->
                sending = false
                error = exception.localizedMessage
                    ?: "Could not send $action. Check the connection and Rules."
                AppLog.error("Command", "$action could not be sent: $error")
            }
            val writeCommand = {
                newCommand.setValue(payload)
                    .addOnSuccessListener {
                        sending = false
                        commandAction = action
                        commandId = id
                        commandStatus = "PENDING"
                        AppLog.info("Command", "$action sent (${shortId(id)}), waiting for the agent")
                    }
                    .addOnFailureListener { exception -> onFailure(exception) }
            }
            if (upload == null) {
                writeCommand()
            } else {
                database.getReference("uploads").child(id).setValue(upload)
                    .addOnSuccessListener { writeCommand() }
                    .addOnFailureListener { exception -> onFailure(exception) }
            }
        }
    }

    fun sendDownload(request: DownloadRequest) {
        AppLog.info("Downloads", "Asking the server computer to download \"${request.displayName}\" (${request.source})")
        sendCommand(
            DownloadProtocol.ACTION,
            DownloadProtocol.command(user.uid, request),
            DownloadProtocol.upload(request),
        )
    }

    fun applyDownloadLimit(mbps: Int) {
        applyingLimit = true
        database.getReference("settings/downloadLimitMbps").setValue(mbps)
            .addOnSuccessListener {
                applyingLimit = false
                limitPending = true
                AppLog.info("Downloads", "Speed limit set to ${DownloadProtocol.limitLabel(mbps)}")
            }
            .addOnFailureListener { exception ->
                applyingLimit = false
                error = exception.localizedMessage ?: "Could not save the speed limit."
                AppLog.error("Downloads", "Could not save the speed limit: $error")
            }
    }

    DisposableEffect(database) {
        val connectionRef = database.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = snapshot.getValue(Boolean::class.java) ?: false
                if (now != connected) {
                    // The very first "false" at startup is just "not connected yet" - not news.
                    if (now) {
                        AppLog.info("Connection", "Connected to Firebase")
                    } else if (connected == true) {
                        AppLog.warn("Connection", "Lost the connection to Firebase")
                    }
                }
                connected = now
            }

            override fun onCancelled(databaseError: DatabaseError) {
                connected = false
                error = databaseError.message
                AppLog.error("Connection", "Connection listener cancelled: ${databaseError.message}")
            }
        }
        connectionRef.addValueEventListener(listener)
        onDispose { connectionRef.removeEventListener(listener) }
    }

    DisposableEffect(database, commandId) {
        val id = commandId
        if (id == null) {
            onDispose { }
        } else {
            val commandRef = database.getReference("commands").child(id)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    val newStatus = snapshot.child("status").getValue(String::class.java)
                    val newErrorCode = snapshot.child("errorCode").getValue(String::class.java)
                    if (newStatus != commandStatus) {
                        val what = "$commandAction (${shortId(id)})"
                        when (newStatus) {
                            "PROCESSING" -> AppLog.info("Command", "$what picked up by the agent")
                            "SUCCESS" -> AppLog.info(
                                "Command",
                                "$what succeeded: ${snapshot.child("result").getValue(String::class.java)}",
                            )
                            "FAILED" -> AppLog.error("Command", "$what failed: ${newErrorCode ?: "no error code"}")
                            else -> Unit
                        }
                    }
                    commandStatus = newStatus
                    errorCode = newErrorCode
                    error = null
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    val message = "Could not observe command: ${databaseError.message}"
                    error = message
                    AppLog.error("Command", message)
                }
            }
            commandRef.addValueEventListener(listener)
            onDispose { commandRef.removeEventListener(listener) }
        }
    }

    DisposableEffect(database) {
        val networkRef = database.getReference("network/latest")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                networkScannedAt = snapshot.child("scannedAt").getValue(Long::class.java)
                networkDevices = snapshot.child("devices").children.mapNotNull { device ->
                    val mac = device.key ?: return@mapNotNull null
                    val ip = device.child("ip").getValue(String::class.java) ?: return@mapNotNull null
                    val name = device.child("name").getValue(String::class.java) ?: return@mapNotNull null
                    NetworkDevice(mac = mac, ip = ip, name = name)
                }.sortedBy { it.name.lowercase() }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Leave the last known device list visible; this is a read-only side panel.
                AppLog.warn("Network", "Could not read the device list: ${databaseError.message}")
            }
        }
        networkRef.addValueEventListener(listener)
        onDispose { networkRef.removeEventListener(listener) }
    }

    DisposableEffect(database) {
        val powerRef = database.getReference("power/latest")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val cpu = snapshot.child("cpu")
                val system = snapshot.child("system")
                powerSnapshot = PowerSnapshot(
                    reportedAt = snapshot.child("reportedAt").getValue(Long::class.java),
                    usagePercent = cpu.child("usagePercent").getValue(Double::class.java),
                    tempC = cpu.child("tempC").getValue(Long::class.java),
                    packageWatts = cpu.child("packageWatts").getValue(Double::class.java),
                    coreFreqMhz = cpu.child("coreFreqMhz").children.mapNotNull {
                        it.getValue(Long::class.java)
                    },
                    batteryPercent = snapshot.child("battery/percent").getValue(Long::class.java),
                    fanRpm = system.child("fanRpm").getValue(Long::class.java),
                    ecTempC = system.child("ecTempC").getValue(Long::class.java),
                    powerMode = snapshot.child("powerMode").getValue(String::class.java),
                    tailscale = snapshot.child("tailscale").getValue(String::class.java),
                )
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Leave the last known snapshot visible; this is a read-only side panel.
                AppLog.warn("Power", "Could not read the power report: ${databaseError.message}")
            }
        }
        powerRef.addValueEventListener(listener)
        onDispose { powerRef.removeEventListener(listener) }
    }

    // The last day of power samples for the Power history charts.
    DisposableEffect(database) {
        val since = System.currentTimeMillis() - HistoryRange.DAY.millis
        val historyQuery = database.getReference("power/history").orderByKey().startAt(since.toString())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                powerHistory = snapshot.children.mapNotNull { sample ->
                    val time = sample.key?.toLongOrNull() ?: return@mapNotNull null
                    PowerSample(
                        timeMs = time,
                        cpu = sample.child("cpu").getValue(Double::class.java)?.toFloat(),
                        watts = sample.child("w").getValue(Double::class.java)?.toFloat(),
                    )
                }.sortedBy { it.timeMs }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                AppLog.warn("Power", "Could not read the power history: ${databaseError.message}")
            }
        }
        historyQuery.addValueEventListener(listener)
        onDispose { historyQuery.removeEventListener(listener) }
    }

    DisposableEffect(database) {
        val updateRef = database.getReference("system/update")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    systemUpdate = null
                    return
                }
                val previous = systemUpdate
                val status = SystemUpdateStatus(
                    state = snapshot.child("state").getValue(String::class.java) ?: return,
                    phase = snapshot.child("phase").getValue(String::class.java) ?: "REFRESHING",
                    percent = snapshot.child("percent").getValue(Long::class.java)?.toInt() ?: 0,
                    done = snapshot.child("done").getValue(Long::class.java)?.toInt() ?: 0,
                    total = snapshot.child("total").getValue(Long::class.java)?.toInt() ?: 0,
                    packages = snapshot.child("packages").getValue(Long::class.java)?.toInt() ?: 0,
                    startedAt = snapshot.child("startedAt").getValue(Long::class.java),
                    finishedAt = snapshot.child("finishedAt").getValue(Long::class.java),
                    error = snapshot.child("error").getValue(String::class.java),
                    rebootRecommended = snapshot.child("rebootRecommended").getValue(Boolean::class.java),
                )
                systemUpdate = status
                if (previous?.running == true && !status.running) {
                    AppLog.info("Update", status.describe())
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                AppLog.warn("Update", "Could not read the system update status: ${databaseError.message}")
            }
        }
        updateRef.addValueEventListener(listener)
        onDispose { updateRef.removeEventListener(listener) }
    }

    // The Apply button's light: until the server reports the new limit, or 30 s at most.
    LaunchedEffect(limitPending, appliedLimitMbps, savedLimitMbps) {
        if (!limitPending) return@LaunchedEffect
        if (appliedLimitMbps != null && appliedLimitMbps == savedLimitMbps) {
            limitPending = false
        } else {
            delay(LIMIT_APPLY_WAIT_MILLIS)
            limitPending = false
        }
    }

    // The Tailscale switch shows the asked-for state only while its command is on its way.
    LaunchedEffect(commandPending) {
        if (!commandPending) requestedTailscale = null
    }

    DisposableEffect(database) {
        val downloadsRef = database.getReference("downloads/latest")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                downloadsReportedAt = snapshot.child("reportedAt").getValue(Long::class.java)
                appliedLimitMbps = snapshot.child("appliedLimitMbps").getValue(Long::class.java)?.toInt()
                downloadItems = snapshot.child("items").children.mapNotNull { item ->
                    val id = item.key ?: return@mapNotNull null
                    DownloadItem(
                        id = id,
                        name = item.child("name").getValue(String::class.java) ?: return@mapNotNull null,
                        kind = item.child("kind").getValue(String::class.java) ?: "URL",
                        state = item.child("state").getValue(String::class.java) ?: return@mapNotNull null,
                        totalBytes = item.child("totalBytes").getValue(Long::class.java) ?: 0L,
                        doneBytes = item.child("doneBytes").getValue(Long::class.java) ?: 0L,
                        downBps = item.child("downBps").getValue(Long::class.java) ?: 0L,
                        upBps = item.child("upBps").getValue(Long::class.java) ?: 0L,
                        etaSec = item.child("etaSec").getValue(Long::class.java),
                        error = item.child("error").getValue(String::class.java),
                    )
                }.sortedWith(compareBy({ downloadStateOrder(it.state) }, { it.name.lowercase() }))
            }

            override fun onCancelled(databaseError: DatabaseError) {
                AppLog.warn("Downloads", "Could not read the download list: ${databaseError.message}")
            }
        }
        downloadsRef.addValueEventListener(listener)
        onDispose { downloadsRef.removeEventListener(listener) }
    }

    DisposableEffect(database) {
        val limitRef = database.getReference("settings/downloadLimitMbps")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Nothing saved yet means the agent's default - the same one.
                savedLimitMbps = DownloadProtocol.parseLimit(snapshot.value)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                AppLog.warn("Downloads", "Could not read the speed limit: ${databaseError.message}")
            }
        }
        limitRef.addValueEventListener(listener)
        onDispose { limitRef.removeEventListener(listener) }
    }

    // A command that nobody picks up is the classic "agent not running / outdated" symptom -
    // say so instead of leaving only "Waiting for the agent" on screen.
    LaunchedEffect(commandId, commandStatus) {
        val id = commandId
        if (id != null && commandStatus == "PENDING") {
            delay(PENDING_WARNING_MILLIS)
            AppLog.warn(
                "Command",
                "$commandAction (${shortId(id)}) still PENDING after ${PENDING_WARNING_MILLIS / 1000} s - " +
                    "the agent hasn't picked it up. Is it running, and is it the latest version?",
            )
        }
    }

    if (signOutRequested) {
        AlertDialog(
            onDismissRequest = { signOutRequested = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to send commands.") },
            confirmButton = {
                TextButton(onClick = {
                    signOutRequested = false
                    AppLog.info("Auth", "Signed out")
                    auth.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { signOutRequested = false }) { Text("Cancel") }
            },
        )
    }

    val request = sleepRequest
    if (request != null) {
        AlertDialog(
            onDismissRequest = { sleepRequest = null },
            title = { Text("Put the laptop to sleep?") },
            text = {
                Text(
                    "The laptop suspends in a few seconds and wakes up " +
                        (if (request.minutes == null) "in the morning" else "after ${PingProtocol.formatDuration(request.minutes)}") +
                        ". Until then it cannot receive commands.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    sleepRequest = null
                    sendCommand(
                        PingProtocol.SLEEP_ACTION,
                        PingProtocol.sleepCommand(user.uid, request.minutes),
                    )
                }) { Text("Sleep") }
            },
            dismissButton = {
                TextButton(onClick = { sleepRequest = null }) { Text("Cancel") }
            },
        )
    }

    // Picking a .torrent inside the app is already an explicit choice, so it's sent right away.
    val torrentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val request = IncomingDownloads.readTorrent(context, uri)
            if (request != null) sendDownload(request) else error = "That file isn't a torrent (or is over 2 MB)."
        }
    }

    // Anything arriving from another app waits for an explicit "Download".
    incomingDownload?.let { incoming ->
        val what = when (incoming.source) {
            DownloadProtocol.SOURCE_MAGNET -> "Magnet link"
            DownloadProtocol.SOURCE_TORRENT_FILE -> "Torrent file"
            else -> "Link: " + (incoming.url ?: "").take(160)
        }
        AlertDialog(
            onDismissRequest = {
                IncomingDownloads.pending.value = null
                AppLog.info("Downloads", "Cancelled \"${incoming.displayName}\"")
            },
            title = { Text("Download on the server computer?") },
            text = {
                Text(
                    "\"${incoming.displayName}\"\n$what" +
                        if (canSend) "" else "\n\nNot ready yet - the server computer is busy or not connected. Try again in a moment.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = canSend,
                    onClick = {
                        IncomingDownloads.pending.value = null
                        activeButton = null
                        sendDownload(incoming)
                        selectedCategory = Category.DOWNLOADS
                        if (openCategory != null) openCategoryFolder(Category.DOWNLOADS)
                    },
                ) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = {
                    IncomingDownloads.pending.value = null
                    AppLog.info("Downloads", "Cancelled \"${incoming.displayName}\"")
                }) { Text("Cancel") }
            },
        )
    }

    if (shutdownRequested) {
        AlertDialog(
            onDismissRequest = { shutdownRequested = false },
            title = { Text("Shut the laptop down completely?") },
            text = {
                Text(
                    "It powers off immediately, with no delay. Unlike Sleep, it will " +
                        "NOT wake up on its own - you need physical access to turn it back on.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    shutdownRequested = false
                    sendCommand(
                        PingProtocol.SHUTDOWN_ACTION,
                        PingProtocol.shutdownCommand(user.uid),
                    )
                }) { Text("Shut down", color = GlassColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { shutdownRequested = false }) { Text("Cancel") }
            },
        )
    }

    // Shared pieces of content: extracted so portrait (one scrolling column) and landscape
    // (a controls strip + a separate full-height "monitor" pane) can arrange the exact same
    // cards differently without duplicating any of them.
    val reorderButton: @Composable () -> Unit = {
        GlassIconButton(
            description = if (reordering) "Finish reordering" else "Reorder",
            onClick = { toggleReordering() },
            active = reordering,
        ) { tint -> drawReorderIcon(tint) }
    }

    val reorderHint: @Composable () -> Unit = {
        if (reordering) {
            Text(
                "Hold a card and drag it to move it. Tap the reorder button again to save.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalGlassPalette.current.inkMuted,
            )
        }
    }

    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ComApp AV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (authorized) "Signed in as the Android test account"
                    else "Signed in with an unrecognized account",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGlassPalette.current.inkMuted,
                )
                Text(
                    "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalGlassPalette.current.inkFaint,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                reorderButton()
                GlassChip("Sign out", onClick = { signOutRequested = true })
            }
        }
    }

    val connectionCard: @Composable () -> Unit = {
        GlassCard(Modifier.fillMaxWidth(), accent = connectionColor(connected), cornerRadius = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(connectionColor(connected), CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = when (connected) {
                        true -> "Realtime Database: connected"
                        false -> "Realtime Database: disconnected (pull down to retry)"
                        null -> "Realtime Database: checking connection…"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }

    val pingCard: @Composable () -> Unit = {
        GlassCommandCard(
            title = "Ping",
            tag = PingProtocol.ACTION,
            description = "Check that the agent is alive. It answers with PONG.",
            accent = GlassColors.Blue,
        ) {
            GlassButton(
                text = "Send PING",
                accent = GlassColors.Blue,
                enabled = canSend,
                loading = sending && commandAction == PingProtocol.ACTION,
                working = activeButton == "ping" && commandPending,
                onClick = {
                    activeButton = "ping"
                    sendCommand(PingProtocol.ACTION, PingProtocol.pendingCommand(user.uid))
                },
            )
        }
    }

    // Speak: one line of text (Enter sends it), and a flag button next to the SPEAK tag that
    // switches the language the laptop reads it in. While typing, the field is scrolled up above the keyboard.
    val speakCard: @Composable () -> Unit = {
        val english = speakLanguage == PingProtocol.LANGUAGE_EN
        val sendSpeak = {
            if (canSend && speakText.isNotBlank()) {
                activeButton = "speak"
                sendCommand(
                    PingProtocol.SPEAK_ACTION,
                    PingProtocol.speakCommand(user.uid, speakText.trim(), speakLanguage),
                )
            }
        }
        val speakField = remember { BringIntoViewRequester() }
        var speakFocused by remember { mutableStateOf(false) }
        val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
        LaunchedEffect(speakFocused, imeBottom > 0) {
            if (speakFocused) {
                delay(SPEAK_FIELD_SCROLL_DELAY_MILLIS) // let the keyboard finish sliding in
                speakField.bringIntoView()
            }
        }
        GlassCommandCard(
            title = "Speak",
            tag = PingProtocol.SPEAK_ACTION,
            description = "The laptop reads your text out loud.",
            accent = GlassColors.Indigo,
            headerAction = {
                GlassContentButton(
                    description = if (english) {
                        "Speech language: English. Tap for Turkish."
                    } else {
                        "Speech language: Turkish. Tap for English."
                    },
                    onClick = {
                        val next = if (english) PingProtocol.LANGUAGE_TR else PingProtocol.LANGUAGE_EN
                        speakLanguage = next
                        speechPrefs.edit().putString(SPEECH_LANGUAGE_KEY, next).apply()
                        AppLog.info("Speak", "Language set to ${if (next == PingProtocol.LANGUAGE_EN) "English" else "Turkish"}")
                    },
                ) {
                    Text(if (english) "\uD83C\uDDEC\uD83C\uDDE7" else "\uD83C\uDDF9\uD83C\uDDF7", fontSize = 18.sp)
                }
            },
        ) {
            OutlinedTextField(
                value = speakText,
                onValueChange = { value ->
                    // One line only: a pasted line break becomes a space.
                    val line = value.replace('\n', ' ').replace('\r', ' ')
                    if (line.length <= PingProtocol.MAX_SPEAK_LENGTH) speakText = line
                },
                label = { Text(if (english) "Text to speak (English)" else "Text to speak (Türkçe)") },
                // A bare "x" at the end of the field clears it - no background, no ring.
                trailingIcon = if (speakText.isEmpty()) {
                    null
                } else {
                    {
                        val tint = LocalGlassPalette.current.inkMuted
                        Box(
                            Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button,
                                    onClickLabel = "Clear the text",
                                ) { speakText = "" }
                                .semantics { contentDescription = "Clear the text" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.size(12.dp)) {
                                val stroke = 1.8.dp.toPx()
                                drawLine(tint, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
                                drawLine(tint, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendSpeak() }),
                shape = GlassFieldShape,
                colors = glassTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(speakField)
                    .onFocusChanged { speakFocused = it.isFocused },
            )
            Text(
                "${speakText.length}/${PingProtocol.MAX_SPEAK_LENGTH}",
                style = MaterialTheme.typography.labelSmall,
                color = LocalGlassPalette.current.inkFaint,
            )
            GlassButton(
                text = "Send SPEAK",
                accent = GlassColors.Indigo,
                enabled = canSend && speakText.isNotBlank(),
                loading = sending && commandAction == PingProtocol.SPEAK_ACTION,
                working = activeButton == "speak" && commandPending,
                onClick = { sendSpeak() },
            )
        }
    }

    val sleepCard: @Composable () -> Unit = {
        GlassCommandCard(
            title = "Sleep",
            tag = PingProtocol.SLEEP_ACTION,
            description = "Suspend the laptop. It wakes up by itself.",
            accent = GlassColors.Violet,
        ) {
            GlassButton(
                text = "Sleep until morning",
                accent = GlassColors.Violet,
                enabled = canSend,
                working = activeButton == "sleep-morning" && commandPending,
                onClick = {
                    activeButton = "sleep-morning"
                    sleepRequest = SleepRequest(null)
                },
            )
            Text(
                "Or sleep for a set time",
                style = MaterialTheme.typography.labelLarge,
                color = LocalGlassPalette.current.inkMuted,
            )
            val chipScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalEdgeFade(chipScroll)
                    .horizontalScroll(chipScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(2, 10, 30, 60, 120).forEach { minutes ->
                    GlassChip(
                        text = PingProtocol.formatDuration(minutes),
                        onClick = {
                            sleepHours = (minutes / 60).toString()
                            sleepMinutes = (minutes % 60).toString()
                        },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sleepHours,
                    onValueChange = { value ->
                        if (value.length <= 2 && value.all { it.isDigit() }) sleepHours = value
                    },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = GlassFieldShape,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = sleepMinutes,
                    onValueChange = { value ->
                        if (value.length <= 2 && value.all { it.isDigit() }) sleepMinutes = value
                    },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = GlassFieldShape,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.weight(1f),
                )
            }
            GlassButton(
                text = if (sleepTotalValid) "Sleep for ${PingProtocol.formatDuration(sleepTotal)}"
                else "Enter 1 minute to 24 hours",
                accent = GlassColors.Violet,
                prominent = false,
                enabled = canSend && sleepTotalValid,
                working = activeButton == "sleep-timed" && commandPending,
                onClick = {
                    activeButton = "sleep-timed"
                    sleepRequest = SleepRequest(sleepTotal)
                },
            )
        }
    }

    val scanCard: @Composable () -> Unit = {
        GlassCommandCard(
            title = "Scan",
            tag = PingProtocol.SCAN_ACTION,
            description = "See who is on the home network right now.",
            accent = GlassColors.Neutral,
        ) {
            GlassButton(
                text = "Send SCAN",
                accent = GlassColors.Neutral,
                enabled = canSend,
                loading = sending && commandAction == PingProtocol.SCAN_ACTION,
                working = activeButton == "scan" && commandPending,
                onClick = {
                    activeButton = "scan"
                    sendCommand(PingProtocol.SCAN_ACTION, PingProtocol.scanCommand(user.uid))
                },
            )
        }
    }

    val shutdownCard: @Composable () -> Unit = {
        GlassCommandCard(
            title = "Shutdown",
            tag = PingProtocol.SHUTDOWN_ACTION,
            description = "Power the laptop off completely. No automatic wake-up - physical access is needed to turn it back on.",
            accent = GlassColors.Red,
        ) {
            GlassButton(
                text = "Shut down now",
                accent = GlassColors.Red,
                enabled = canSend,
                loading = sending && commandAction == PingProtocol.SHUTDOWN_ACTION,
                working = activeButton == "shutdown" && commandPending,
                onClick = {
                    activeButton = "shutdown"
                    shutdownRequested = true
                },
            )
        }
    }

    val powerModeCard: @Composable () -> Unit = {
        val busy = commandAction == PingProtocol.POWER_MODE_ACTION &&
            (sending || commandStatus in setOf("PENDING", "PROCESSING"))
        val currentMode = powerSnapshot?.powerMode
        GlassCommandCard(
            title = "Power mode",
            tag = PingProtocol.POWER_MODE_ACTION,
            description = "This machine runs 24/7 and never needs performance - keep it at " +
                "the lowest power draw, or switch back to normal if you need it responsive.",
            accent = GlassColors.Amber,
        ) {
            // The current mode is shown in the "System Info" card; here it only decides
            // which button looks pressed-in (prominent).
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = "Performance",
                    modifier = Modifier.weight(1f),
                    accent = GlassColors.Blue,
                    prominent = currentMode == PingProtocol.MODE_PERFORMANCE,
                    enabled = canSend && !busy,
                    loading = sending && requestedPowerMode == PingProtocol.MODE_PERFORMANCE,
                    working = busy && requestedPowerMode == PingProtocol.MODE_PERFORMANCE,
                    onClick = {
                        activeButton = null
                        requestedPowerMode = PingProtocol.MODE_PERFORMANCE
                        sendCommand(
                            PingProtocol.POWER_MODE_ACTION,
                            PingProtocol.powerModeCommand(user.uid, PingProtocol.MODE_PERFORMANCE),
                        )
                    },
                )
                GlassButton(
                    text = "Power save",
                    modifier = Modifier.weight(1f),
                    accent = GlassColors.Amber,
                    prominent = currentMode == PingProtocol.MODE_POWERSAVE,
                    enabled = canSend && !busy,
                    loading = sending && requestedPowerMode == PingProtocol.MODE_POWERSAVE,
                    working = busy && requestedPowerMode == PingProtocol.MODE_POWERSAVE,
                    onClick = {
                        activeButton = null
                        requestedPowerMode = PingProtocol.MODE_POWERSAVE
                        sendCommand(
                            PingProtocol.POWER_MODE_ACTION,
                            PingProtocol.powerModeCommand(user.uid, PingProtocol.MODE_POWERSAVE),
                        )
                    },
                )
            }
        }
    }

    val networkCard: @Composable () -> Unit = {
        if (networkDevices.isNotEmpty()) {
            val clipboard = LocalClipboardManager.current
            val context = LocalContext.current
            GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Neutral, cornerRadius = 22.dp) {
                Text(
                    "On the network",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                networkScannedAt?.let { scannedAt ->
                    Text(
                        "Last scan: ${formatRelativeTime(scannedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGlassPalette.current.inkMuted,
                    )
                }
                Text(
                    "Tap a device to copy its IP address.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalGlassPalette.current.inkFaint,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    networkDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboard.setText(AnnotatedString(device.ip))
                                    Toast.makeText(
                                        context,
                                        "Copied ${device.ip}",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(device.name, fontWeight = FontWeight.Medium)
                            Text(
                                device.ip,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalGlassPalette.current.inkFaint,
                            )
                        }
                    }
                }
            }
        }
    }

    // Portrait: a fixed bottom navigation bar, pinned outside the scrolling content.
    val bottomNav: @Composable () -> Unit = {
        GlassNavSurface(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // The glass runs on behind the system navigation bar; the tabs stay above it.
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoryOrder.forEach { category ->
                    GlassBottomNavItem(
                        label = category.label,
                        selected = selectedCategory == category,
                        accent = category.accent,
                        onClick = { selectedCategory = category },
                        modifier = Modifier.weight(1f),
                        icon = category.icon,
                    )
                }
            }
        }
    }

    // Console: the app's own event log (see AppLog), newest first, for spotting and sharing
    // errors. "Copy all" puts every saved entry on the clipboard, ready to paste into a chat.
    val consoleCard: @Composable () -> Unit = {
        val clipboard = LocalClipboardManager.current
        val context = LocalContext.current
        val palette = LocalGlassPalette.current
        val all = AppLog.entries
        val newestFirst = all.asReversed().filter { !consoleErrorsOnly || it.level != LogLevel.INFO }
        val shown = newestFirst.take(CONSOLE_VISIBLE_ENTRIES)
        // "Clear" needs a second tap within a few seconds, so a stray tap can't wipe the log.
        var confirmClear by remember { mutableStateOf(false) }
        LaunchedEffect(confirmClear) {
            if (confirmClear) {
                delay(3_000)
                confirmClear = false
            }
        }
        GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Green, cornerRadius = 22.dp) {
            Text(
                "Console",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${all.size} events saved · newest first · tap a line to copy it",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted,
            )
            val actionScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalEdgeFade(actionScroll)
                    .horizontalScroll(actionScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassChip(
                    text = if (consoleErrorsOnly) "Show all" else "Warnings & errors",
                    onClick = { consoleErrorsOnly = !consoleErrorsOnly },
                )
                GlassChip(
                    text = "Copy all",
                    onClick = {
                        val header = "ComApp AV v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - " +
                            "${all.size} log entries, oldest first"
                        val body = all.joinToString(separator = "\n") { LogFormat.format(it) }
                        clipboard.setText(AnnotatedString("$header\n$body"))
                        Toast.makeText(context, "Copied ${all.size} log entries", Toast.LENGTH_SHORT).show()
                    },
                )
                GlassChip(
                    text = if (confirmClear) "Tap again to clear" else "Clear",
                    onClick = {
                        if (confirmClear) {
                            AppLog.clear()
                            confirmClear = false
                        } else {
                            confirmClear = true
                        }
                    },
                )
            }
            if (shown.isEmpty()) {
                Text(
                    if (consoleErrorsOnly) "No warnings or errors." else "No events yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkFaint,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                shown.forEach { entry ->
                    val levelColor = when (entry.level) {
                        LogLevel.INFO -> palette.inkMuted
                        LogLevel.WARN -> GlassColors.Amber
                        LogLevel.ERROR -> GlassColors.Red
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(LogFormat.format(entry)))
                                Toast.makeText(context, "Copied this line", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "${LogFormat.timeOfDay(entry)}  ${entry.level.name}  ${entry.tag}",
                            color = levelColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            entry.message,
                            color = palette.ink,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (newestFirst.size > shown.size) {
                Text(
                    "Showing the newest ${shown.size} of ${newestFirst.size}. \"Copy all\" includes everything.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkFaint,
                )
            }
        }
    }

    // The cards inside one category - shared by portrait's bottom-nav tabs and landscape's
    // open folder, so both show exactly the same cards.
    val downloadAddCard: @Composable () -> Unit = {
        val linkRequest = DownloadProtocol.findLink(downloadLink.trim())
        GlassCommandCard(
            title = "Add download",
            tag = DownloadProtocol.ACTION,
            description = "A .torrent file, a magnet link or a direct file link - the server computer downloads it to " +
                "~/Downloads/ComApp. From any app you can also share a link with \"Download on the server computer\".",
            accent = GlassColors.Indigo,
        ) {
            GlassButton(
                text = "Choose a .torrent file",
                accent = GlassColors.Indigo,
                enabled = canSend,
                loading = sending && commandAction == DownloadProtocol.ACTION && activeButton == "torrent",
                working = activeButton == "torrent" && commandPending,
                onClick = {
                    activeButton = "torrent"
                    torrentPicker.launch(arrayOf("application/x-bittorrent", "application/octet-stream"))
                },
            )
            OutlinedTextField(
                value = downloadLink,
                onValueChange = { if (it.length <= DownloadProtocol.MAX_URL_LENGTH) downloadLink = it },
                label = { Text("Magnet or file link") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = GlassFieldShape,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (downloadLink.isNotBlank() && linkRequest == null) {
                Text(
                    "That isn't a magnet or http(s) link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassColors.Red,
                )
            }
            GlassButton(
                text = "Download on the server computer",
                accent = GlassColors.Indigo,
                prominent = false,
                enabled = canSend && linkRequest != null,
                working = activeButton == "download-link" && commandPending,
                onClick = {
                    linkRequest?.let {
                        activeButton = "download-link"
                        sendDownload(it)
                        downloadLink = ""
                    }
                },
            )
        }
    }

    val downloadListCard: @Composable () -> Unit = {
        val palette = LocalGlassPalette.current
        GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Indigo, cornerRadius = 22.dp) {
            Text(
                "On the server computer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                downloadsReportedAt?.let { "Updated ${formatRelativeTime(it)}" } ?: "Waiting for the server computer's first report…",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkMuted,
            )
            if (downloadItems.isEmpty()) {
                Text("No downloads yet.", style = MaterialTheme.typography.bodySmall, color = palette.inkFaint)
            }
            downloadItems.forEach { item ->
                val stateColor = when (item.state) {
                    "DOWNLOADING" -> GlassColors.Indigo
                    "SEEDING", "COMPLETE" -> GlassColors.Green
                    "FAILED" -> GlassColors.Red
                    else -> palette.inkMuted
                }
                val detail = when (item.state) {
                    "DOWNLOADING" -> buildString {
                        append(DownloadProtocol.formatBytes(item.doneBytes))
                        if (item.totalBytes > 0) append(" of ").append(DownloadProtocol.formatBytes(item.totalBytes))
                        append(" · ").append(DownloadProtocol.formatSpeed(item.downBps))
                        item.etaSec?.let { append(" · ").append(DownloadProtocol.formatEta(it)).append(" left") }
                    }
                    "SEEDING" -> "${DownloadProtocol.formatBytes(item.totalBytes)} · sharing " +
                        "${DownloadProtocol.formatSpeed(item.upBps)} for a few minutes, then done"
                    "COMPLETE" -> "${DownloadProtocol.formatBytes(item.totalBytes)} · in ~/Downloads/ComApp"
                    "FAILED" -> DownloadProtocol.describeItemError(item.error)
                    "QUEUED" -> "Waiting for a free slot"
                    else -> DownloadProtocol.stateLabel(item.state)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            DownloadProtocol.stateLabel(item.state),
                            style = MaterialTheme.typography.labelSmall,
                            color = stateColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (item.totalBytes > 0 && item.state != "FAILED") {
                        GlassProgressBar(
                            fraction = item.doneBytes.toFloat() / item.totalBytes.toFloat(),
                            accent = stateColor,
                        )
                    }
                    Text(
                        (if (item.kind == "TORRENT") "Torrent · " else "Link · ") + detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted,
                    )
                }
            }
        }
    }

    val downloadLimitCard: @Composable () -> Unit = {
        val saved = savedLimitMbps
        val chosen = chosenLimitMbps ?: saved ?: DownloadProtocol.DEFAULT_LIMIT_MBPS
        val applied = appliedLimitMbps
        GlassCommandCard(
            title = "Speed limit",
            tag = "SETTING",
            description = "One limit shared by every download on the server computer, torrents and links together.",
            accent = GlassColors.Indigo,
        ) {
            val limitScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalEdgeFade(limitScroll)
                    .horizontalScroll(limitScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DownloadProtocol.LIMIT_PRESETS_MBPS.forEach { mbps ->
                    GlassChip(
                        text = DownloadProtocol.limitLabel(mbps),
                        selected = chosen == mbps,
                        accent = GlassColors.Indigo,
                        onClick = { chosenLimitMbps = mbps },
                    )
                }
            }
            GlassButton(
                text = if (chosen == saved) "Applied" else "Apply ${DownloadProtocol.limitLabel(chosen)}",
                accent = GlassColors.Indigo,
                enabled = authorized && connected == true && chosen != saved && !applyingLimit,
                loading = applyingLimit,
                working = limitPending,
                onClick = { applyDownloadLimit(chosen) },
            )
            Text(
                if (applied == null) {
                    "The server computer hasn't reported its limit yet."
                } else if (applied == saved) {
                    "The server computer is using ${DownloadProtocol.limitLabel(applied)}."
                } else {
                    "The server computer is still on ${DownloadProtocol.limitLabel(applied)} - it picks up the new one within ~15 s."
                },
                style = MaterialTheme.typography.bodySmall,
                color = LocalGlassPalette.current.inkMuted,
            )
        }
    }

    // Tailscale on the server computer: the switch follows what the agent last reported, and
    // shows the asked-for position (with the running light) while the change is on its way.
    val tailscaleRow: @Composable () -> Unit = {
        val reported = powerSnapshot?.tailscale
        val switching = commandPending && requestedTailscale != null
        val on = requestedTailscale ?: (reported == "RUNNING" || reported == "STARTING")
        val note = when {
            switching -> if (on) "Turning on…" else "Turning off…"
            reported == "NOT_INSTALLED" -> "Not installed on the server computer"
            reported == "NEEDS_LOGIN" -> "Needs a login on the server computer"
            reported == null -> "Waiting for the server computer's report…"
            else -> null
        }
        GlassSwitchRow(
            title = "Tailscale",
            subtitle = note,
            checked = on,
            enabled = canSend && reported != null && reported != "NOT_INSTALLED",
            working = switching,
            accent = GlassColors.Blue,
            onCheckedChange = { wanted ->
                activeButton = null
                requestedTailscale = wanted
                AppLog.info("Tailscale", if (wanted) "Turning on" else "Turning off")
                sendCommand(PingProtocol.TAILSCALE_ACTION, PingProtocol.tailscaleCommand(user.uid, wanted))
            },
        )
    }

    // Update server system: installs every available update (dnf upgrade) without a password;
    // the thin bar under the button follows the agent's progress report.
    val systemUpdateCard: @Composable () -> Unit = {
        val status = systemUpdate
        val palette = LocalGlassPalette.current
        GlassCommandCard(
            title = "Update server system",
            tag = "UPDATE",
            description = "Install every available update on the server computer (dnf upgrade).",
            accent = GlassColors.Green,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassButton(
                    text = "Update",
                    accent = GlassColors.Green,
                    enabled = canSend && status?.running != true,
                    loading = sending && activeButton == "update",
                    working = activeButton == "update" && commandPending,
                    onClick = {
                        activeButton = "update"
                        AppLog.info("Update", "System update requested")
                        sendCommand(PingProtocol.SYSTEM_UPDATE_ACTION, PingProtocol.systemUpdateCommand(user.uid))
                    },
                )
                GlassProgressBar(
                    fraction = status?.fraction ?: 0f,
                    accent = if (status?.state == "FAILED") GlassColors.Red else GlassColors.Green,
                )
            }
            Text(
                when {
                    status == null -> "No update has run from the app yet."
                    status.running -> status.describe()
                    else -> status.describe() +
                        (status.finishedAt?.let { " · ${formatRelativeTime(it)}" } ?: "")
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (status?.state == "FAILED") GlassColors.Red else palette.inkMuted,
            )
        }
    }

    // Power history: CPU usage and package power over the last hour, 6 hours or day, from the
    // agent's once-a-minute samples.
    val powerHistoryCard: @Composable () -> Unit = {
        val palette = LocalGlassPalette.current
        val now = powerHistory.lastOrNull()?.timeMs?.coerceAtLeast(System.currentTimeMillis())
            ?: System.currentTimeMillis()
        val start = now - historyRange.millis
        val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
        val startLabel = timeFormat.format(java.util.Date(start))
        GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Amber, cornerRadius = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HistoryRange.entries.forEach { range ->
                        GlassChip(
                            text = range.label,
                            selected = historyRange == range,
                            accent = GlassColors.Amber,
                            onClick = { historyRange = range },
                        )
                    }
                }
            }
            GlassLineChart(
                title = "CPU usage",
                points = powerHistory.mapNotNull { sample -> sample.cpu?.let { ChartPoint(sample.timeMs, it) } },
                startMs = start,
                endMs = now,
                accent = GlassColors.Blue,
                format = { "${"%.0f".format(it)}%" },
                startLabel = startLabel,
                endLabel = "now",
                fixedMax = 100f,
            )
            GlassLineChart(
                title = "Power draw",
                points = powerHistory.mapNotNull { sample -> sample.watts?.let { ChartPoint(sample.timeMs, it) } },
                startMs = start,
                endMs = now,
                accent = GlassColors.Amber,
                format = { "${"%.1f".format(it)} W" },
                startLabel = startLabel,
                endLabel = "now",
            )
            Text(
                "One sample a minute, kept for a day. CPU package only, like System Info.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkFaint,
            )
        }
    }

    val cardContent: @Composable (CardId) -> Unit = { id ->
        when (id) {
            CardId.PING -> pingCard()
            CardId.SPEAK -> speakCard()
            CardId.SLEEP -> sleepCard()
            CardId.SHUTDOWN -> shutdownCard()
            CardId.POWER_MODE -> powerModeCard()
            CardId.SCAN -> scanCard()
            CardId.DEVICES -> networkCard()
            CardId.DOWNLOAD_ADD -> downloadAddCard()
            CardId.DOWNLOAD_LIST -> downloadListCard()
            CardId.DOWNLOAD_LIMIT -> downloadLimitCard()
            CardId.CONSOLE -> consoleCard()
            CardId.TAILSCALE -> tailscaleRow()
            CardId.SYSTEM_UPDATE -> systemUpdateCard()
            CardId.POWER_HISTORY -> powerHistoryCard()
        }
    }

    val categoryCards: @Composable (Category) -> Unit = { category ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            reorderHint()
            ReorderableColumn(
                items = cardOrders[category] ?: category.cards,
                itemKey = { it },
                enabled = reordering,
                onMove = { from, to ->
                    val cards = (cardOrders[category] ?: category.cards).toMutableList()
                    cards.add(to, cards.removeAt(from))
                    cardOrders[category] = cards
                },
                modifier = Modifier.fillMaxWidth(),
                accent = category.accent,
            ) { id -> cardContent(id) }
        }
    }

    // Portrait: switching bottom-nav tabs slides the cards sideways - a tab further right
    // slides in from the right, one further left slides in from the left.
    val categoryContent: @Composable () -> Unit = {
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                val forward = categoryOrder.indexOf(targetState) > categoryOrder.indexOf(initialState)
                (
                    slideInHorizontally(initialOffsetX = { width -> if (forward) width else -width }) +
                        fadeIn()
                    ) togetherWith (
                    slideOutHorizontally(targetOffsetX = { width -> if (forward) -width else width }) +
                        fadeOut()
                    )
            },
            label = "category",
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = tabSwipeOffset.value },
        ) { category ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                categoryCards(category)
            }
        }
    }

    // Landscape Console folder: a list of tools; tapping one opens its window in the right pane,
    // tapping it again closes it.
    val consoleTools: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Pick a tool to open it on the right.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalGlassPalette.current.inkMuted,
            )
            ConsoleTool.entries.forEach { tool ->
                GlassFolderRow(
                    title = tool.label,
                    subtitle = tool.description,
                    accent = Category.CONSOLE.accent,
                    selected = consoleTool == tool,
                    onClick = { consoleTool = if (consoleTool == tool) null else tool },
                )
            }
        }
    }

    // Landscape: the button in a category's left pane that opens one of its info cards on the
    // right - its title and a short line about what's in it; null hides the button.
    fun infoButton(id: CardId): Pair<String, String>? = when (id) {
        CardId.DEVICES -> if (networkDevices.isEmpty()) {
            null
        } else {
            "On the network" to "${networkDevices.size} device${if (networkDevices.size == 1) "" else "s"} found"
        }
        CardId.DOWNLOAD_LIST -> {
            val active = downloadItems.count { it.state == "DOWNLOADING" || it.state == "QUEUED" }
            "On the server computer" to when {
                downloadItems.isEmpty() -> "No downloads yet"
                active > 0 -> "$active active · ${downloadItems.size} in the list"
                else -> "${downloadItems.size} in the list"
            }
        }
        CardId.POWER_HISTORY -> "History" to "CPU usage and power draw"
        else -> null
    }

    // Landscape folder of a category: its interactive cards (in the user's order, reorderable),
    // then a button for each info card, which opens that card in the right pane.
    val landscapeFolder: @Composable (Category) -> Unit = { category ->
        val cards = cardOrders[category] ?: category.cards
        val interactive = cards.filter { it.kind == CardKind.INTERACTIVE }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            reorderHint()
            ReorderableColumn(
                items = interactive,
                itemKey = { it },
                enabled = reordering,
                onMove = { from, to ->
                    // Move within the full order, so the info cards keep their places.
                    val full = (cardOrders[category] ?: category.cards).toMutableList()
                    val shown = full.filter { it.kind == CardKind.INTERACTIVE }
                    val moved = shown[from]
                    val target = shown[to]
                    full.remove(moved)
                    val at = full.indexOf(target)
                    full.add(if (to > from) at + 1 else at, moved)
                    cardOrders[category] = full
                },
                modifier = Modifier.fillMaxWidth(),
                accent = category.accent,
            ) { id -> cardContent(id) }
            cards.filter { it.kind == CardKind.INFO }.forEach { id ->
                infoButton(id)?.let { (title, subtitle) ->
                    GlassFolderRow(
                        title = title,
                        subtitle = subtitle,
                        accent = category.accent,
                        selected = openInfoCard == id,
                        onClick = { openInfoCard = if (openInfoCard == id) null else id },
                    )
                }
            }
        }
    }

    // Landscape: the left pane works like browsing folders. At the root it lists only the
    // categories; opening one slides its cards in from the right over the list. Going back -
    // the "‹ Categories" chip, the system back button, or a left-to-right swipe anywhere on
    // the pane (Telegram-style, it follows the finger) - slides them out to the right again.
    val landscapeNavigator: @Composable () -> Unit = {
        val velocityTracker = remember { VelocityTracker() }
        Box(
            Modifier
                .fillMaxHeight()
                .width(300.dp)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onDragEnd = {
                            if (openCategory != null) {
                                val velocityX = velocityTracker.calculateVelocity().x
                                if (velocityX > 800f || openProgress.value < 0.6f) {
                                    closeCategoryFolder()
                                } else {
                                    scope.launch { openProgress.animateTo(1f, tween(durationMillis = 200)) }
                                }
                            }
                        },
                        onDragCancel = {
                            if (openCategory != null) {
                                scope.launch { openProgress.animateTo(1f, tween(durationMillis = 200)) }
                            }
                        },
                    ) { change, dragAmount ->
                        if (openCategory != null) {
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val paneWidth = size.width.toFloat().coerceAtLeast(1f)
                            val next = (openProgress.value - dragAmount / paneWidth).coerceIn(0f, 1f)
                            scope.launch { openProgress.snapTo(next) }
                        }
                    }
                },
        ) {
            // Root level: just the category "folders". It drifts a little to the left and fades
            // out while a folder is open, like the previous screen in a navigation stack.
            val listScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -0.3f * openProgress.value * size.width
                        alpha = 1f - openProgress.value
                    }
                    .verticalEdgeFade(listScroll)
                    .verticalScroll(listScroll)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                header()
                Text(
                    "Categories",
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalGlassPalette.current.inkMuted,
                )
                reorderHint()
                ReorderableColumn(
                    items = categoryOrder,
                    itemKey = { it },
                    enabled = reordering,
                    onMove = { from, to ->
                        categoryOrder = categoryOrder.toMutableList().apply { add(to, removeAt(from)) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { category ->
                    GlassFolderRow(
                        title = category.label,
                        subtitle = category.contents,
                        accent = category.accent,
                        // The list is invisible (alpha 0) while a folder is open; don't let a
                        // tap on that invisible list open another one underneath.
                        onClick = { if (openCategory == null) openCategoryFolder(category) },
                    )
                }
            }

            // One level deeper: the open category's cards, on top of the list.
            openCategory?.let { category ->
                key(category) {
                    val folderScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = (1f - openProgress.value) * size.width }
                            .verticalEdgeFade(folderScroll)
                            .verticalScroll(folderScroll)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassChip("‹  Categories", onClick = { closeCategoryFolder() })
                            Spacer(Modifier.width(12.dp))
                            Text(
                                category.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            // The header (and its reorder button) isn't on screen inside a
                            // folder, so the folder bar carries its own - when there are at
                            // least two cards here to reorder. Console lists tools, not cards.
                            val interactiveCount = (cardOrders[category] ?: category.cards)
                                .count { it.kind == CardKind.INTERACTIVE }
                            if (category != Category.CONSOLE && interactiveCount >= 2) reorderButton()
                        }
                        if (category == Category.CONSOLE) consoleTools() else landscapeFolder(category)
                    }
                }
            }
        }
    }

    // Tapping anywhere on System Info asks the agent for a fresh report; a spinner sits in the
    // card's corner until the agent has published it.
    val systemInfoCard: @Composable () -> Unit = {
        powerSnapshot?.let { power ->
            val palette = LocalGlassPalette.current
            val refreshingInfo = commandAction == PingProtocol.SYSTEM_INFO_ACTION &&
                (sending || commandStatus == "PENDING" || commandStatus == "PROCESSING")
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        when {
                            refreshingInfo -> Unit
                            canSend -> {
                                AppLog.info("System info", "Refresh requested")
                                sendCommand(
                                    PingProtocol.SYSTEM_INFO_ACTION,
                                    PingProtocol.systemInfoCommand(user.uid),
                                )
                            }
                            else -> Toast.makeText(
                                context,
                                "Can't refresh right now - another command is running or there's no connection.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                accent = GlassColors.Amber,
                cornerRadius = 22.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "System Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    AnimatedVisibility(visible = refreshingInfo, enter = fadeIn(), exit = fadeOut()) {
                        // Same look as the pull-to-refresh spinner: a small round chip with a
                        // spinning arc inside.
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(palette.baseMid, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = GlassColors.Amber,
                            )
                        }
                    }
                }
                Text(
                    buildString {
                        power.reportedAt?.let { append("Last report: ").append(formatRelativeTime(it)).append(" · ") }
                        append(if (refreshingInfo) "Refreshing…" else "Tap to refresh")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    power.powerMode?.let {
                        val label = when (it) {
                            PingProtocol.MODE_POWERSAVE -> "Power save"
                            PingProtocol.MODE_PERFORMANCE -> "Performance"
                            else -> "Unknown"
                        }
                        Text("Power mode: $label", fontWeight = FontWeight.Medium)
                    }
                    power.usagePercent?.let { Text("CPU usage: ${"%.1f".format(it)}%") }
                    power.tempC?.let { Text("CPU temperature: $it°C") }
                    power.packageWatts?.let { watts ->
                        Text("Package power: ${"%.1f".format(watts)} W")
                        val costPerHourEur = watts / 1000.0 * ELECTRICITY_PRICE_EUR_PER_KWH
                        Text(
                            "Estimated cost: ${"%.2f".format(costPerHourEur * 100)} ¢/hour " +
                                "(~€${"%.2f".format(costPerHourEur * 24)}/day at this rate)",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalGlassPalette.current.inkMuted,
                        )
                    }
                    if (power.coreFreqMhz.isNotEmpty()) {
                        val avgGhz = power.coreFreqMhz.average() / 1000
                        Text(
                            "Core frequency (avg): ${"%.1f".format(avgGhz)} GHz " +
                                "(${power.coreFreqMhz.size} cores)",
                        )
                    }
                    power.batteryPercent?.let { Text("Battery: $it%") }
                    power.fanRpm?.let { Text("Fan: $it RPM") }
                    power.ecTempC?.let { Text("EC temperature: $it°C") }
                    PingProtocol.tailscaleLabel(power.tailscale)?.let { Text("Tailscale: $it") }
                }
                Text(
                    "CPU-side only: RAM, SSD, Wi-Fi/Bluetooth, and any discrete GPU aren't " +
                        "measurable in software on this hardware.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalGlassPalette.current.inkFaint,
                )
            }
        }
    }

    val authorizedWarning: @Composable () -> Unit = {
        if (!authorized) {
            GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Red, cornerRadius = 22.dp) {
                Text(
                    "This account is not authorized for this milestone. Sign out and use the configured Android test account.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    val latestCommandCard: @Composable () -> Unit = {
        if (commandId != null) {
            GlassCard(Modifier.fillMaxWidth(), accent = statusColor(commandStatus), cornerRadius = 22.dp) {
                Text(
                    "Latest command",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ID: $commandId", style = MaterialTheme.typography.bodySmall)
                    Text("Action: $commandAction")
                    Text("Status: ${commandStatus ?: if (sending) "SENDING" else "WAITING"}")
                    if (commandStatus == "FAILED" && errorCode != null) {
                        Text("Error: $errorCode", color = MaterialTheme.colorScheme.error)
                        (PingProtocol.describeError(errorCode) ?: DownloadProtocol.describeError(errorCode))?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (commandStatus == "PENDING") {
                        Text("Waiting for the agent.")
                    }
                    if (commandStatus == "PROCESSING") {
                        Text("The agent is processing this $commandAction.")
                    }
                    if (commandStatus == "SUCCESS") {
                        Text(
                            when (commandAction) {
                                PingProtocol.SPEAK_ACTION -> "The agent spoke your text."
                                PingProtocol.SLEEP_ACTION -> "The laptop is going to sleep."
                                PingProtocol.SCAN_ACTION -> "Network scan complete."
                                PingProtocol.SHUTDOWN_ACTION -> "The laptop is powering off now."
                                PingProtocol.POWER_MODE_ACTION -> "Power mode updated."
                                PingProtocol.SYSTEM_INFO_ACTION -> "System info refreshed."
                                PingProtocol.TAILSCALE_ACTION -> "Tailscale switched."
                                PingProtocol.SYSTEM_UPDATE_ACTION -> "Update started - follow it on the Update server system card."
                                DownloadProtocol.ACTION -> "Download started on the server computer - follow it under Downloads."
                                else -> "PONG received from the agent."
                            },
                        )
                    }
                }
            }
        }
    }

    val errorText: @Composable () -> Unit = {
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // System back (button or edge gesture) closes an open landscape folder before it would
    // leave the app.
    BackHandler(enabled = isLandscape && openCategory != null) { closeCategoryFolder() }
    // Declared after the folder handler, so while reordering, back ends reorder mode first.
    BackHandler(enabled = reordering) { toggleReordering() }
    DisposableEffect(Unit) {
        // Rotating or leaving the screen mid-reorder still keeps the order.
        onDispose { if (reordering) saveOrders() }
    }

    // A thin vertical gradient standing in for a hard divider between side-by-side panes.
    val verticalDivider: @Composable () -> Unit = {
        Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            LocalGlassPalette.current.inkFaint.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = LocalGlassPalette.current.ink,
        bottomBar = { if (!isLandscape) bottomNav() },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshConnection() },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // The keyboard isn't part of the Scaffold's padding (the app draws edge to edge),
                // so the content shrinks above it here - otherwise a text field near the bottom
                // ends up hidden behind the keyboard.
                .consumeWindowInsets(padding)
                .imePadding(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = LocalGlassPalette.current.baseMid,
                    color = GlassColors.Blue,
                )
            },
        ) {
            if (isLandscape) {
                // Left: the folder-style category navigator (categories -> cards). Right: a
                // full-height "monitor" pane with everything that's read rather than tapped.
                // Each pane scrolls on its own.
                Row(Modifier.fillMaxSize()) {
                    landscapeNavigator()
                    verticalDivider()
                    val infoScroll = rememberScrollState()
                    // A Console tool, or an info card of the open category, while one is open.
                    val openCategoryCards = openCategory?.let { cardOrders[it] ?: it.cards }.orEmpty()
                    val rightPaneTool: RightPaneView? = when {
                        openCategory == Category.CONSOLE && consoleTool == ConsoleTool.LOGS -> RightPaneView.Logs
                        openInfoCard != null && openInfoCard in openCategoryCards -> RightPaneView.Info(openInfoCard!!)
                        else -> null
                    }
                    LaunchedEffect(rightPaneTool) { infoScroll.scrollTo(0) }
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .verticalEdgeFade(infoScroll)
                            .verticalScroll(infoScroll)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Always first, whatever opens below it: is the database reachable?
                        connectionCard()
                        AnimatedContent(
                            targetState = rightPaneTool,
                            transitionSpec = {
                                fadeIn(tween(durationMillis = 220)) togetherWith fadeOut(tween(durationMillis = 150))
                            },
                            label = "rightPane",
                            modifier = Modifier.fillMaxWidth(),
                        ) { tool ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                val infoCards: @Composable () -> Unit = {
                                    systemInfoCard()
                                    authorizedWarning()
                                    latestCommandCard()
                                    errorText()
                                }
                                when (tool) {
                                    RightPaneView.Logs -> consoleCard()
                                    // The opened info card goes first; the usual ones stay below it.
                                    is RightPaneView.Info -> {
                                        cardContent(tool.card)
                                        infoCards()
                                    }
                                    null -> infoCards()
                                }
                            }
                        }
                    }
                }
            } else {
                val portraitScroll = rememberScrollState()
                val swipeTracker = remember { VelocityTracker() }
                var swipeDrag by remember { mutableFloatStateOf(0f) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Swipe left/right anywhere to change tab. Vertical drags still scroll
                        // (the scroll claims them first), and horizontally scrolling rows inside
                        // cards keep their own swipes.
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    swipeTracker.resetTracking()
                                    swipeDrag = 0f
                                },
                                onDragEnd = {
                                    val velocityX = swipeTracker.calculateVelocity().x
                                    val flingVelocity = TAB_SWIPE_VELOCITY_DP_PER_S.dp.toPx()
                                    val distance = size.width * TAB_SWIPE_DISTANCE_FRACTION
                                    val step = when {
                                        velocityX < -flingVelocity -> 1
                                        velocityX > flingVelocity -> -1
                                        swipeDrag < -distance -> 1
                                        swipeDrag > distance -> -1
                                        else -> 0
                                    }
                                    if (step != 0) switchTab(step)
                                    swipeDrag = 0f
                                    scope.launch { tabSwipeOffset.animateTo(0f, tween(durationMillis = 250)) }
                                },
                                onDragCancel = {
                                    swipeDrag = 0f
                                    scope.launch { tabSwipeOffset.animateTo(0f, tween(durationMillis = 200)) }
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                swipeTracker.addPosition(change.uptimeMillis, change.position)
                                swipeDrag += dragAmount
                                val index = categoryOrder.indexOf(selectedCategory)
                                val hasNeighbour = if (swipeDrag < 0f) index < categoryOrder.lastIndex else index > 0
                                val follow = if (hasNeighbour) TAB_SWIPE_FOLLOW else TAB_SWIPE_FOLLOW_AT_EDGE
                                val offset = swipeDrag * follow
                                scope.launch { tabSwipeOffset.snapTo(offset) }
                            }
                        }
                        .verticalEdgeFade(portraitScroll)
                        .verticalScroll(portraitScroll)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    header()
                    connectionCard()
                    categoryContent()
                    systemInfoCard()
                    authorizedWarning()
                    latestCommandCard()
                    errorText()
                }
            }
        }
    }
}

private fun connectionColor(connected: Boolean?): Color = when (connected) {
    true -> GlassColors.Green
    false -> GlassColors.Red
    null -> GlassColors.Amber
}

private fun statusColor(status: String?): Color = when (status) {
    "SUCCESS" -> GlassColors.Green
    "FAILED" -> GlassColors.Red
    else -> GlassColors.Amber
}

private fun formatRelativeTime(epochMillis: Long): String {
    val seconds = (System.currentTimeMillis() - epochMillis) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3_600} h ago"
        else -> "${seconds / 86_400} d ago"
    }
}

// A true alpha fade at the edges of a scrollable area (masking what's actually drawn, via
// BlendMode.DstIn) rather than painting an approximate-color overlay on top - so it looks right
// no matter what's behind it. Each edge only fades while there is more content hidden that way.
// CompositingStrategy.Offscreen renders the content to a buffer first so the mask only cuts into
// this content, not whatever is drawn behind it in the rest of the screen.
//
// ORDER MATTERS: put these BEFORE .verticalScroll()/.horizontalScroll() in the chain. Placed
// after, they'd wrap the full (unclipped, scrolled) content instead of the visible viewport,
// and the fades would sit off-screen at the content's own top/bottom.

private fun Modifier.verticalEdgeFade(state: ScrollState, edgeSize: Dp = 32.dp): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        val fadePx = edgeSize.toPx().coerceAtMost(size.height / 2f)
        if (state.value > 0) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = fadePx,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.value < state.maxValue) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - fadePx,
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }

private fun Modifier.horizontalEdgeFade(state: ScrollState, edgeSize: Dp = 28.dp): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        val fadePx = edgeSize.toPx().coerceAtMost(size.width / 2f)
        if (state.value > 0) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = fadePx,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.value < state.maxValue) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
