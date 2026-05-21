/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.applock.auth.security

import android.app.AppLockManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.os.RemoteException
import android.provider.Settings
import java.security.MessageDigest

enum class SecurityType {
    NONE,
    PIN,
    PASSWORD,
    PATTERN,
}

/** Privacy password storage for App Lock ({@code applock_*} Secure keys). */
class AppLockSecurityManager(private val context: Context) {

    private val appLockManager: AppLockManager? by lazy {
        context.getSystemService(AppLockManager::class.java)
    }

    fun getSecurityType(): SecurityType {
        val type = readSecurityType() ?: return SecurityType.NONE
        return try {
            SecurityType.valueOf(type)
        } catch (_: IllegalArgumentException) {
            SecurityType.NONE
        }
    }

    fun isSetup(): Boolean {
        val type = readSecurityType() ?: return false
        val hash = readCredentialHash()
        return type != "NONE" && !hash.isNullOrEmpty()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < MIN_PIN_LENGTH || pin.length > MAX_PIN_LENGTH || !pin.all { it.isDigit() }) {
            return false
        }
        val hash = hashCredential(pin)
        return putSecure(KEY_SECURITY_TYPE, SecurityType.PIN.name)
            && putSecure(KEY_CREDENTIAL_HASH, hash)
    }

    fun setPassword(password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH) return false
        val hash = hashCredential(password)
        return putSecure(KEY_SECURITY_TYPE, SecurityType.PASSWORD.name)
            && putSecure(KEY_CREDENTIAL_HASH, hash)
    }

    fun setPattern(pattern: List<Int>): Boolean {
        if (pattern.size < MIN_PATTERN_LENGTH) return false
        val hash = hashCredential(pattern.joinToString(","))
        return putSecure(KEY_SECURITY_TYPE, SecurityType.PATTERN.name)
            && putSecure(KEY_CREDENTIAL_HASH, hash)
    }

    fun verifyCredential(credential: String): Boolean {
        val storedHash = readCredentialHash() ?: return false
        return storedHash == hashCredential(credential)
    }

    fun verifyPattern(pattern: List<Int>): Boolean =
        verifyCredential(pattern.joinToString(","))

    enum class BiometricType {
        NONE,
        FINGERPRINT,
        FACE,
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = context.getSystemService(BiometricManager::class.java)
        return biometricManager?.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                or BiometricManager.Authenticators.BIOMETRIC_WEAK,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricType(): BiometricType {
        val pm = context.packageManager
        if (pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)) {
            return BiometricType.FINGERPRINT
        }
        if (pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FACE)) {
            return BiometricType.FACE
        }
        return BiometricType.NONE
    }

    fun isBiometricEnabled(): Boolean =
        readInt(KEY_BIOMETRIC_ENABLED, LEGACY_KEY_BIOMETRIC_ENABLED) == 1

    fun setBiometricEnabled(enabled: Boolean) {
        putInt(KEY_BIOMETRIC_ENABLED, if (enabled) 1 else 0)
        if (!enabled) setPreferBiometric(false)
    }

    fun isPreferBiometric(): Boolean =
        readInt(KEY_PREFER_BIOMETRIC, LEGACY_KEY_PREFER_BIOMETRIC) == 1

    fun setPreferBiometric(preferred: Boolean) {
        putInt(KEY_PREFER_BIOMETRIC, if (preferred) 1 else 0)
    }

    private fun readSecurityType(): String? =
        Settings.Secure.getString(context.contentResolver, KEY_SECURITY_TYPE)
            ?: Settings.Secure.getString(context.contentResolver, LEGACY_KEY_SECURITY_TYPE)

    private fun readCredentialHash(): String? =
        Settings.Secure.getString(context.contentResolver, KEY_CREDENTIAL_HASH)
            ?: Settings.Secure.getString(context.contentResolver, LEGACY_KEY_CREDENTIAL_HASH)

    private fun readInt(key: String, legacyKey: String): Int {
        val resolver = context.contentResolver
        if (Settings.Secure.getString(resolver, key) != null) {
            return Settings.Secure.getInt(resolver, key, 0)
        }
        return Settings.Secure.getInt(resolver, legacyKey, 0)
    }

    private fun putSecure(key: String, value: String): Boolean {
        val mgr = appLockManager ?: return false
        return try {
            mgr.putSecureString(key, value)
        } catch (_: RemoteException) {
            false
        }
    }

    private fun putInt(key: String, value: Int) {
        val mgr = appLockManager ?: return
        try {
            mgr.putSecureInt(key, value)
        } catch (_: RemoteException) {
            // ignore
        }
    }

    private fun hashCredential(credential: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(credential.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val KEY_SECURITY_TYPE = AppLockManager.SETTING_SECURITY_TYPE
        const val KEY_CREDENTIAL_HASH = AppLockManager.SETTING_CREDENTIAL_HASH
        const val KEY_BIOMETRIC_ENABLED = AppLockManager.SETTING_BIOMETRIC_ENABLED
        const val KEY_PREFER_BIOMETRIC = AppLockManager.SETTING_PREFER_BIOMETRIC

        /** Legacy AppLocker / Sandbox keys — read-only fallback for upgrades. */
        private const val LEGACY_KEY_SECURITY_TYPE = "sandbox_security_type"
        private const val LEGACY_KEY_CREDENTIAL_HASH = "sandbox_credential_hash"
        private const val LEGACY_KEY_BIOMETRIC_ENABLED = "sandbox_biometric_enabled"
        private const val LEGACY_KEY_PREFER_BIOMETRIC = "sandbox_prefer_biometric"

        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 4
        const val MIN_PASSWORD_LENGTH = 4
        const val MIN_PATTERN_LENGTH = 4
    }
}
