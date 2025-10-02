package com.meneses.budgethunter.commons.platform

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Serializable
private data class AppStoreResponse(
    val resultCount: Int,
    val results: List<AppStoreResult>
)

@Serializable
private data class AppStoreResult(
    val version: String,
    val trackId: Long,
    val bundleId: String,
    val releaseNotes: String? = null
)

actual class AppUpdateManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val httpClient = HttpClient(Darwin)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    actual fun checkForUpdates(onResult: (AppUpdateResult) -> Unit) {
        scope.launch {
            try {
                val bundleId = getBundleId()
                val currentVersion = getCurrentVersion()

                if (bundleId == null || currentVersion == null) {
                    println("AppUpdateManager: Unable to get bundle ID or current version")
                    onResult(AppUpdateResult.UpdateFailed)
                    return@launch
                }

                checkAppStoreVersion(bundleId, currentVersion, onResult)
            } catch (e: Exception) {
                println("AppUpdateManager: Error checking for updates: ${e.message}")
                onResult(AppUpdateResult.UpdateFailed)
            }
        }
    }

    private fun getBundleId(): String? {
        return NSBundle.mainBundle.bundleIdentifier
    }

    private fun getCurrentVersion(): String? {
        return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    }

    private suspend fun checkAppStoreVersion(
        bundleId: String,
        currentVersion: String,
        onResult: (AppUpdateResult) -> Unit
    ) {
        try {
            println("AppUpdateManager: Checking for updates...")
            println("AppUpdateManager: Bundle ID: $bundleId")
            println("AppUpdateManager: Current version: $currentVersion")

            val iTunesUrl = "https://itunes.apple.com/lookup?bundleId=$bundleId"
            println("AppUpdateManager: Requesting: $iTunesUrl")

            val responseText = httpClient.get(iTunesUrl).bodyAsText()
            val response = json.decodeFromString<AppStoreResponse>(responseText)

            if (response.resultCount > 0) {
                val appStoreResult = response.results.first()
                val latestVersion = appStoreResult.version
                val trackId = appStoreResult.trackId

                println("AppUpdateManager: Latest version from App Store: $latestVersion")

                if (isNewerVersion(currentVersion, latestVersion)) {
                    println("AppUpdateManager: Update available - $currentVersion -> $latestVersion")
                    onResult(AppUpdateResult.UpdateAvailable {
                        openAppStore(trackId)
                    })
                } else {
                    println("AppUpdateManager: No update available - current version is up to date")
                    onResult(AppUpdateResult.NoUpdateAvailable)
                }
            } else {
                println("AppUpdateManager: App not found in App Store")
                onResult(AppUpdateResult.NoUpdateAvailable)
            }
        } catch (e: Exception) {
            println("AppUpdateManager: Error checking App Store version: ${e.message}")
            onResult(AppUpdateResult.UpdateFailed)
        }
    }

    /**
     * Compares two version strings to determine if the latest version is newer.
     * Supports semantic versioning (e.g., "1.2.3").
     */
    private fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        return try {
            val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(currentParts.size, latestParts.size)

            for (i in 0 until maxLength) {
                val current = currentParts.getOrNull(i) ?: 0
                val latest = latestParts.getOrNull(i) ?: 0

                when {
                    latest > current -> return true
                    latest < current -> return false
                    // Continue to next part if equal
                }
            }

            false // Versions are equal
        } catch (e: Exception) {
            println("AppUpdateManager: Error comparing versions: ${e.message}")
            false
        }
    }

    /**
     * Opens the App Store page for the app using the provided App Store ID.
     * This uses UIApplication.openURL to launch the App Store app.
     */
    private fun openAppStore(appId: Long) {
        try {
            val appStoreURL = "https://apps.apple.com/app/id$appId"
            val url = NSURL.URLWithString(appStoreURL)

            if (url != null) {
                val application = UIApplication.sharedApplication
                if (application.canOpenURL(url)) {
                    application.openURL(url)
                    println("AppUpdateManager: Opened App Store URL: $appStoreURL")
                } else {
                    println("AppUpdateManager: Cannot open App Store URL: $appStoreURL")
                }
            } else {
                println("AppUpdateManager: Invalid App Store URL: $appStoreURL")
            }
        } catch (e: Exception) {
            println("AppUpdateManager: Error opening App Store: ${e.message}")
        }
    }
}