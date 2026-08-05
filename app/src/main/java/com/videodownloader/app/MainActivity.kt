package com.videodownloader.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.videodownloader.app.data.config.AppUpdateInfo
import com.videodownloader.app.data.updater.AppUpdater
import com.videodownloader.app.ui.screens.MainScreen
import com.videodownloader.app.ui.theme.VideoDownloaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf("")
    private var updateInfoState by mutableStateOf<AppUpdateInfo?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()
        handleSharedIntent(intent)

        // Check for OTA update in background
        lifecycleScope.launch {
            val update = AppUpdater.checkForUpdates(this@MainActivity)
            if (update != null) {
                updateInfoState = update
            }
        }

        setContent {
            VideoDownloaderTheme {
                MainScreen(
                    initialSharedUrl = sharedUrl,
                    updateInfo = updateInfoState,
                    onStartUpdateInstall = { apkUrl ->
                        Toast.makeText(this, "Đang tải bản cập nhật ứng dụng...", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            val success = AppUpdater.downloadAndInstallApk(this@MainActivity, apkUrl) { _ -> }
                            if (!success) {
                                Toast.makeText(this@MainActivity, "Không thể tải bản cập nhật", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent != null && intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNull_or_empty()) {
                sharedUrl = extractUrl(sharedText!!)
            }
        }
    }

    private fun extractUrl(input: String): String {
        val regex = Regex("""https?://[^\s"'<]+""")
        val match = regex.find(input)
        return match?.value ?: input
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
