package com.android.applock.auth

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.applock.auth.security.AppLockSecurityManager
import com.android.applock.auth.security.SecurityType
import com.android.applock.auth.ui.BiometricSetupScreen
import com.android.applock.auth.ui.LockScreen
import com.android.applock.auth.ui.PasswordScreen
import com.android.applock.auth.ui.PatternScreen
import com.android.applock.auth.ui.SecuritySetupScreen
import com.android.applock.auth.ui.theme.AppLockAuthTheme

/** Set or change the privacy password used by App Lock and Sandbox auth. */
class SetupCredentialsActivity : ComponentActivity() {

    private lateinit var securityManager: AppLockSecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        securityManager = AppLockSecurityManager(this)

        val startScreen = if (securityManager.isSetup()) {
            SetupScreen.VERIFY_CURRENT
        } else {
            SetupScreen.TYPE_SELECTOR
        }

        setContent {
            AppLockAuthTheme {
                var screen by rememberSaveable { mutableStateOf(startScreen) }
                var selectedType by rememberSaveable { mutableStateOf(securityManager.getSecurityType()) }
                var firstCredential by rememberSaveable { mutableStateOf<Any?>(null) }
                var isConfirmStep by rememberSaveable { mutableStateOf(false) }
                var biometricEnabled by rememberSaveable {
                    mutableStateOf(securityManager.isBiometricEnabled())
                }

                fun finishSuccess() {
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                fun goToBiometricOrDone() {
                    if (securityManager.isBiometricAvailable()) {
                        biometricEnabled = securityManager.isBiometricEnabled()
                        screen = SetupScreen.BIOMETRIC
                    } else {
                        finishSuccess()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (screen) {
                        SetupScreen.VERIFY_CURRENT -> {
                            val type = securityManager.getSecurityType()
                            VerifyCurrentCredential(
                                securityManager = securityManager,
                                securityType = type,
                                onVerified = {
                                    selectedType = type
                                    firstCredential = null
                                    isConfirmStep = false
                                    screen = SetupScreen.TYPE_SELECTOR
                                },
                                onCancel = { finish() },
                            )
                        }

                        SetupScreen.TYPE_SELECTOR -> {
                            BackHandler { finish() }
                            SecuritySetupScreen(
                                onSecurityTypeSelected = { type ->
                                    selectedType = type
                                    firstCredential = null
                                    isConfirmStep = false
                                    screen = SetupScreen.SETUP_CREDENTIAL
                                },
                            )
                        }

                        SetupScreen.SETUP_CREDENTIAL -> {
                            val handleBack = {
                                if (isConfirmStep) {
                                    isConfirmStep = false
                                    firstCredential = null
                                } else {
                                    screen = SetupScreen.TYPE_SELECTOR
                                }
                            }
                            BackHandler { handleBack() }

                            val setupPrompt = getString(
                                if (isConfirmStep) {
                                    R.string.setup_confirm_credential
                                } else {
                                    R.string.setup_enter_credential
                                },
                            )

                            when (selectedType) {
                                SecurityType.PIN -> LockScreen(
                                    isSetup = true,
                                    promptText = setupPrompt,
                                    onUnlock = {
                                        if (!isConfirmStep) {
                                            isConfirmStep = true
                                        } else {
                                            securityManager.setPin(firstCredential as String)
                                            goToBiometricOrDone()
                                        }
                                    },
                                    onPinEntered = { pin ->
                                        if (!isConfirmStep) {
                                            firstCredential = pin
                                            true
                                        } else {
                                            pin == (firstCredential as? String)
                                        }
                                    },
                                    confirmPin = if (isConfirmStep) {
                                        firstCredential as? String
                                    } else {
                                        null
                                    },
                                    onBack = handleBack,
                                )

                                SecurityType.PASSWORD -> PasswordScreen(
                                    isSetup = true,
                                    promptText = setupPrompt,
                                    onUnlock = {
                                        if (!isConfirmStep) {
                                            isConfirmStep = true
                                        } else {
                                            securityManager.setPassword(firstCredential as String)
                                            goToBiometricOrDone()
                                        }
                                    },
                                    onPasswordEntered = { password ->
                                        if (!isConfirmStep) {
                                            firstCredential = password
                                            true
                                        } else {
                                            password == (firstCredential as? String)
                                        }
                                    },
                                    confirmPassword = if (isConfirmStep) {
                                        firstCredential as? String
                                    } else {
                                        null
                                    },
                                    onBack = handleBack,
                                )

                                SecurityType.PATTERN -> PatternScreen(
                                    isSetup = true,
                                    promptText = setupPrompt,
                                    onUnlock = {
                                        if (!isConfirmStep) {
                                            isConfirmStep = true
                                        } else {
                                            @Suppress("UNCHECKED_CAST")
                                            securityManager.setPattern(firstCredential as List<Int>)
                                            goToBiometricOrDone()
                                        }
                                    },
                                    onPatternEntered = { pattern ->
                                        if (!isConfirmStep) {
                                            firstCredential = pattern
                                            true
                                        } else {
                                            pattern == (firstCredential as? List<*>)
                                        }
                                    },
                                    confirmPattern = if (isConfirmStep) {
                                        @Suppress("UNCHECKED_CAST")
                                        firstCredential as? List<Int>
                                    } else {
                                        null
                                    },
                                    onBack = handleBack,
                                )

                                SecurityType.NONE -> finish()
                            }
                        }

                        SetupScreen.BIOMETRIC -> {
                            BackHandler { finishSuccess() }
                            val label = when (securityManager.getBiometricType()) {
                                AppLockSecurityManager.BiometricType.FINGERPRINT ->
                                    getString(R.string.setup_biometric_fingerprint)
                                AppLockSecurityManager.BiometricType.FACE ->
                                    getString(R.string.setup_biometric_face)
                                else -> getString(R.string.setup_biometric_generic)
                            }
                            BiometricSetupScreen(
                                biometricLabel = label,
                                enabled = biometricEnabled,
                                onEnabledChange = { enabled ->
                                    biometricEnabled = enabled
                                    securityManager.setBiometricEnabled(enabled)
                                    if (enabled) {
                                        securityManager.setPreferBiometric(true)
                                    }
                                },
                                onDone = { finishSuccess() },
                            )
                        }
                    }
                }
            }
        }
    }

    private enum class SetupScreen {
        VERIFY_CURRENT,
        TYPE_SELECTOR,
        SETUP_CREDENTIAL,
        BIOMETRIC,
    }

    companion object {
        const val ACTION_SETUP_CREDENTIALS =
            "com.android.applock.action.SETUP_CREDENTIALS"
    }
}

@androidx.compose.runtime.Composable
private fun VerifyCurrentCredential(
    securityManager: AppLockSecurityManager,
    securityType: SecurityType,
    onVerified: () -> Unit,
    onCancel: () -> Unit,
) {
    val prompt = "Enter your current privacy password"
    when (securityType) {
        SecurityType.PIN -> LockScreen(
            isSetup = false,
            promptText = prompt,
            onUnlock = onVerified,
            onPinEntered = { securityManager.verifyCredential(it) },
            onBack = onCancel,
        )

        SecurityType.PASSWORD -> PasswordScreen(
            isSetup = false,
            promptText = prompt,
            onUnlock = onVerified,
            onPasswordEntered = { securityManager.verifyCredential(it) },
            onBack = onCancel,
        )

        SecurityType.PATTERN -> PatternScreen(
            isSetup = false,
            promptText = prompt,
            onUnlock = onVerified,
            onPatternEntered = { securityManager.verifyPattern(it) },
            onBack = onCancel,
        )

        SecurityType.NONE -> androidx.compose.runtime.LaunchedEffect(Unit) { onVerified() }
    }
}
