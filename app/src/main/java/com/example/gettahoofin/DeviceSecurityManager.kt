package com.example.gettahoofin

import android.content.Context
import android.os.Build
import java.security.MessageDigest
import java.util.UUID
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Handles device identification and security verification using a privacy-friendly approach
 */
class DeviceSecurityManager(private val context: Context) {

    companion object {
        private const val SECURITY_PREFS = "device_security_prefs"
        private const val DEVICE_ID_KEY = "device_id"
        private const val APP_INSTALL_ID_KEY = "app_install_id"
        private const val TAG = "DeviceSecurityManager"
    }

    private val securityPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE)
    }

    /**
     * Verifies if the app is running on the authorized device
     * @return true if the device is authorized, false otherwise
     */
    fun isAuthorizedDevice(): Boolean {
        val storedDeviceId = getStoredDeviceId()

        // If no device ID is stored yet, this is first run - authorize and store the ID
        if (storedDeviceId.isNullOrEmpty()) {
            // First, make sure app install ID exists (generate if needed)
            ensureAppInstallIdExists()

            // Now generate the device ID (which will include the app install ID)
            val currentDeviceId = generateDeviceId()
            storeDeviceId(currentDeviceId)

            AppLogger.d(TAG, "First run - device authorized and IDs stored")
            return true
        }

        // Compare stored ID with current device ID
        val currentDeviceId = generateDeviceId()
        val isAuthorized = currentDeviceId == storedDeviceId

        // Add detailed logging (only in debug mode now)
        AppLogger.d(TAG, "Stored device ID: $storedDeviceId")
        AppLogger.d(TAG, "Current device ID: $currentDeviceId")
        AppLogger.d(TAG, "Devices match: $isAuthorized")

        if (!isAuthorized) {
            AppLogger.w(TAG, "Unauthorized device detected")
        }

        return isAuthorized
    }

    /**
     * Ensures the app installation ID exists, generating it if needed
     */
    private fun ensureAppInstallIdExists() {
        if (getAppInstallationId().isNullOrEmpty()) {
            generateAndStoreAppInstallId()
            AppLogger.d(TAG, "Generated new app installation ID")
        }
    }

    /**
     * Generates a unique device identifier using multiple device properties
     * The identifier is hashed to ensure privacy and security
     */
    private fun generateDeviceId(): String {
        val deviceIdBuilder = StringBuilder()

        // Log device components only in debug mode
        AppLogger.d(TAG, "Device ID components:")
        AppLogger.d(TAG, "BOARD: ${Build.BOARD}")
        AppLogger.d(TAG, "BRAND: ${Build.BRAND}")
        AppLogger.d(TAG, "DEVICE: ${Build.DEVICE}")
        AppLogger.d(TAG, "HARDWARE: ${Build.HARDWARE}")
        AppLogger.d(TAG, "MANUFACTURER: ${Build.MANUFACTURER}")
        AppLogger.d(TAG, "MODEL: ${Build.MODEL}")
        AppLogger.d(TAG, "PRODUCT: ${Build.PRODUCT}")
        AppLogger.d(TAG, "FINGERPRINT: ${Build.FINGERPRINT}")

        // Use Build properties which are consistent across device reboots
        deviceIdBuilder.append(Build.BOARD)
        deviceIdBuilder.append(Build.BRAND)
        deviceIdBuilder.append(Build.DEVICE)
        deviceIdBuilder.append(Build.HARDWARE)
        deviceIdBuilder.append(Build.MANUFACTURER)
        deviceIdBuilder.append(Build.MODEL)
        deviceIdBuilder.append(Build.PRODUCT)

        // Add build fingerprint which combines many device characteristics
        deviceIdBuilder.append(Build.FINGERPRINT)

        // Add the app installation UUID if it exists
        val appInstallId = getAppInstallationId()
        if (!appInstallId.isNullOrEmpty()) {
            AppLogger.d(TAG, "Including APP_INSTALL_ID in device ID: $appInstallId")
            deviceIdBuilder.append(appInstallId)
        } else {
            AppLogger.d(TAG, "APP_INSTALL_ID is null or empty, not including in device ID")
        }

        // Hash the combined value
        val result = hashString(deviceIdBuilder.toString())
        AppLogger.d(TAG, "Generated device ID: $result")
        return result
    }

    /**
     * Generates and stores a unique ID for this app installation
     * This ID persists until the app is uninstalled
     */
    private fun generateAndStoreAppInstallId() {
        val uuid = UUID.randomUUID().toString()
        securityPrefs.edit {
            putString(APP_INSTALL_ID_KEY, uuid)
        }
        AppLogger.d(TAG, "Generated and stored new APP_INSTALL_ID: $uuid")
    }

    /**
     * Gets the stored app installation ID
     */
    private fun getAppInstallationId(): String? {
        val id = securityPrefs.getString(APP_INSTALL_ID_KEY, null)
        AppLogger.d(TAG, "Retrieved APP_INSTALL_ID: $id")
        return id
    }

    /**
     * Creates a SHA-256 hash of the input string
     */
    private fun hashString(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))

            // Convert bytes to hex string
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error creating hash", e)

            // Fallback to a UUID if hashing fails
            UUID.randomUUID().toString()
        }
    }

    /**
     * Stores the device ID in SharedPreferences
     */
    private fun storeDeviceId(deviceId: String) {
        securityPrefs.edit {
            putString(DEVICE_ID_KEY, deviceId)
        }
        AppLogger.d(TAG, "Stored device ID: $deviceId")
    }

    /**
     * Retrieves the stored device ID from SharedPreferences
     */
    private fun getStoredDeviceId(): String? {
        val id = securityPrefs.getString(DEVICE_ID_KEY, null)
        AppLogger.d(TAG, "Retrieved stored device ID: $id")
        return id
    }

    /**
     * Resets the stored device ID (for testing or admin purposes)
     */
    fun resetDeviceAuthorization() {
        securityPrefs.edit {
            remove(DEVICE_ID_KEY)
            remove(APP_INSTALL_ID_KEY)
        }
        AppLogger.d(TAG, "Device authorization reset")
    }
}