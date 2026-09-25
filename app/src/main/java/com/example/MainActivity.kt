package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.gemini.GeminiService
import com.example.data.repository.FirestoreRepository
import com.example.ui.MainApp
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.theme.ZariaTheme
import com.example.util.AudioHelper
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private lateinit var audioHelper: AudioHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "FirebaseApp init warning: ${e.message}")
        }

        audioHelper = AudioHelper(this)

        setContent {
            ZariaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(audioHelper = audioHelper)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHelper.release()
    }
}

@Composable
fun AppRoot(audioHelper: AudioHelper) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isGuestMode by remember { mutableStateOf(false) }

    // Safely obtain currentUser without crashing if Firebase is initializing
    var currentUser by remember {
        mutableStateOf<FirebaseUser?>(
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    Firebase.auth.currentUser
                } else null
            } catch (e: Exception) {
                Log.d("AppRoot", "Firebase not ready: ${e.message}")
                null
            }
        )
    }

    val credentialManager = remember {
        try {
            CredentialManager.create(context)
        } catch (_: Exception) {
            null
        }
    }

    // Attempt silent auto-sign in on startup if credentials exist
    LaunchedEffect(Unit) {
        if (currentUser == null && credentialManager != null) {
            try {
                attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        try {
                            currentUser = Firebase.auth.currentUser
                        } catch (_: Exception) {}
                    },
                    onUnauthenticated = { /* Stay on login / guest gate */ },
                    scope = scope
                )
            } catch (e: Exception) {
                Log.d("AppRoot", "Silent sign-in skipped: ${e.message}")
            }
        }
    }

    // Keep auth state synced with Firebase safely
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.auth.addAuthStateListener(listener)
            }
        } catch (_: Exception) {}

        onDispose {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            } catch (_: Exception) {}
        }
    }

    if (currentUser == null && !isGuestMode) {
        // Unauthenticated gate: Sign in with Google OR Continue as Farmer Guest
        AuthScreen(
            onAuthSuccess = {
                try {
                    currentUser = Firebase.auth.currentUser
                } catch (_: Exception) {}
            },
            onContinueAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        // Authenticated user or Farmer Guest
        val firestoreRepository = remember { FirestoreRepository(context) }
        val geminiService = remember { GeminiService() }

        MainApp(
            geminiService = geminiService,
            firestoreRepository = firestoreRepository,
            audioHelper = audioHelper,
            onSignOutComplete = {
                currentUser = null
                isGuestMode = false
            }
        )
    }
}
