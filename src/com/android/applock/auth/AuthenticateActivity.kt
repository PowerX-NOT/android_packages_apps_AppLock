package com.android.applock.auth

import android.app.Activity
import android.app.AppLockManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.android.applock.auth.security.SecurityType
import com.android.applock.auth.security.AppLockSecurityManager
import com.android.applock.auth.ui.CredentialsRequiredScreen
import com.android.applock.auth.ui.LockScreen
import com.android.applock.auth.ui.PasswordScreen
import com.android.applock.auth.ui.PatternScreen
import com.android.applock.auth.ui.theme.AppLockAuthTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthenticateActivity : ComponentActivity() {

    private enum class AuthState { IDLE, PROMPT_SHOWING, EXITING, FINISHED }

    private lateinit var securityManager: AppLockSecurityManager
    private var packageName: String? = null
    private var userId: Int = 0
    private var lockedUid: Int = 0
    private var appLabel: String = "App"

    private enum class AuthLoadState { LOADING, READY, NOT_CONFIGURED }

    private var authState: AuthState = AuthState.IDLE
    private var biometricCancellationSignal: CancellationSignal? = null

    private val isExiting = mutableStateOf(false)
    private val hasWindowFocus = mutableStateOf(false)
    private val securitySnapshot = mutableStateOf<SecuritySnapshot?>(null)

    private data class SecuritySnapshot(
        val securityType: SecurityType,
        val biometricType: AppLockSecurityManager.BiometricType,
        val isPreferBiometric: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, lifecycleTag("onCreate") + " savedState=" + (savedInstanceState != null)
                + " intentAction=" + intent?.action + " extras=" + intent?.extras?.keySet())

        setupWindowForOverlay()
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startExitAnimation(success = false)
            }
        })

        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            ?: intent.getStringExtra(EXTRA_LOCKED_PACKAGE)

        lockedUid = intent.getIntExtra(EXTRA_LOCKED_UID, 0)
        userId = intent.getIntExtra(EXTRA_USER_ID, 0).takeIf { it != 0 }
            ?: UserHandle.getUserId(lockedUid)

        appLabel = intent.getStringExtra(EXTRA_APP_LABEL)
            ?: packageName
            ?: "App"

        securityManager = AppLockSecurityManager(this)

        val loadState = mutableStateOf(AuthLoadState.LOADING)

        lifecycleScope.launch {
            val snap = withContext(Dispatchers.IO) {
                if (!securityManager.isSetup()) return@withContext null
                val bioType = if (securityManager.isBiometricEnabled()
                                  && securityManager.isBiometricAvailable()) {
                    securityManager.getBiometricType()
                } else {
                    AppLockSecurityManager.BiometricType.NONE
                }
                SecuritySnapshot(
                    securityType = securityManager.getSecurityType(),
                    biometricType = bioType,
                    isPreferBiometric = securityManager.isPreferBiometric()
                )
            }
            if (snap == null) {
                loadState.value = AuthLoadState.NOT_CONFIGURED
            } else {
                securitySnapshot.value = snap
                loadState.value = AuthLoadState.READY
            }
        }

        setContent {
            AppLockAuthTheme {
                val snap = securitySnapshot.value
                val focused = hasWindowFocus.value
                val state = loadState.value

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        state == AuthLoadState.NOT_CONFIGURED -> {
                            CredentialsRequiredScreen(
                                appLabel = appLabel,
                                onSetPassword = {
                                    startActivity(
                                        Intent(
                                            this@AuthenticateActivity,
                                            SetupCredentialsActivity::class.java,
                                        ),
                                    )
                                    cancelAndFinish()
                                },
                                onCancel = { startExitAnimation(success = false) },
                            )
                        }
                        focused && state == AuthLoadState.READY && snap != null -> {
                        AuthenticateScreen(
                            securityManager = securityManager,
                            securityType = snap.securityType,
                            appLabel = appLabel,
                            onSuccess = { startExitAnimation(success = true) },
                            onCancel = { startExitAnimation(success = false) },
                            biometricType = snap.biometricType,
                            onBiometricClick = { showBiometricPrompt() },
                            isPreferBiometric = snap.isPreferBiometric,
                            isExiting = isExiting.value
                        )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, lifecycleTag("onWindowFocusChanged") + " hasFocus=" + hasFocus)
        if (hasFocus && !hasWindowFocus.value) {
            lifecycleScope.launch {
                delay(UI_DEFER_MS)
                hasWindowFocus.value = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, lifecycleTag("onStart"))
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, lifecycleTag("onResume"))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, lifecycleTag("onNewIntent") + " action=" + intent.action
                + " extras=" + intent.extras?.keySet())
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, lifecycleTag("onRestart") + " RESTART - activity instance was reused!")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, lifecycleTag("onStop") + " isFinishing=" + isFinishing)
    }

    private val isSystemUnlock: Boolean
        get() = intent?.action == ACTION_SYSTEM_UNLOCK

    private fun lifecycleTag(phase: String): String {
        val inst = Integer.toHexString(System.identityHashCode(this))
        return "$phase pid=${Process.myPid()} inst=$inst task=$taskId state=$authState finishing=$isFinishing"
    }

    private fun startExitAnimation(success: Boolean) {
        if (authState == AuthState.EXITING || authState == AuthState.FINISHED) return
        authState = AuthState.EXITING
        isExiting.value = true

        lifecycleScope.launch {
            delay(EXIT_ANIMATION_MS)
            if (success) unlockAndFinish() else cancelAndFinish()
        }
    }

    private fun showBiometricPrompt() {
        if (authState != AuthState.IDLE || isFinishing) return
        authState = AuthState.PROMPT_SHOWING

        val negativeButtonText = when (securitySnapshot.value?.securityType) {
            SecurityType.PIN -> "Use PIN"
            SecurityType.PASSWORD -> "Use Password"
            SecurityType.PATTERN -> "Use Pattern"
            else -> "Cancel"
        }

        val prompt = BiometricPrompt.Builder(this)
            .setTitle("Unlock $appLabel")
            .setNegativeButton(negativeButtonText, mainExecutor) { _, _ ->
                if (authState == AuthState.PROMPT_SHOWING) authState = AuthState.IDLE
            }
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        biometricCancellationSignal?.cancel()
        val signal = CancellationSignal()
        biometricCancellationSignal = signal

        prompt.authenticate(
            signal,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    biometricCancellationSignal = null
                    if (authState == AuthState.PROMPT_SHOWING) {
                        authState = AuthState.IDLE
                        startExitAnimation(success = true)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    biometricCancellationSignal = null
                    val wasShowing = authState == AuthState.PROMPT_SHOWING
                    if (wasShowing) authState = AuthState.IDLE
                    if (wasShowing
                        && errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                        && errorCode != BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON
                        && errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                        cancelAndFinish()
                    }
                }
            }
        )
    }

    private fun setupWindowForOverlay() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        window?.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
            attributes = attributes?.apply {
                privateFlags = privateFlags or
                    WindowManager.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS
            }
        }

        val km = getSystemService(KeyguardManager::class.java)
        km?.requestDismissKeyguard(this, null)
    }

    private fun buildResultData(): Intent = Intent().apply {
        putExtra(EXTRA_LOCKED_PACKAGE, packageName)
        putExtra(EXTRA_LOCKED_UID, lockedUid)
    }

    private fun notifySessionUnlocked() {
        val pkg = packageName ?: return
        Log.i(TAG, "[Session] notifySessionUnlocked pkg=$pkg userId=$userId systemUnlock=$isSystemUnlock")
        (getSystemService(Context.APP_LOCK_SERVICE) as? AppLockManager)
            ?.unlockApp(pkg, userId)
    }

    private fun unlockAndFinish() {
        if (authState == AuthState.FINISHED || isFinishing) return
        authState = AuthState.FINISHED
        Log.i(TAG, "[Session] unlockAndFinish pkg=$packageName userId=$userId")
        notifySessionUnlocked()
        setResult(Activity.RESULT_OK, buildResultData())
        finish()
    }

    private fun cancelAndFinish() {
        if (authState == AuthState.FINISHED || isFinishing) return
        authState = AuthState.FINISHED
        Log.i(TAG, "[Session] cancelAndFinish pkg=$packageName userId=$userId")
        setResult(Activity.RESULT_CANCELED, buildResultData())
        finish()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, lifecycleTag("onPause"))
        if (authState == AuthState.PROMPT_SHOWING) {
            Log.d(TAG, lifecycleTag("onPause") + " skipping - bio prompt active")
            return
        }
        // System unlock runs in the target app task; runtime permission dialogs pause
        // this activity without the user leaving — do not cancel the unlock flow.
        if (isSystemUnlock) {
            return
        }
        biometricCancellationSignal?.cancel()
        biometricCancellationSignal = null
        if (authState != AuthState.FINISHED) {
            authState = AuthState.FINISHED
            setResult(Activity.RESULT_CANCELED, buildResultData())
            finish()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        Log.d(TAG, lifecycleTag("onUserLeaveHint"))
        if (!isSystemUnlock && authState == AuthState.IDLE) {
            cancelAndFinish()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, lifecycleTag("onDestroy"))
        biometricCancellationSignal?.cancel()
        biometricCancellationSignal = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AppLock.Auth"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_LABEL = "app_label"
        const val EXTRA_USER_ID = "user_id"

        const val EXTRA_LOCKED_PACKAGE = "LOCKED_PACKAGE"
        const val EXTRA_LOCKED_UID = "LOCKED_UID"

        const val ACTION_AUTHENTICATE = "com.android.applock.action.AUTHENTICATE"
        const val ACTION_SYSTEM_UNLOCK = "com.android.applock.action.SYSTEM_UNLOCK"

        private const val EXIT_ANIMATION_MS = 300L
        private const val UI_DEFER_MS = 400L
    }
}

