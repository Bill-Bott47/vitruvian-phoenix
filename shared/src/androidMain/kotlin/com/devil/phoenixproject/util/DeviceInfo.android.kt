package com.devil.phoenixproject.util

import android.os.Build

/**
 * Android implementation of DeviceInfo.
 * Uses android.os.Build for device information.
 *
 * Note: App version info is from shared Constants.
 * For full BuildConfig integration, configure in androidApp module.
 */
actual object DeviceInfo {

    // ==================== App Build Info ====================

    actual val appVersionName: String = Constants.APP_VERSION

    actual val appVersionCode: Int = 1  // TODO: Integrate with BuildConfig

    actual val isDebugBuild: Boolean = false  // TODO: Integrate with BuildConfig.DEBUG

    actual val buildType: String = if (isDebugBuild) "debug" else "release"

    // ==================== Android Device Info ====================

    actual val manufacturer: String = Build.MANUFACTURER

    actual val model: String = Build.MODEL

    actual val osVersion: String = Build.VERSION.RELEASE

    private val sdkInt: Int = Build.VERSION.SDK_INT

    actual val platformVersionFull: String = "Android $osVersion (SDK $sdkInt)"

    private val device: String = Build.DEVICE

    private val fingerprint: String = Build.FINGERPRINT

    // ==================== Formatted Output ====================

    actual fun getFormattedInfo(): String {
        return buildString {
            appendLine("App: VitruvianPhoenix v$appVersionName (build $appVersionCode)")
            appendLine("Build Type: $buildType")
            appendLine()
            appendLine("Device: $manufacturer $model")
            appendLine("Model Name: $device")
            appendLine("OS: $platformVersionFull")
            appendLine("Build: ${Build.DISPLAY}")
        }
    }

    actual fun getCompactInfo(): String {
        return "$manufacturer $model (Android $osVersion, SDK $sdkInt)"
    }

    actual fun getAppVersionInfo(): String {
        return "v$appVersionName ($buildType)"
    }

    actual fun toJson(): String {
        return buildString {
            append("{")
            append("\"appVersion\":\"$appVersionName\",")
            append("\"appVersionCode\":$appVersionCode,")
            append("\"buildType\":\"$buildType\",")
            append("\"manufacturer\":\"$manufacturer\",")
            append("\"model\":\"$model\",")
            append("\"device\":\"$device\",")
            append("\"osVersion\":\"$osVersion\",")
            append("\"sdkInt\":$sdkInt,")
            append("\"fingerprint\":\"$fingerprint\"")
            append("}")
        }
    }

    // ==================== Android-Specific Helpers ====================

    /**
     * Check if running on Android 12 or higher (new BLE permissions)
     */
    fun isAndroid12OrHigher(): Boolean = sdkInt >= Build.VERSION_CODES.S

    /**
     * Check if running on Samsung device
     */
    fun isSamsung(): Boolean = manufacturer.equals("samsung", ignoreCase = true)

    /**
     * Check if running on Google Pixel
     */
    fun isPixel(): Boolean = manufacturer.equals("Google", ignoreCase = true)
}
