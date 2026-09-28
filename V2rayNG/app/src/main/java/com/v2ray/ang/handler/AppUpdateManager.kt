package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.annotations.SerializedName
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.dto.GitHubRelease
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.extension.concatUrl
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class CustomUpdateManifest(
    @SerializedName("versionCode") val versionCode: Long = 0L,
    @SerializedName("versionName") val versionName: String = "",
    @SerializedName("downloadUrl") val downloadUrl: String = "",
    @SerializedName("changelog") val changelog: String? = null,
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false
)

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String? = null,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null,
    val isPreRelease: Boolean = false,
    val isForceUpdate: Boolean = false,
    val apkSize: Long = 0L
)

sealed interface UpdateDownloadProgress {
    object Idle : UpdateDownloadProgress
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long, val percent: Int) : UpdateDownloadProgress
    data class Completed(val apkFile: File) : UpdateDownloadProgress
    data class Failed(val error: String) : UpdateDownloadProgress
}

object AppUpdateManager {

    private val _downloadProgress = MutableStateFlow<UpdateDownloadProgress>(UpdateDownloadProgress.Idle)
    val downloadProgress: StateFlow<UpdateDownloadProgress> = _downloadProgress.asStateFlow()

    fun resetDownloadProgress() {
        _downloadProgress.value = UpdateDownloadProgress.Idle
    }

    suspend fun checkForUpdate(
        customEndpoint: String? = null,
        includePreRelease: Boolean = false
    ): AppUpdateInfo = withContext(Dispatchers.IO) {
        val configuredUrl = customEndpoint?.trim()?.ifEmpty { null }
            ?: MmkvManager.decodeSettingsString(AppConfig.PREF_CUSTOM_UPDATE_URL, "")?.trim()?.ifEmpty { null }

        if (!configuredUrl.isNullOrEmpty()) {
            return@withContext checkCustomUrl(configuredUrl)
        }

        // Fallback to default GitHub repository releases
        return@withContext checkGitHubUpdate(includePreRelease)
    }

    private fun checkCustomUrl(url: String): AppUpdateInfo {
        try {
            val response = HttpUtil.getUrlContent(UrlContentRequest(url = url, timeout = 7000))
                ?: throw IllegalStateException("Empty response from custom update URL")

            // First attempt to parse as custom manifest
            val customManifest = JsonUtil.fromJsonSafe(response, CustomUpdateManifest::class.java)
            if (customManifest != null && customManifest.versionName.isNotEmpty()) {
                val hasUpdate = compareVersions(customManifest.versionName, BuildConfig.VERSION_NAME) > 0 ||
                        (customManifest.versionCode > 0 && customManifest.versionCode > BuildConfig.VERSION_CODE)
                return AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = customManifest.versionName,
                    releaseNotes = customManifest.changelog,
                    downloadUrl = customManifest.downloadUrl,
                    isForceUpdate = customManifest.forceUpdate
                )
            }

            // Fallback: Attempt to parse as GitHub release
            val gitHubRelease = JsonUtil.fromJsonSafe(response, GitHubRelease::class.java)
            if (gitHubRelease != null) {
                val version = gitHubRelease.tagName.removePrefix("v").trim()
                val hasUpdate = compareVersions(version, BuildConfig.VERSION_NAME) > 0
                val downloadUrl = resolveDownloadUrl(gitHubRelease, Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
                return AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = version,
                    releaseNotes = gitHubRelease.body,
                    downloadUrl = downloadUrl,
                    isPreRelease = gitHubRelease.prerelease
                )
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to check custom update endpoint: $url", e)
        }
        return AppUpdateInfo(hasUpdate = false)
    }

    private fun checkGitHubUpdate(includePreRelease: Boolean): AppUpdateInfo {
        val url = if (includePreRelease) {
            AppConfig.APP_API_URL
        } else {
            AppConfig.APP_API_URL.concatUrl("latest")
        }

        var response = HttpUtil.getUrlContent(UrlContentRequest(url = url, timeout = 7000))
        if (response.isNullOrEmpty()) {
            val httpPort = SettingsManager.getHttpPort()
            response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 7000,
                    httpPort = httpPort,
                    proxyUsername = SettingsManager.getSocksUsername(),
                    proxyPassword = SettingsManager.getSocksPassword()
                )
            )
        }
        if (response.isNullOrEmpty()) {
            return AppUpdateInfo(hasUpdate = false)
        }

        val latestRelease = if (includePreRelease) {
            JsonUtil.fromJsonSafe(response, Array<GitHubRelease>::class.java)?.firstOrNull()
        } else {
            JsonUtil.fromJsonSafe(response, GitHubRelease::class.java)
        } ?: return AppUpdateInfo(hasUpdate = false)

        val latestVersion = latestRelease.tagName.removePrefix("v").trim()
        val hasUpdate = compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0

        val downloadUrl = if (hasUpdate) {
            try {
                resolveDownloadUrl(latestRelease, Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
            } catch (e: Exception) {
                null
            }
        } else null

        return AppUpdateInfo(
            hasUpdate = hasUpdate,
            latestVersion = latestVersion,
            releaseNotes = latestRelease.body,
            downloadUrl = downloadUrl,
            isPreRelease = latestRelease.prerelease
        )
    }

    suspend fun downloadApk(context: Context, downloadUrl: String, versionName: String): File = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, "updates")
        if (!updateDir.exists()) updateDir.mkdirs()

        val apkFile = File(updateDir, "K-Client-$versionName.apk")
        if (apkFile.exists()) apkFile.delete()

        _downloadProgress.value = UpdateDownloadProgress.Downloading(0L, 0L, 0)

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val err = "Download failed: HTTP ${response.code}"
            _downloadProgress.value = UpdateDownloadProgress.Failed(err)
            throw IllegalStateException(err)
        }

        val body = response.body ?: throw IllegalStateException("Empty download body")
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(apkFile)

        try {
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val percent = if (contentLength > 0) {
                    ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                } else 0

                _downloadProgress.value = UpdateDownloadProgress.Downloading(
                    bytesDownloaded = totalRead,
                    totalBytes = contentLength,
                    percent = percent
                )
            }
            outputStream.flush()
            _downloadProgress.value = UpdateDownloadProgress.Completed(apkFile)
            return@withContext apkFile
        } catch (e: Exception) {
            _downloadProgress.value = UpdateDownloadProgress.Failed(e.message ?: "Download failed")
            throw e
        } finally {
            try { outputStream.close() } catch (_: Exception) {}
            try { inputStream.close() } catch (_: Exception) {}
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            LogUtil.e(AppConfig.TAG, "Cannot install: APK file does not exist: ${apkFile.absolutePath}")
            return
        }

        val authority = "${context.packageName}.cache"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun compareVersions(version1: String, version2: String): Int {
        val v1 = version1.split(".").mapNotNull { it.trim().toIntOrNull() }
        val v2 = version2.split(".").mapNotNull { it.trim().toIntOrNull() }

        for (i in 0 until maxOf(v1.size, v2.size)) {
            val num1 = if (i < v1.size) v1[i] else 0
            val num2 = if (i < v2.size) v2[i] else 0
            if (num1 != num2) return num1 - num2
        }
        return 0
    }

    private fun resolveDownloadUrl(release: GitHubRelease, abi: String): String {
        val assetsByAbi = release.assets.filter { it.name.contains(abi, true) }
        val asset = assetsByAbi.firstOrNull { !it.name.contains("fdroid", true) }
            ?: release.assets.firstOrNull()
        return asset?.browserDownloadUrl
            ?: throw IllegalStateException("No compatible APK found in release assets")
    }
}
