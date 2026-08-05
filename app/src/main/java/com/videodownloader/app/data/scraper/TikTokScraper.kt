package com.videodownloader.app.data.scraper

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.videodownloader.app.data.config.RemoteConfigManager
import com.videodownloader.app.data.model.DownloadOption
import com.videodownloader.app.data.model.Platform
import com.videodownloader.app.data.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object TikTokScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun parseTikTokUrl(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = extractUrl(url)
            if (cleanUrl.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("URL TikTok không hợp lệ"))
            }

            val config = RemoteConfigManager.getConfig().scraperConfig
            val endpoints = config.tiktokApiEndpoints.ifEmpty {
                listOf("https://www.tikwm.com/api/")
            }

            for (endpoint in endpoints) {
                val videoInfo = fetchFromEndpoint(endpoint, cleanUrl)
                if (videoInfo != null) {
                    return@withContext Result.success(videoInfo)
                }
            }

            Result.failure(Exception("Không thể lấy thông tin video TikTok. Vui lòng thử lại sau!"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun extractUrl(input: String): String {
        val regex = Regex("""https?://[^\s"'<]+""")
        val match = regex.find(input)
        return match?.value ?: ""
    }

    private fun fetchFromEndpoint(endpoint: String, targetUrl: String): VideoInfo? {
        try {
            val requestUrl = if (endpoint.contains("tikwm.com")) {
                "$endpoint?url=${URLEncoder.encode(targetUrl, "UTF-8")}"
            } else {
                endpoint
            }

            val requestBuilder = Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")

            val request = if (endpoint.contains("tikwm.com")) {
                requestBuilder.url(requestUrl).get().build()
            } else {
                val formBody = FormBody.Builder()
                    .add("url", targetUrl)
                    .build()
                requestBuilder.url(endpoint).post(formBody).build()
            }

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) return null
                val jsonString = response.body!!.string()
                val root = JsonParser.parseString(jsonString).asJsonObject

                val code = if (root.has("code")) root.get("code").asInt else -1
                if (code == 0 && root.has("data")) {
                    val data = root.getAsJsonObject("data")
                    val videoId = data.get("id")?.asString ?: "tiktok_${System.currentTimeMillis()}"
                    val title = data.get("title")?.asString ?: "TikTok Video"
                    val cover = data.get("cover")?.asString ?: ""
                    val authorObj = if (data.has("author")) data.getAsJsonObject("author") else null
                    val authorName = authorObj?.get("nickname")?.asString ?: authorObj?.get("unique_id")?.asString ?: "TikTok User"
                    val duration = data.get("duration")?.asInt ?: 0

                    val playNoWm = data.get("play")?.asString ?: ""
                    val playWm = data.get("wmplay")?.asString ?: ""
                    val musicUrl = data.get("music")?.asString ?: ""

                    val sizeBytesApi = data.get("size")?.asLong ?: 0L

                    val options = mutableListOf<DownloadOption>()

                    if (playNoWm.isNotEmpty()) {
                        val realSize = if (sizeBytesApi > 0) sizeBytesApi else fetchContentLength(playNoWm)
                        options.add(
                            DownloadOption(
                                label = "Không logo (No Watermark)",
                                url = playNoWm,
                                quality = "HD",
                                sizeBytes = realSize,
                                formattedSize = formatSize(realSize),
                                isNoWatermark = true,
                                isAudioOnly = false,
                                fileExtension = "mp4"
                            )
                        )
                    }

                    if (playWm.isNotEmpty()) {
                        val realSize = fetchContentLength(playWm)
                        options.add(
                            DownloadOption(
                                label = "Có logo (Watermark)",
                                url = playWm,
                                quality = "SD",
                                sizeBytes = realSize,
                                formattedSize = formatSize(realSize),
                                isNoWatermark = false,
                                isAudioOnly = false,
                                fileExtension = "mp4"
                            )
                        )
                    }

                    if (musicUrl.isNotEmpty()) {
                        val musicSize = fetchContentLength(musicUrl)
                        options.add(
                            DownloadOption(
                                label = "Nhạc MP3 (Audio)",
                                url = musicUrl,
                                quality = "Audio",
                                sizeBytes = musicSize,
                                formattedSize = formatSize(musicSize),
                                isNoWatermark = false,
                                isAudioOnly = true,
                                fileExtension = "mp3"
                            )
                        )
                    }

                    return VideoInfo(
                        id = videoId,
                        title = title,
                        author = authorName,
                        coverUrl = cover,
                        durationSeconds = duration,
                        platform = Platform.TIKTOK,
                        options = options
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchContentLength(url: String): Long {
        try {
            val req = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val len = res.header("Content-Length")?.toLongOrNull()
                    if (len != null && len > 0) return len
                }
            }
        } catch (_: Exception) {}
        return 0L
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "Kích thước N/A"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            "${(mb * 100).roundToInt() / 100.0} MB"
        } else {
            "${(kb * 100).roundToInt() / 100.0} KB"
        }
    }
}
