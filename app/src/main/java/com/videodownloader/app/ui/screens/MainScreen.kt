package com.videodownloader.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videodownloader.app.data.config.AppUpdateInfo
import com.videodownloader.app.data.downloader.VideoDownloader
import com.videodownloader.app.data.model.DownloadOption
import com.videodownloader.app.data.model.Platform
import com.videodownloader.app.data.model.VideoInfo
import com.videodownloader.app.data.scraper.FacebookScraper
import com.videodownloader.app.data.scraper.TikTokScraper
import com.videodownloader.app.data.updater.AppUpdater
import com.videodownloader.app.ui.theme.FacebookColor
import com.videodownloader.app.ui.theme.SecondaryAccent
import com.videodownloader.app.ui.theme.TikTokColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialSharedUrl: String = "",
    updateInfo: AppUpdateInfo? = null,
    onStartUpdateInstall: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf(initialSharedUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var selectedOption by remember { mutableStateOf<DownloadOption?>(null) }
    var showUpdateDialog by remember { mutableStateOf(updateInfo != null) }

    val scrollState = rememberScrollState()

    // Handle initial shared URL if app opened via Share
    LaunchedEffect(initialSharedUrl) {
        if (initialSharedUrl.isNotEmpty()) {
            inputUrl = initialSharedUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tải Video Fast",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    if (updateInfo != null) {
                        IconButton(onClick = { showUpdateDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Cập nhật",
                                tint = SecondaryAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tải Video TikTok & Facebook",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Không logo • Tùy chọn chất lượng HD/SD • Có kích thước MB",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Input URL Section
            OutlinedTextField(
                value = inputUrl,
                onValueChange = {
                    inputUrl = it
                    if (errorMessage != null) errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Dán link TikTok hoặc Facebook...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Row {
                        if (inputUrl.isNotEmpty()) {
                            IconButton(onClick = { inputUrl = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Xóa")
                            }
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotEmpty()) {
                                    inputUrl = text
                                    Toast.makeText(context, "Đã dán link!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Dán", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Process Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    val target = inputUrl.trim()
                    if (target.isEmpty()) {
                        errorMessage = "Vui lòng nhập hoặc dán URL video!"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    videoInfo = null
                    selectedOption = null

                    scope.launch {
                        val isTikTok = target.contains("tiktok.com", ignoreCase = true) || target.contains("douyin.com", ignoreCase = true)
                        val isFB = target.contains("facebook.com", ignoreCase = true) || target.contains("fb.watch", ignoreCase = true) || target.contains("fb.me", ignoreCase = true)

                        val result = when {
                            isTikTok -> TikTokScraper.parseTikTokUrl(target)
                            isFB -> FacebookScraper.parseFacebookUrl(target)
                            else -> {
                                // Try TikTok first then FB fallback
                                val ttRes = TikTokScraper.parseTikTokUrl(target)
                                if (ttRes.isSuccess) ttRes else FacebookScraper.parseFacebookUrl(target)
                            }
                        }

                        isLoading = false
                        if (result.isSuccess) {
                            videoInfo = result.getOrNull()
                            selectedOption = videoInfo?.options?.firstOrNull()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Không thể lấy thông tin video. Vui lòng kiểm tra lại link!"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Đang xử lý...")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lấy thông tin Video", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error Message Display
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Video Preview & Download Options Card
            AnimatedVisibility(
                visible = videoInfo != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                videoInfo?.let { info ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // Video Info Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (info.coverUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = info.coverUrl,
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    // Platform Badge
                                    Surface(
                                        color = if (info.platform == Platform.TIKTOK) TikTokColor else FacebookColor,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (info.platform == Platform.TIKTOK) "TikTok" else "Facebook",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = info.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (info.author.isNotEmpty()) {
                                        Text(
                                            text = "@${info.author}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            Text(
                                text = "Chọn độ phân giải / Định dạng:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Download options list
                            info.options.forEach { option ->
                                val isSelected = selectedOption == option
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedOption = option },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedOption = option }
                                        )

                                        Icon(
                                            imageVector = if (option.isAudioOnly) Icons.Default.MusicNote else Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = if (option.isNoWatermark) SecondaryAccent else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = option.label,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            if (option.quality.isNotEmpty()) {
                                                Text(
                                                    text = "Chất lượng: ${option.quality}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }

                                        if (option.formattedSize.isNotEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = option.formattedSize,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Big Download Now Button
                            Button(
                                onClick = {
                                    selectedOption?.let { opt ->
                                        VideoDownloader.downloadVideo(context, info, opt)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                                enabled = selectedOption != null
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tải về máy ngay",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // OTA Update Dialog
        if (showUpdateDialog && updateInfo != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SecondaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Có phiên bản mới (${updateInfo.latestVersionName})")
                    }
                },
                text = {
                    Text(
                        if (updateInfo.releaseNotes.isNotEmpty()) updateInfo.releaseNotes
                        else "Đã có bản cập nhật mới sửa lỗi chặn tải TikTok/Facebook. Bạn có muốn cập nhật ngay không?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUpdateDialog = false
                            onStartUpdateInstall(updateInfo.apkUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent)
                    ) {
                        Text("Cập nhật ngay")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Để sau")
                    }
                }
            )
        }
    }
}
