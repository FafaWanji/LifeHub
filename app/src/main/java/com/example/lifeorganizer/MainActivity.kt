package com.example.lifeorganizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.lifeorganizer.core.i18n.ProvideAppLocale
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.core.theme.AppTheme
import com.example.lifeorganizer.ui.MainScreen

class MainActivity : androidx.fragment.app.FragmentActivity() {

    companion object {
        const val ACTION_SMART_ADD = "com.example.lifeorganizer.ACTION_SMART_ADD"
        const val ACTION_SMART_ADD_VOICE = "com.example.lifeorganizer.ACTION_SMART_ADD_VOICE"
        const val ACTION_SMART_ADD_SCAN = "com.example.lifeorganizer.ACTION_SMART_ADD_SCAN"
        const val ACTION_NEW_EVENT = "com.example.lifeorganizer.ACTION_NEW_EVENT"
        const val ACTION_NEW_NOTE = "com.example.lifeorganizer.ACTION_NEW_NOTE"
        const val ACTION_WAYPOINTS = "com.example.lifeorganizer.ACTION_WAYPOINTS"
        const val ACTION_SEARCH = "com.example.lifeorganizer.ACTION_SEARCH"
        const val ACTION_AGENDA = "com.example.lifeorganizer.ACTION_AGENDA"
        const val ACTION_OPEN_NOTE = com.example.lifeorganizer.calendar.alarm.NotificationHelper.ACTION_OPEN_NOTE

        /** Re-lock when the app was in the background longer than this. */
        private const val RELOCK_AFTER_MS = 30_000L
    }

    private var sharedText by mutableStateOf<String?>(null)
    private var sharedImageUri by mutableStateOf<Uri?>(null)
    private var currentIntentAction by mutableStateOf<String?>(null)
    private var noteToOpen by mutableStateOf<Long?>(null)
    private var importUri by mutableStateOf<Uri?>(null)
    private lateinit var settingsManager: SettingsManager

    // App lock state lives in the activity so it survives recomposition and can be reset on background.
    private var isUnlocked by mutableStateOf(false)
    private var lockError by mutableStateOf<String?>(null)
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.lifeorganizer.backup.AutoBackup.schedule(applicationContext)

        // Check if we're restarting after a crash — show the crash info
        val prefs = getSharedPreferences("crash_reporter", MODE_PRIVATE)
        val savedCrash = prefs.getString("last_crash", null)
        if (savedCrash != null) {
            // Clear it so we don't show it again next launch
            prefs.edit().remove("last_crash").apply()
            setContent {
                CrashReportScreen(
                    error = savedCrash,
                    onRetry = {
                        // Restart fresh
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finishAffinity()
                    }
                )
            }
            return
        }

        // Install crash handler that saves the crash and restarts the activity
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashText = throwable.stackTraceToString()
                Log.e("LifeOrganizer", "CRASH: $crashText")
                getSharedPreferences("crash_reporter", MODE_PRIVATE)
                    .edit()
                    .putString("last_crash", crashText)
                    .commit() // Use commit() not apply() since we're about to crash
            } catch (_: Exception) { }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()

        try {
            settingsManager = SettingsManager(applicationContext)
            if (savedInstanceState != null) isUnlocked = savedInstanceState.getBoolean("unlocked", false)
            handleIntent(intent)
        } catch (e: Exception) {
            Log.e("LifeOrganizer", "Init crash", e)
            getSharedPreferences("crash_reporter", MODE_PRIVATE)
                .edit().putString("last_crash", "INIT: ${e.stackTraceToString()}").commit()
            recreate()
            return
        }

        setContent {
            val designStyle by settingsManager.designStyle.collectAsState(initial = "pastel")
            val isDarkTheme by settingsManager.isDarkTheme.collectAsState(initial = null)
            val accentColor by settingsManager.accentColor.collectAsState(initial = null)
            val language by settingsManager.languageCode.collectAsState(initial = "en")
            // null until DataStore has answered, so locked content never flashes on screen.
            val enableAppLock by settingsManager.enableAppLock.collectAsState(initial = null)

            LaunchedEffect(enableAppLock, isUnlocked) {
                if (enableAppLock == true && !isUnlocked) showBiometricPrompt()
            }

            ProvideAppLocale(language) {
                AppTheme(
                    designStyle = designStyle,
                    darkTheme = isDarkTheme ?: isSystemInDarkTheme(),
                    accentColor = accentColor
                ) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        when (enableAppLock) {
                            null -> Unit
                            else -> {
                                val locked = enableAppLock == true && !isUnlocked
                                MainScreen(
                                    initialSharedText = sharedText,
                                    initialSharedImageUri = sharedImageUri,
                                    initialIntentAction = currentIntentAction,
                                    initialNoteId = noteToOpen,
                                    importUri = importUri,
                                    onImportHandled = { importUri = null },
                                    // Hold pending intents until the user has unlocked.
                                    intentsEnabled = !locked,
                                    onSharedHandled = {
                                        sharedText = null
                                        sharedImageUri = null
                                        currentIntentAction = null
                                        noteToOpen = null
                                    }
                                )
                                // Opaque overlay instead of replacing MainScreen keeps open editors/dialogs intact.
                                if (locked) {
                                    AppLockScreen(
                                        error = lockError,
                                        onUnlock = { showBiometricPrompt() },
                                        onBack = { moveTaskToBack(true) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
            canAuth == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
            canAuth == BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
        ) {
            // No screen lock / biometrics configured: don't lock the user out of their own data.
            isUnlocked = true
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockError = null
                    isUnlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelling keeps the lock screen with an "Unlock" button instead of closing the app.
                    lockError = if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) null else errString.toString()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(Str.unlockSubtitle.of(java.util.Locale.getDefault().language))
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(promptInfo)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt != 0L && SystemClock.elapsedRealtime() - backgroundedAt > RELOCK_AFTER_MS) {
            isUnlocked = false
        }
        backgroundedAt = 0L
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", isUnlocked)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun handleIntent(intent: Intent) {
        currentIntentAction = intent.action
        if (intent.action == ACTION_OPEN_NOTE) {
            noteToOpen = intent.getLongExtra(com.example.lifeorganizer.calendar.alarm.NotificationHelper.EXTRA_NOTE_ID, -1L).takeIf { it > 0 }
        }
        // Backups / calendar files shared to LifeOrganizer or opened from a file manager
        val type = intent.type.orEmpty()
        val isBackupFile = type.startsWith("text/calendar") || type.startsWith("text/x-vcalendar") || type == "application/json"
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            importUri = intent.data
            return
        }
        if (intent.action == Intent.ACTION_SEND && isBackupFile) {
            importUri = if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            return
        }
        if (intent.action == Intent.ACTION_SEND) {
            when {
                intent.type?.startsWith("text/") == true -> {
                    sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                }
                intent.type?.startsWith("image/") == true -> {
                    sharedImageUri = if (android.os.Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockScreen(error: String?, onUnlock: () -> Unit, onBack: () -> Unit) {
    // Swallow back so screens underneath the overlay are not navigated while locked.
    BackHandler(onBack = onBack)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(Str.locked.text(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                error ?: Str.unlockSubtitle.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onUnlock) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(Str.unlock.text())
            }
        }
    }
}

@Composable
fun CrashReportScreen(error: String, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A1A2E)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "❌ LifeOrganizer Crashed",
                color = Color(0xFFE94560),
                fontSize = 22.sp,
                modifier = Modifier.padding(top = 48.dp)
            )
            Text(
                "Please screenshot this and share it so the crash can be fixed:",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Text(
                text = error,
                color = Color(0xFF00FF88),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
