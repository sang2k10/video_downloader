package com.videodownloader.app.data.scraper

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
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.roundToInt

object FacebookScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun parseFacebookUrl(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = extractUrl(url)
            if (cleanUrl.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("URL Facebook không hợp lệ"))
            }

            // 1. Try Remote Config proxy endpoints (FSave / SnapSave)
            val config = RemoteConfigManager.getConfig().scraperConfig
            val endpoints = config.facebookApiEndpoints.ifEmpty {
                listOf("https://fsave.net/proxy.php")
            }

            for (endpoint in endpoints) {
                val videoInfo = fetchFromFSaveProxy(endpoint, cleanUrl)
                if (videoInfo != null && videoInfo.options.isNotEmpty()) {
                    return@withContext Result.success(videoInfo)
                }
            }

            // 2. Direct Mobile Facebook HTML Parsing Engine Fallback
            val directInfo = fetchDirectFacebookHtml(cleanUrl)
            if (directInfo != null && directInfo.options.isNotEmpty()) {
                return@withContext Result.success(directInfo)
            }

            Result.failure(Exception("Không thể giải mã video Facebook. Vui lòng kiểm tra lại link!"))
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

    private fun fetchFromFSaveProxy(endpoint: String, targetUrl: String): VideoInfo? {
        try {
            val formBody = FormBody.Builder()
                .add("url", targetUrl)
                .build()

            val request = Request.Builder()
                .url(endpoint)
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://fsave.net/")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) return null
                val jsonString = response.body!!.string()
                val root = JsonParser.parseString(jsonString).asJsonObject

                val apiObj = if (root.has("api") && root.get("api").isJsonObject) root.getAsJsonObject("api") else null
                if (apiObj != null && apiObj.has("status") && apiObj.get("status").asString.lowercase() == "ok") {
                    val title = apiObj.get("title")?.asString ?: "Facebook Video"
                    val userInfo = if (apiObj.has("userInfo") && apiObj.get("userInfo").isJsonObject) apiObj.getAsJsonObject("userInfo") else null
                    val authorName = userInfo?.get("name")?.asString ?: "Facebook User"
                    val avatar = userInfo?.get("userAvatar")?.asString ?: ""

                    val mediaItems = if (apiObj.has("mediaItems") && apiObj.get("mediaItems").isJsonArray) apiObj.getAsJsonArray("mediaItems") else null
                    val options = mutableListOf<DownloadOption>()

                    mediaItems?.forEach { itemElem ->
                        if (itemElem.isJsonObject) {
                            val item = itemElem.asJsonObject
                            val type = item.get("type")?.asString ?: "Video"
                            val qualityStr = item.get("quality")?.asString ?: "SD"
                            val downloadUrl = item.get("url")?.asString ?: item.get("fileUrl")?.asString ?: ""
                            val sizeFormatted = item.get("formattedSize")?.asString ?: ""
                            val sizeBytes = item.get("size")?.asLong ?: 0L

                            if (downloadUrl.isNotEmpty()) {
                                val realSize = if (sizeBytes > 0) sizeBytes else fetchContentLength(downloadUrl)
                                val finalFormattedSize = if (sizeFormatted.isNotEmpty()) sizeFormatted else formatSize(realSize)
                                val isAudio = type.lowercase() == "audio"

                                options.add(
                                    DownloadOption(
                                        label = if (isAudio) "Âm thanh MP3" else "Video Facebook ($qualityStr)",
                                        url = downloadUrl,
                                        quality = qualityStr,
                                        sizeBytes = realSize,
                                        formattedSize = finalFormattedSize,
                                        isNoWatermark = false,
                                        isAudioOnly = isAudio,
                                        fileExtension = if (isAudio) "mp3" else "mp4"
                                    )
                                )
                            }
                        }
                    }

                    if (options.isNotEmpty()) {
                        return VideoInfo(
                            id = "fb_${System.currentTimeMillis()}",
                            title = title,
                            author = authorName,
                            coverUrl = avatar,
                            durationSeconds = 0,
                            platform = Platform.FACEBOOK,
                            options = options
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchDirectFacebookHtml(targetUrl: String): VideoInfo? {
        try {
            val req = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful || response.body == null) return null
                val html = response.body!!.string()

                val hdMatches = findRegexMatches(html, """["']hd_src["']\s*:\s*["']([^"']+)["']|["']browser_native_hd_url["']\s*:\s*["']([^"']+)["']""")
                val sdMatches = findRegexMatches(html, """["']sd_src["']\s*:\s*["']([^"']+)["']|["']browser_native_sd_url["']\s*:\s*["']([^"']+)["']""")

                val titleMatch = findRegexMatches(html, """<title[^>]*>(.*?)</title>""")
                val title = titleMatch.firstOrNull()?.replace(" | Facebook", "")?.trim() ?: "Facebook Video"

                val options = mutableListOf<DownloadOption>()

                if (hdMatches.isNotEmpty()) {
                    val hdUrl = hdMatches.first().replace("\\/", "/")
                    val size = fetchContentLength(hdUrl)
                    options.add(
                        DownloadOption(
                            label = "Facebook HD Video",
                            url = hdUrl,
                            quality = "HD (1080p)",
                            sizeBytes = size,
                            formattedSize = formatSize(size),
                            isNoWatermark = false,
                            isAudioOnly = false,
                            fileExtension = "mp4"
                        )
                    )
                }

                if (sdMatches.isNotEmpty()) {
                    val sdUrl = sdMatches.first().replace("\\/", "/")
                    val size = fetchContentLength(sdUrl)
                    options.add(
                        DownloadOption(
                            label = "Facebook SD Video",
                            url = sdUrl,
                            quality = "SD (480p)",
                            sizeBytes = size,
                            formattedSize = formatSize(size),
                            isNoWatermark = false,
                            isAudioOnly = false,
                            fileExtension = "mp4"
                        )
                    )
                }

                if (options.isNotEmpty()) {
                    return VideoInfo(
                        id = "fb_direct_${System.currentTimeMillis()}",
                        title = title,
                        author = "Facebook",
                        coverUrl = "",
                        durationSeconds = 0,
                        platform = Platform.FACEBOOK,
                        options = options
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun findRegexMatches(text: String, patternStr: String): List<String> {
        val list = mutableListOf<String>()
        val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val groupCount = matcher.groupCount()
            for (i in 1..groupCount) {
                val g = matcher.group(i)
                if (!g.isNullOrEmpty()) {
                    list.add(g!!)
                }
            }
        }
        return list
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()

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
