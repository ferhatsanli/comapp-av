package com.ferhatsanli.comapp

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.ferhatsanli.comapp.ui.glass.GlassButton
import com.ferhatsanli.comapp.ui.glass.GlassCard
import com.ferhatsanli.comapp.ui.glass.GlassChip
import com.ferhatsanli.comapp.ui.glass.GlassColors
import com.ferhatsanli.comapp.ui.glass.GlassFieldShape
import com.ferhatsanli.comapp.ui.glass.LocalGlassPalette
import com.ferhatsanli.comapp.ui.glass.glassTextFieldColors
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

private const val DEVICE_PREFS = "devices"
private const val SELECTED_DEVICE_KEY = "selected"

/** Firebase deletes an account only shortly after sign-in (5 minutes); stay well inside that. */
private const val RECENT_SIGN_IN_SECONDS = 240L

/** Sign-in: Google only. Nothing is stored on the phone besides Firebase's own session. */
@Composable
internal fun SignInScreen(auth: FirebaseAuth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun signIn() {
        isLoading = true
        error = null
        val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        scope.launch {
            try {
                val credential = CredentialManager.create(context).getCredential(context, request).credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                AppLog.info("Auth", "Signed in with Google")
                            } else {
                                error = task.exception?.localizedMessage ?: "Sign-in failed. Check the connection and try again."
                                AppLog.error("Auth", "Sign-in failed: $error")
                            }
                        }
                } else {
                    isLoading = false
                    error = "Google did not return an account. Please try again."
                    AppLog.error("Auth", "Unexpected credential type: ${credential.type}")
                }
            } catch (cancelled: GetCredentialCancellationException) {
                isLoading = false // the user closed the account picker: nothing to report
            } catch (failure: GetCredentialException) {
                isLoading = false
                error = failure.localizedMessage ?: "Could not open Google sign-in on this phone."
                AppLog.error("Auth", "Google sign-in could not start: ${failure.type}: $error")
            }
        }
    }

    Scaffold(containerColor = Color.Transparent, contentColor = LocalGlassPalette.current.ink) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Blue) {
                Text("ComApp", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Control your own computers from your phone. Sign in to pair a computer and send it commands.",
                    color = LocalGlassPalette.current.inkMuted,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                GlassButton(
                    text = "Continue with Google",
                    accent = GlassColors.Blue,
                    loading = isLoading,
                    enabled = !isLoading,
                    onClick = { signIn() },
                )
            }
            VersionLine()
        }
    }
}

@Composable
private fun VersionLine() {
    Text(
        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        style = MaterialTheme.typography.labelSmall,
        color = LocalGlassPalette.current.inkFaint,
        textAlign = TextAlign.Center,
    )
}

/** Signs out of Firebase and forgets the Google account choice, so the picker shows again. */
private suspend fun signOut(context: Context, auth: FirebaseAuth) {
    AppLog.info("Auth", "Signed out")
    auth.signOut()
    try {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    } catch (failure: Exception) {
        AppLog.warn("Auth", "Could not clear the Google sign-in state: ${failure.localizedMessage}")
    }
}

/**
 * After sign-in: the user's computers. With none paired it shows the pairing screen; otherwise
 * the main screen for the selected computer, with a menu to switch, add, rename and remove
 * computers, sign out, or delete the account.
 */
@Composable
internal fun SignedInScreen(user: FirebaseUser, auth: FirebaseAuth, database: FirebaseDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(DEVICE_PREFS, Context.MODE_PRIVATE) }
    // null while the list has not arrived yet.
    var devices by remember { mutableStateOf<List<Device>?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf(prefs.getString(SELECTED_DEVICE_KEY, null)) }
    var menuOpen by remember { mutableStateOf(false) }
    var addingDevice by remember { mutableStateOf(false) }

    fun select(id: String) {
        selectedId = id
        prefs.edit().putString(SELECTED_DEVICE_KEY, id).apply()
    }

    DisposableEffect(database, user.uid) {
        val ref = database.getReference(Devices.devicesPath(user.uid))
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listError = null
                devices = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    Devices.parse(id, (child.value as? Map<String, Any?>).orEmpty())
                }.sortedBy { it.name.lowercase() }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listError = databaseError.message
                AppLog.error("Devices", "Could not read the list of computers: ${databaseError.message}")
            }
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val list = devices
    val device = list?.firstOrNull { it.id == selectedId } ?: list?.firstOrNull()
    when {
        list == null -> CenteredMessage(listError ?: "Loading your computers…")
        device == null || addingDevice -> PairingScreen(
            user = user,
            database = database,
            canCancel = device != null,
            onCancel = { addingDevice = false },
            onPaired = { id ->
                select(id)
                addingDevice = false
            },
            onSignOut = { scope.launch { signOut(context, auth) } },
        )
        else -> key(device.id) {
            PingScreen(user = user, device = device, database = database, onDeviceMenu = { menuOpen = true })
        }
    }

    if (menuOpen && list != null && device != null) {
        DeviceMenu(
            user = user,
            database = database,
            devices = list,
            current = device,
            onSelect = { id ->
                select(id)
                menuOpen = false
            },
            onAdd = {
                menuOpen = false
                addingDevice = true
            },
            onDismiss = { menuOpen = false },
            onSignOut = {
                menuOpen = false
                scope.launch { signOut(context, auth) }
            },
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = LocalGlassPalette.current.inkMuted, textAlign = TextAlign.Center)
    }
}

