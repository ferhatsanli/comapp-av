package com.ferhatsanli.comapp

import com.ferhatsanli.comapp.ui.glass.GlassTextField
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
import androidx.compose.ui.res.stringResource
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
                                error = task.exception?.localizedMessage ?: context.getString(R.string.signin_failed)
                                AppLog.error("Auth", "Sign-in failed: $error")
                            }
                        }
                } else {
                    isLoading = false
                    error = context.getString(R.string.signin_no_account)
                    AppLog.error("Auth", "Unexpected credential type: ${credential.type}")
                }
            } catch (cancelled: GetCredentialCancellationException) {
                isLoading = false // the user closed the account picker: nothing to report
            } catch (failure: GetCredentialException) {
                isLoading = false
                error = failure.localizedMessage ?: context.getString(R.string.signin_cannot_open)
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
                    stringResource(R.string.signin_intro),
                    color = LocalGlassPalette.current.inkMuted,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                GlassButton(
                    text = stringResource(R.string.signin_google),
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
        list == null -> CenteredMessage(listError ?: stringResource(R.string.computers_loading))
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
    val context = LocalContext.current

    fun pair() {
        if (!Devices.isValidCode(code)) {
            error = context.getString(R.string.pair_code_format)
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
                    error = context.getString(R.string.pair_code_unknown)
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
                        error = context.getString(R.string.pair_code_expired)
                        AppLog.error("Devices", "Pairing was refused: ${failure.localizedMessage}")
                    }
            }
            .addOnFailureListener { failure ->
                busy = false
                error = failure.localizedMessage ?: context.getString(R.string.pair_code_check_failed)
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
                Text(stringResource(R.string.pair_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.pair_steps),
                    color = palette.inkMuted,
                )
                GlassTextField(
                    value = code,
                    onValueChange = {
                        code = Devices.normalizeCode(it)
                        error = null
                    },
                    label = stringResource(R.string.pair_code_label),
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
                    modifier = Modifier.fillMaxWidth(),
                    clearDescription = stringResource(R.string.pair_code_clear),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                GlassButton(
                    text = stringResource(R.string.pair_button),
                    accent = GlassColors.Blue,
                    loading = busy,
                    enabled = !busy && code.length == Devices.CODE_LENGTH,
                    onClick = { pair() },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canCancel) GlassChip(stringResource(R.string.common_back), onClick = onCancel)
                GlassChip(stringResource(R.string.account_sign_out), onClick = onSignOut)
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
    val context = LocalContext.current
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
                error = failure.localizedMessage ?: context.getString(R.string.account_rename_failed)
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
                error = failure.localizedMessage ?: context.getString(R.string.account_remove_failed)
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
                    error = context.getString(R.string.account_delete_reauth)
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
                                    ?: context.getString(R.string.account_delete_partial)
                                AppLog.error("Auth", "Account deletion failed: $error")
                            }
                        }
                    }
                    .addOnFailureListener { failure ->
                        busy = false
                        error = failure.localizedMessage ?: context.getString(R.string.account_delete_data_failed)
                        AppLog.error("Auth", "Deleting the account's data failed: $error")
                    }
            }
            .addOnFailureListener { failure ->
                busy = false
                error = failure.localizedMessage ?: context.getString(R.string.account_server_unreachable)
            }
    }

    when (confirm) {
        MenuConfirm.REMOVE -> ConfirmDialog(
            title = stringResource(R.string.account_remove_title, current.name),
            text = stringResource(R.string.account_remove_text),
            action = stringResource(R.string.account_remove),
            onConfirm = { confirm = null; remove() },
            onDismiss = { confirm = null },
        )
        MenuConfirm.SIGN_OUT -> ConfirmDialog(
            title = stringResource(R.string.account_sign_out_title),
            text = stringResource(R.string.account_sign_out_text),
            action = stringResource(R.string.account_sign_out),
            onConfirm = { confirm = null; onSignOut() },
            onDismiss = { confirm = null },
        )
        MenuConfirm.DELETE_ACCOUNT -> ConfirmDialog(
            title = stringResource(R.string.account_delete_title),
            text = stringResource(R.string.account_delete_text),
            action = stringResource(R.string.account_delete),
            onConfirm = { confirm = null; deleteAccount() },
            onDismiss = { confirm = null },
        )
        null -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.computers_title)) },
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
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it.take(Devices.MAX_NAME_LENGTH) },
                        label = stringResource(R.string.computers_name_label),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { rename() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (Devices.cleanName(name).let { it.isNotEmpty() && it != current.name }) {
                        TextButton(onClick = { rename() }) { Text(stringResource(R.string.computers_save_name)) }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = onAdd, enabled = !busy) { Text(stringResource(R.string.pair_title)) }
                    TextButton(onClick = { confirm = MenuConfirm.REMOVE }, enabled = !busy) { Text(stringResource(R.string.computers_remove_this)) }
                    TextButton(onClick = { confirm = MenuConfirm.SIGN_OUT }, enabled = !busy) { Text(stringResource(R.string.account_sign_out)) }
                    TextButton(onClick = { confirm = MenuConfirm.DELETE_ACCOUNT }, enabled = !busy) {
                        Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        user.email.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkFaint,
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
