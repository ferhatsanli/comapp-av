package com.ferhat.comappav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ferhat.comappav.ui.glass.GlassButton
import com.ferhat.comappav.ui.glass.GlassCard
import com.ferhat.comappav.ui.glass.GlassChip
import com.ferhat.comappav.ui.glass.GlassColors
import com.ferhat.comappav.ui.glass.GlassCommandCard
import com.ferhat.comappav.ui.glass.GlassFieldShape
import com.ferhat.comappav.ui.glass.GlassScreen
import com.ferhat.comappav.ui.glass.glassTextFieldColors
import com.ferhat.comappav.ui.theme.ComAppAVTheme

private data class SleepRequest(val minutes: Int?)

private const val DATABASE_URL =
    "https://comapp-av-default-rtdb.europe-west1.firebasedatabase.app"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
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

    Scaffold(containerColor = Color.Transparent, contentColor = Color.White) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Cyan) {
                Text(
                    "ComApp AV",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Sign in with your Firebase Email/Password account to send commands.",
                    color = Color.White.copy(alpha = 0.70f),
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
                    accent = GlassColors.Cyan,
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
                                    } else {
                                        error = task.exception?.localizedMessage
                                            ?: "Sign-in failed. Check the account and connection."
                                    }
                                }
                        }
                    },
                )
            }
        }
    }
}

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
    var sleepHours by rememberSaveable { mutableStateOf("0") }
    var sleepMinutes by rememberSaveable { mutableStateOf("30") }
    val sleepTotal = (sleepHours.toIntOrNull() ?: 0) * 60 + (sleepMinutes.toIntOrNull() ?: 0)
    val sleepTotalValid = sleepTotal in 1..PingProtocol.MAX_SLEEP_MINUTES
    val authorized = user.uid == PingProtocol.ANDROID_UID
    val canSend = authorized &&
        connected == true &&
        !sending &&
        commandStatus !in setOf("PENDING", "PROCESSING")

    fun sendCommand(action: String, payload: Map<String, Any>) {
        val newCommand = database.getReference("commands").push()
        val id = newCommand.key
        if (id == null) {
            error = "Could not allocate a command ID."
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
                }
                .addOnFailureListener { exception ->
                    sending = false
                    error = exception.localizedMessage
                        ?: "Could not send $action. Check the connection and Rules."
                }
        }
    }

    DisposableEffect(database) {
        val connectionRef = database.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                connected = snapshot.getValue(Boolean::class.java) ?: false
            }

            override fun onCancelled(databaseError: DatabaseError) {
                connected = false
                error = databaseError.message
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
                    commandStatus = snapshot.child("status").getValue(String::class.java)
                    errorCode = snapshot.child("errorCode").getValue(String::class.java)
                    error = null
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    error = "Could not observe command: ${databaseError.message}"
                }
            }
            commandRef.addValueEventListener(listener)
            onDispose { commandRef.removeEventListener(listener) }
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

    Scaffold(containerColor = Color.Transparent, contentColor = Color.White) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
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
                        color = Color.White.copy(alpha = 0.70f),
                    )
                }
                GlassChip("Sign out", onClick = { auth.signOut() })
            }

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
                            false -> "Realtime Database: disconnected"
                            null -> "Realtime Database: checking connection…"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            GlassCommandCard(
                title = "Ping",
                tag = PingProtocol.ACTION,
                description = "Check that the agent is alive. It answers with PONG.",
                accent = GlassColors.Cyan,
            ) {
                GlassButton(
                    text = "Send PING",
                    accent = GlassColors.Cyan,
                    enabled = canSend,
                    loading = sending && commandAction == PingProtocol.ACTION,
                    onClick = { sendCommand(PingProtocol.ACTION, PingProtocol.pendingCommand(user.uid)) },
                )
            }

            GlassCommandCard(
                title = "Speak",
                tag = PingProtocol.SPEAK_ACTION,
                description = "The laptop reads your text out loud.",
                accent = GlassColors.Pink,
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
                    color = Color.White.copy(alpha = 0.60f),
                )
                GlassButton(
                    text = "Send SPEAK",
                    accent = GlassColors.Pink,
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
                    color = Color.White.copy(alpha = 0.70f),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
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

            if (!authorized) {
                GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Red, cornerRadius = 22.dp) {
                    Text(
                        "This account is not authorized for this milestone. Sign out and use the configured Android test account.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

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
                                    else -> "PONG received from the agent."
                                },
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
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