/** Typing the 8-character code a freshly installed agent shows on the computer. */
@Composable
private fun PairingScreen(
    user: FirebaseUser,
    database: FirebaseDatabase,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onPaired: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val palette = LocalGlassPalette.current

    fun pair() {
        if (!Devices.isValidCode(code)) {
            error = "The code has 8 letters and digits. Check it on the computer."
            return
        }
        busy = true
        error = null
        database.getReference(Devices.PAIRING_PATH).orderByChild("code").equalTo(code).limitToFirst(1).get()
            .addOnSuccessListener { snapshot ->
                val found = snapshot.children.firstOrNull()
                val agentUid = found?.key
                if (found == null || agentUid == null) {
                    busy = false
                    error = "No computer is showing this code right now. Check the code, or start pairing again on the computer."
                    return@addOnSuccessListener
                }
                val offer = PairingOffer(
                    agentUid = agentUid,
                    name = found.child("name").getValue(String::class.java).orEmpty(),
                    platform = found.child("platform").getValue(String::class.java) ?: "linux",
                )
                database.reference.updateChildren(Devices.claimUpdate(user.uid, offer, code))
                    .addOnSuccessListener {
                        busy = false
                        AppLog.info("Devices", "Paired a ${Devices.platformLabel(offer.platform)} computer")
                        onPaired(agentUid)
                    }
                    .addOnFailureListener { failure ->
                        busy = false
                        // The Rules refuse a code older than 10 minutes or an agent that already has an owner.
                        error = "This code is no longer valid. Start pairing again on the computer to get a new one."
                        AppLog.error("Devices", "Pairing was refused: ${failure.localizedMessage}")
                    }
            }
            .addOnFailureListener { failure ->
                busy = false
                error = failure.localizedMessage ?: "Could not check the code. Check the connection and try again."
                AppLog.error("Devices", "Could not look the code up: $error")
            }
    }

    Scaffold(containerColor = Color.Transparent, contentColor = palette.ink) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            GlassCard(Modifier.fillMaxWidth(), accent = GlassColors.Blue) {
                Text("Add a computer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "1. Install the ComApp agent on the computer you want to control. The steps are at " +
                        "github.com/ferhatsanli/comapp-agent.\n" +
                        "2. The agent shows an 8-character code. Type it here within 10 minutes.",
                    color = palette.inkMuted,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = Devices.normalizeCode(it)
                        error = null
                    },
                    label = { Text("Code from the computer") },
                    singleLine = true,
                    enabled = !busy,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { pair() }),
                    shape = GlassFieldShape,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                GlassButton(
                    text = "Pair this computer",
                    accent = GlassColors.Blue,
                    loading = busy,
                    enabled = !busy && code.length == Devices.CODE_LENGTH,
                    onClick = { pair() },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canCancel) GlassChip("Back", onClick = onCancel)
                GlassChip("Sign out", onClick = onSignOut)
            }
            VersionLine()
        }
    }
}

private enum class MenuConfirm { REMOVE, SIGN_OUT, DELETE_ACCOUNT }

