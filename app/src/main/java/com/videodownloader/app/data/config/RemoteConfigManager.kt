package com.videodownloader.app.data.config

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class RemoteScraperConfig(
    val tiktokApiEndpoints: List<String> = listOf(
        "https://www.tikwm.com/api/",
        "https://snaptik.vn/api/tiktok",
        "https://d.zcdn.top/api/tiktok"
    ),
    val facebookApiEndpoints: List<String> = listOf(
        "https://fsave.net/proxy.php",
        "https://snapsave.app/action.php"
    ),
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    val timeoutSeconds: Long = 15L
)

data class AppUpdateInfo(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false
)

data class RemoteConfigResponse(
    val scraperConfig: RemoteScraperConfig = RemoteScraperConfig(),
    val updateInfo: AppUpdateInfo = AppUpdateInfo()
)

object RemoteConfigManager {
    // Remote JSON endpoint hosted on GitHub Raw / Gist / Vercel
    private const val DEFAULT_REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/videodownloader/config/main/remote_config.json"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    
    private var currentConfig: RemoteConfigResponse = RemoteConfigResponse()

    fun getConfig(): RemoteConfigResponse = currentConfig

    suspend fun fetchRemoteConfig(customUrl: String? = null): RemoteConfigResponse = withContext(Dispatchers.IO) {
        val url = customUrl ?: DEFAULT_REMOTE_CONFIG_URL
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "VideoDownloader-AndroidApp")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful && response.body != null) {
                    val json = response.body!!.string()
                    val parsed = gson.fromJson(json, RemoteConfigResponse::class.java)
                    if (parsed != null) {
                        currentConfig = parsed
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext currentConfig
    }
}
