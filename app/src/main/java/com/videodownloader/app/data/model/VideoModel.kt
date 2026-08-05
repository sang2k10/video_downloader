package com.videodownloader.app.data.model

enum class Platform {
    TIKTOK,
    FACEBOOK,
    UNKNOWN
}

data class DownloadOption(
    val label: String,            // e.g. "Không logo", "Có logo", "HD", "SD", "MP3 Audio"
    val url: String,              // Download URL
    val quality: String = "",     // e.g. "1080p", "720p"
    val sizeBytes: Long = 0L,     // Exact or estimated size in bytes
    val formattedSize: String = "", // Pre-formatted size string, e.g. "15.4 MB"
    val isNoWatermark: Boolean = false,
    val isAudioOnly: Boolean = false,
    val fileExtension: String = "mp4"
)

data class VideoInfo(
    val id: String,
    val title: String,
    val author: String = "",
    val coverUrl: String = "",
    val durationSeconds: Int = 0,
    val platform: Platform,
    val options: List<DownloadOption> = emptyList()
)