/** The "Computers" menu: switch, add, rename, remove; sign out; delete the account. */
@Composable
private fun DeviceMenu(
    user: FirebaseUser,
    database: FirebaseDatabase,
    devices: List<Device>,
    current: Device,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    var name by remember(current.id) { mutableStateOf(current.name) }
    var confirm by remember { mutableStateOf<MenuConfirm?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun rename() {
        val cleaned = Devices.cleanName(name)
        if (cleaned.isEmpty() || cleaned == current.name) return
        database.getReference("${Devices.devicesPath(user.uid)}/${current.id}/name").setValue(cleaned)
            .addOnSuccessListener { AppLog.info("Devices", "Renamed a computer") }
            .addOnFailureListener { failure ->
                error = failure.localizedMessage ?: "Could not rename the computer."
                AppLog.error("Devices", "Rename failed: $error")
            }
    }

    fun remove() {
        busy = true
        database.reference.updateChildren(Devices.unpairUpdate(user.uid, current.id))
            .addOnSuccessListener {
                busy = false
                AppLog.info("Devices", "Removed a computer")
                onDismiss()
            }
            .addOnFailureListener { failure ->
                busy = false
                error = failure.localizedMessage ?: "Could not remove the computer."
                AppLog.error("Devices", "Remove failed: $error")
            }
    }

    // Play requires that an account made in the app can be deleted in the app: the data first
    // (while the Rules still know the user), then the account itself.
    fun deleteAccount() {
        busy = true
        error = null
        // Firebase only deletes an account that signed in a few minutes ago. Check that first,
        // so the data is never deleted while the account itself has to stay.
        user.getIdToken(false)
            .addOnSuccessListener { token ->
                if (System.currentTimeMillis() / 1000 - token.authTimestamp > RECENT_SIGN_IN_SECONDS) {
                    busy = false
                    error = "For your security, please sign out, sign in again and then choose Delete account right away."
                    return@addOnSuccessListener
                }
                database.reference.updateChildren(Devices.deleteAccountUpdate(user.uid, devices.map { it.id }))
                    .addOnSuccessListener {
                        user.delete().addOnCompleteListener { task ->
                            busy = false
                            if (task.isSuccessful) {
                                AppLog.info("Auth", "Account deleted")
                                onSignOut()
                            } else {
                                error = task.exception?.localizedMessage
                                    ?: "Your data was deleted, but the account could not be removed. Please try again."
                                AppLog.error("Auth", "Account deletion failed: $error")
                            }
                        }
                    }
                    .addOnFailureListener { failure ->
                        busy = false
                        error = failure.localizedMessage ?: "Your data could not be deleted. Please try again."
                        AppLog.error("Auth", "Deleting the account's data failed: $error")
                    }
            }
            .addOnFailureListener { failure ->
                busy = false
                error = failure.localizedMessage ?: "Could not reach the server. Please try again."
            }
    }

    when (confirm) {
        MenuConfirm.REMOVE -> ConfirmDialog(
            title = "Remove ${current.name}?",
            text = "The phone can no longer control this computer, and its reports are deleted. " +
                "You can pair it again later with a new code.",
            action = "Remove",
            onConfirm = { confirm = null; remove() },
            onDismiss = { confirm = null },
        )
        MenuConfirm.SIGN_OUT -> ConfirmDialog(
            title = "Sign out?",
            text = "Your computers stay paired. Sign in again to control them.",
            action = "Sign out",
            onConfirm = { confirm = null; onSignOut() },
            onDismiss = { confirm = null },
        )
        MenuConfirm.DELETE_ACCOUNT -> ConfirmDialog(
            title = "Delete your account?",
            text = "This removes all your paired computers, their reports and your ComApp account. " +
                "It cannot be undone. The agent stays installed on your computers until you uninstall it there.",
            action = "Delete account",
            onConfirm = { confirm = null; deleteAccount() },
            onDismiss = { confirm = null },
        )
        null -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Computers") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    devices.forEach { device ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(device.id) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (device.id == current.id) "●" else "○",
                                color = if (device.id == current.id) GlassColors.Green else palette.inkFaint,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.Medium)
                                Text(
                                    listOfNotNull(Devices.platformLabel(device.platform), device.hostname).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkMuted,
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(Devices.MAX_NAME_LENGTH) },
                        label = { Text("Name of this computer") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { rename() }),
                        shape = GlassFieldShape,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (Devices.cleanName(name).let { it.isNotEmpty() && it != current.name }) {
                        TextButton(onClick = { rename() }) { Text("Save name") }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = onAdd, enabled = !busy) { Text("Add a computer") }
                    TextButton(onClick = { confirm = MenuConfirm.REMOVE }, enabled = !busy) { Text("Remove this computer") }
                    TextButton(onClick = { confirm = MenuConfirm.SIGN_OUT }, enabled = !busy) { Text("Sign out") }
                    TextButton(onClick = { confirm = MenuConfirm.DELETE_ACCOUNT }, enabled = !busy) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        user.email.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkFaint,
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
