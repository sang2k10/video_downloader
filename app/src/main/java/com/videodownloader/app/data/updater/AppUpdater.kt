package com.videodownloader.app.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.videodownloader.app.data.config.AppUpdateInfo
import com.videodownloader.app.data.config.RemoteConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object AppUpdater {

    suspend fun checkForUpdates(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val config = RemoteConfigManager.fetchRemoteConfig()
        val updateInfo = config.updateInfo

        val currentVersionCode = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }

        if (updateInfo.latestVersionCode > currentVersionCode && updateInfo.apkUrl.isNotEmpty()) {
            return@withContext updateInfo
        }
        return@withContext null
    }

    suspend fun downloadAndInstallApk(context: Context, apkUrl: String, onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(apkUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) return@withContext false

            val body = response.body!!
            val contentLength = body.contentLength()
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val apkFile = File(cacheDir, "app-update.apk")
            if (apkFile.exists()) apkFile.delete()

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                outputStream.write(buffer, 0, bytesRead)

                if (contentLength > 0) {
                    val progress = ((totalRead * 100) / contentLength).toInt()
                    onProgress(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
