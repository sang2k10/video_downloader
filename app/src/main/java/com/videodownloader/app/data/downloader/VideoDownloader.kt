package com.videodownloader.app.data.downloader

import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.videodownloader.app.data.model.DownloadOption
import com.videodownloader.app.data.model.VideoInfo
import java.io.File

object VideoDownloader {

    fun downloadVideo(context: Context, videoInfo: VideoInfo, option: DownloadOption): Long {
        try {
            val extension = option.fileExtension.ifEmpty { if (option.isAudioOnly) "mp3" else "mp4" }
            val cleanTitle = sanitizeFilename(videoInfo.title)
            val fileName = "${cleanTitle}_${option.quality.replace(" ", "_")}_${System.currentTimeMillis()}.$extension"

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(option.url)

            val request = DownloadManager.Request(uri).apply {
                setTitle("${videoInfo.title} (${option.quality})")
                setDescription("Đang tải video từ ${videoInfo.platform.name}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "VideoDownloader/$fileName"
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Đã bắt đầu tải xuống: $fileName", Toast.LENGTH_SHORT).show()

            // Trigger MediaScanner so file shows in Android Gallery
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "VideoDownloader/$fileName"
            )
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(if (option.isAudioOnly) "audio/*" else "video/*")
            ) { _, _ -> }

            return downloadId
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi khi khởi chạy tải xuống: ${e.message}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("""[^\w\s\-\.]"""), "").trim().take(40).ifEmpty { "video" }
    }
}
