package com.ferhat.comappav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ferhat.comappav.ui.theme.ComAppAVTheme

private const val DATABASE_URL =
    "https://comapp-av-default-rtdb.europe-west1.firebasedatabase.app"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        setContent {
            ComAppAVTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("ComApp AV", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Sign in with your Firebase Email/Password account to send a PING.")
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Button(
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
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) CircularProgressIndicator()
                else Text("Sign in")
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
    val authorized = user.uid == PingProtocol.ANDROID_UID

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

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ComApp AV", style = MaterialTheme.typography.headlineMedium)
                OutlinedButton(onClick = { auth.signOut() }) { Text("Sign out") }
            }
            Text(if (authorized) "Signed in as the Android test account" else "Signed in with an unrecognized account")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = when (connected) {
                            true -> "Realtime Database: connected"
                            false -> "Realtime Database: disconnected"
                            null -> "Realtime Database: checking connection…"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Text("Send a PING to rpi. The Raspberry Pi agent replies with PONG.")
            Button(
                onClick = {
                    val newCommand = database.getReference("commands").push()
                    val id = newCommand.key
                    if (id == null) {
                        error = "Could not allocate a command ID."
                    } else {
                        commandStatus = null
                        errorCode = null
                        error = null
                        sending = true
                        newCommand.setValue(PingProtocol.pendingCommand(user.uid))
                            .addOnSuccessListener {
                                sending = false
                                commandId = id
                                commandStatus = "PENDING"
                            }
                            .addOnFailureListener { exception ->
                                sending = false
                                error = exception.localizedMessage
                                    ?: "Could not send PING. Check the connection and Rules."
                            }
                    }
                },
                enabled = authorized &&
                    connected == true &&
                    !sending &&
                    commandStatus !in setOf("PENDING", "PROCESSING"),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (sending) CircularProgressIndicator()
                else Text("PING rpi")
            }
            if (!authorized) {
                Text(
                    "This account is not authorized for this milestone. Sign out and use the configured Android test account.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (commandId != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Latest command", style = MaterialTheme.typography.titleMedium)
                        Text("ID: $commandId")
                        Text("Status: ${commandStatus ?: if (sending) "SENDING" else "WAITING"}")
                        if (commandStatus == "FAILED" && errorCode != null) {
                            Text("Error: $errorCode", color = MaterialTheme.colorScheme.error)
                        }
                        if (commandStatus == "PENDING") {
                            Text("Waiting for the Pi agent.")
                        }
                        if (commandStatus == "PROCESSING") {
                            Text("The Pi agent is processing this PING.")
                        }
                        if (commandStatus == "SUCCESS") {
                            Text("PONG received from the Pi agent.")
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