@Composable
fun AuthenticateScreen(
    securityManager: AppLockSecurityManager,
    securityType: SecurityType,
    appLabel: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    biometricType: AppLockSecurityManager.BiometricType = AppLockSecurityManager.BiometricType.NONE,
    onBiometricClick: () -> Unit = {},
    isPreferBiometric: Boolean = false,
    isExiting: Boolean = false
) {
    var didAutoTriggerBio by remember { mutableStateOf(false) }
    LaunchedEffect(biometricType, isPreferBiometric) {
        if (!didAutoTriggerBio
            && biometricType != AppLockSecurityManager.BiometricType.NONE
            && isPreferBiometric) {
            didAutoTriggerBio = true
            onBiometricClick()
        }
    }

    val promptText = "Enter your privacy password to unlock $appLabel"
    
    when (securityType) {
        SecurityType.PIN -> {
            LockScreen(
                isSetup = false,
                promptText = promptText,
                onUnlock = onSuccess,
                onPinEntered = { pin -> securityManager.verifyCredential(pin) },
                onBack = onCancel,
                biometricType = biometricType,
                onBiometricClick = onBiometricClick,
                isExiting = isExiting
            )
        }
        SecurityType.PASSWORD -> {
            PasswordScreen(
                isSetup = false,
                promptText = promptText,
                onUnlock = onSuccess,
                onPasswordEntered = { password -> securityManager.verifyCredential(password) },
                onBack = onCancel,
                biometricType = biometricType,
                onBiometricClick = onBiometricClick,
                isExiting = isExiting
            )
        }
        SecurityType.PATTERN -> {
            PatternScreen(
                isSetup = false,
                promptText = promptText,
                onUnlock = onSuccess,
                onPatternEntered = { pattern -> securityManager.verifyPattern(pattern) },
                onBack = onCancel,
                biometricType = biometricType,
                onBiometricClick = onBiometricClick,
                isExiting = isExiting
            )
        }
        SecurityType.NONE -> {
            LaunchedEffect(Unit) {
                onSuccess()
            }
        }
    }
}
