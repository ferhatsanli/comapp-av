package com.ferhat.comappav

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.ferhat.comappav.ui.glass.GlassScreen
import com.ferhat.comappav.ui.glass.LocalGlassPalette
import com.ferhat.comappav.ui.glass.ReorderableColumn
import com.ferhat.comappav.ui.glass.drawReorderIcon
import com.ferhat.comappav.ui.glass.glassTextFieldColors
import com.ferhat.comappav.ui.theme.ComAppAVTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private data class SleepRequest(val minutes: Int?)

private data class NetworkDevice(val mac: String, val ip: String, val name: String)

/** Every card that can be moved around in reorder mode; the names are what gets saved. */
private enum class CardId { PING, SPEAK, SLEEP, SHUTDOWN, POWER_MODE, SCAN, DEVICES, CONSOLE }

/**
 * Card groups, in their default order. The user can reorder both the categories and the cards
 * inside each one; [cards] is only the starting order.
 */
private enum class Category(
    val label: String,
    val contents: String,
    val accent: Color,
    val cards: List<CardId>,
) {
    COMMANDS("Commands", "Ping · Speak", GlassColors.Blue, listOf(CardId.PING, CardId.SPEAK)),
    POWER(
        "Power",
        "Sleep · Shutdown · Power mode",
        GlassColors.Amber,
        listOf(CardId.SLEEP, CardId.SHUTDOWN, CardId.POWER_MODE),
    ),
    NETWORK("Network", "Scan · Devices on the network", GlassColors.Neutral, listOf(CardId.SCAN, CardId.DEVICES)),
    CONSOLE("Console", "App log · errors · crashes", GlassColors.Green, listOf(CardId.CONSOLE)),
}

/** Key of the category order in [CardOrderStore]; each category's cards are saved under its name. */
private const val CATEGORY_ORDER_KEY = "categories"

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
)

private const val DATABASE_URL =
    "https://comapp-av-default-rtdb.europe-west1.firebasedatabase.app"

// Rough, all-in Dutch household electricity price (energy tax + VAT included),
// checked 2026-10-27 against the daily average on energievergelijk.nl. This is
// only for a ballpark "what does this cost" estimate next to the live reading,
// not a real tariff — edit this constant if you want a sharper number.
private const val ELECTRICITY_PRICE_EUR_PER_KWH = 0.324

private const val PENDING_WARNING_MILLIS = 15_000L

/** How many log lines the Console draws; "Copy all" always includes every saved entry. */
private const val CONSOLE_VISIBLE_ENTRIES = 150

/** The last characters of a Firebase push ID - enough to tell commands apart in the log. */
private fun shortId(id: String): String = id.takeLast(6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppLog.init(this)) {
            AppLog.info("App", "Started v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        }
        enableEdgeToEdge()
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
    var requestedPowerMode by remember { mutableStateOf<String?>(null) }
    // Portrait: which bottom-nav tab is active.
    var selectedCategory by rememberSaveable { mutableStateOf(Category.COMMANDS) }
    // Landscape: the category "folder" that's open in the left pane, or null for the category
    // list itself. [openProgress] is 1 when that folder is fully on screen and 0 when it's slid
    // away to the right; dragging it with a swipe-back gesture moves it in between.
    var openCategory by rememberSaveable { mutableStateOf<Category?>(null) }
    // Console: show only warnings and errors.
    var consoleErrorsOnly by rememberSaveable { mutableStateOf(false) }

    // Reorder mode, and the user's saved order of categories and of the cards in each one.
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val orderStore = remember { CardOrderStore(context) }
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
    val sleepTotal = (sleepHours.toIntOrNull() ?: 0) * 60 + (sleepMinutes.toIntOrNull() ?: 0)
    val sleepTotalValid = sleepTotal in 1..PingProtocol.MAX_SLEEP_MINUTES
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val authorized = user.uid == PingProtocol.ANDROID_UID
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
        openCategory = category
        selectedCategory = category // keep portrait's tab in sync if the phone is rotated
        scope.launch { openProgress.animateTo(1f, tween(durationMillis = 300)) }
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
        scope.launch {
            openProgress.animateTo(0f, tween(durationMillis = 250))
            openCategory = null
        }
    }

    fun sendCommand(action: String, payload: Map<String, Any>) {
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
            newCommand.setValue(payload)
                .addOnSuccessListener {
                    sending = false
                    commandAction = action
                    commandId = id
                    commandStatus = "PENDING"
                    AppLog.info("Command", "$action sent (${shortId(id)}), waiting for the agent")
                }
                .addOnFailureListener { exception ->
                    sending = false
                    error = exception.localizedMessage
                        ?: "Could not send $action. Check the connection and Rules."
                    AppLog.error("Command", "$action could not be sent: $error")
                }
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
                GlassChip("Sign out", onClick = {
                    AppLog.info("Auth", "Signed out")
                    auth.signOut()
                })
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
                onClick = { sendCommand(PingProtocol.ACTION, PingProtocol.pendingCommand(user.uid)) },
            )
        }
    }

    val speakCard: @Composable () -> Unit = {
        GlassCommandCard(
            title = "Speak",
            tag = PingProtocol.SPEAK_ACTION,
            description = "The laptop reads your text out loud.",
            accent = GlassColors.Indigo,
        ) {
            OutlinedTextField(
                value = speakText,
                onValueChange = { if (it.length <= PingProtocol.MAX_SPEAK_LENGTH) speakText = it },
                label = { Text("Text to speak on the laptop") },
                shape = GlassFieldShape,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
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
                onClick = {
                    sendCommand(
                        PingProtocol.SPEAK_ACTION,
                        PingProtocol.speakCommand(user.uid, speakText.trim()),
                    )
                },
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
                onClick = { sleepRequest = SleepRequest(null) },
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
                onClick = { sleepRequest = SleepRequest(sleepTotal) },
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
                onClick = { sendCommand(PingProtocol.SCAN_ACTION, PingProtocol.scanCommand(user.uid)) },
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
                onClick = { shutdownRequested = true },
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
                    loading = busy && requestedPowerMode == PingProtocol.MODE_PERFORMANCE,
                    onClick = {
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
                    loading = busy && requestedPowerMode == PingProtocol.MODE_POWERSAVE,
                    onClick = {
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
    val cardContent: @Composable (CardId) -> Unit = { id ->
        when (id) {
            CardId.PING -> pingCard()
            CardId.SPEAK -> speakCard()
            CardId.SLEEP -> sleepCard()
            CardId.SHUTDOWN -> shutdownCard()
            CardId.POWER_MODE -> powerModeCard()
            CardId.SCAN -> scanCard()
            CardId.DEVICES -> networkCard()
            CardId.CONSOLE -> consoleCard()
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
            modifier = Modifier.fillMaxWidth(),
        ) { category ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                categoryCards(category)
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
                            // folder, so the folder bar carries its own.
                            reorderButton()
                        }
                        categoryCards(category)
                    }
                }
            }
        }
    }

    val systemInfoCard: @Composable () -> Unit = {
        powerSnapshot?.let { power ->
            GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Amber, cornerRadius = 22.dp) {
                Text(
                    "System Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                power.reportedAt?.let { reportedAt ->
                    Text(
                        "Last report: ${formatRelativeTime(reportedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGlassPalette.current.inkMuted,
                    )
                }
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
                .padding(padding),
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
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .verticalEdgeFade(infoScroll)
                            .verticalScroll(infoScroll)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        connectionCard()
                        systemInfoCard()
                        authorizedWarning()
                        latestCommandCard()
                        errorText()
                    }
                }
            } else {
                val portraitScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
