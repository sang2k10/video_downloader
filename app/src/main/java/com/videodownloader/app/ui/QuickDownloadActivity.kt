package com.videodownloader.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videodownloader.app.data.downloader.VideoDownloader
import com.videodownloader.app.data.model.DownloadOption
import com.videodownloader.app.data.model.Platform
import com.videodownloader.app.data.model.VideoInfo
import com.videodownloader.app.data.scraper.FacebookScraper
import com.videodownloader.app.data.scraper.TikTokScraper
import com.videodownloader.app.ui.theme.FacebookColor
import com.videodownloader.app.ui.theme.SecondaryAccent
import com.videodownloader.app.ui.theme.TikTokColor
import com.videodownloader.app.ui.theme.VideoDownloaderTheme
import kotlinx.coroutines.launch

class QuickDownloadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var initialUrl = ""
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString() ?: ""
            if (text.contains("tiktok.com", ignoreCase = true) || text.contains("facebook.com", ignoreCase = true) || text.contains("fb.watch", ignoreCase = true)) {
                initialUrl = extractUrl(text)
            }
        }

        setContent {
            VideoDownloaderTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(16.dp)
                            .clickable(enabled = false) {}
                    ) {
                        PopupDownloadContent(
                            initialUrl = initialUrl,
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun extractUrl(input: String): String {
        val regex = Regex("""https?://[^\s"'<]+""")
        val match = regex.find(input)
        return match?.value ?: input
    }
}

@Composable
fun PopupDownloadContent(
    initialUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf(initialUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var selectedOption by remember { mutableStateOf<DownloadOption?>(null) }

    val scrollState = rememberScrollState()

    // Auto-fetch if URL detected in clipboard
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotEmpty()) {
            isLoading = true
            scope.launch {
                val isTikTok = initialUrl.contains("tiktok.com", ignoreCase = true)
                val isFB = initialUrl.contains("facebook.com", ignoreCase = true) || initialUrl.contains("fb.watch", ignoreCase = true)

                val result = when {
                    isTikTok -> TikTokScraper.parseTikTokUrl(initialUrl)
                    isFB -> FacebookScraper.parseFacebookUrl(initialUrl)
                    else -> TikTokScraper.parseTikTokUrl(initialUrl)
                }

                isLoading = false
                if (result.isSuccess) {
                    videoInfo = result.getOrNull()
                    selectedOption = videoInfo?.options?.firstOrNull()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Unable to fetch video"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(18.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quick Downloader",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Box
        OutlinedTextField(
            value = inputUrl,
            onValueChange = { inputUrl = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste URL...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Row {
                    if (inputUrl.isNotEmpty()) {
                        IconButton(onClick = { inputUrl = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = clipboard.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val text = clipData.getItemAt(0).text?.toString() ?: ""
                            if (text.isNotEmpty()) {
                                inputUrl = text
                                Toast.makeText(context, "Pasted!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Fetch Button
        Button(
            onClick = {
                val target = inputUrl.trim()
                if (target.isEmpty()) return@Button

                isLoading = true
                errorMessage = null
                videoInfo = null
                selectedOption = null

                scope.launch {
                    val isTikTok = target.contains("tiktok.com", ignoreCase = true)
                    val isFB = target.contains("facebook.com", ignoreCase = true) || target.contains("fb.watch", ignoreCase = true)

                    val result = when {
                        isTikTok -> TikTokScraper.parseTikTokUrl(target)
                        isFB -> FacebookScraper.parseFacebookUrl(target)
                        else -> TikTokScraper.parseTikTokUrl(target)
                    }

                    isLoading = false
                    if (result.isSuccess) {
                        videoInfo = result.getOrNull()
                        selectedOption = videoInfo?.options?.firstOrNull()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Unable to fetch video"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing...")
            } else {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fetch Video", fontWeight = FontWeight.Bold)
            }
        }

        // Error Msg
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp), fontSize = 13.sp)
                }
            }
        }

        // Video Result Card
        AnimatedVisibility(visible = videoInfo != null, enter = fadeIn(), exit = fadeOut()) {
            videoInfo?.let { info ->
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (info.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = info.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = if (info.platform == Platform.TIKTOK) TikTokColor else FacebookColor,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (info.platform == Platform.TIKTOK) "TikTok" else "Facebook",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = info.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    info.options.forEach { option ->
                        val isSelected = selectedOption == option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedOption = option },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = { selectedOption = option })
                                Icon(
                                    imageVector = if (option.isAudioOnly) Icons.Default.MusicNote else Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = if (option.isNoWatermark) SecondaryAccent else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = option.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                if (option.formattedSize.isNotEmpty()) {
                                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                        Text(text = option.formattedSize, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            selectedOption?.let { opt ->
                                VideoDownloader.downloadVideo(context, info, opt)
                                Toast.makeText(context, "Download started!", Toast.LENGTH_SHORT).show()
                                onClose()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                        enabled = selectedOption != null
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Now", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
