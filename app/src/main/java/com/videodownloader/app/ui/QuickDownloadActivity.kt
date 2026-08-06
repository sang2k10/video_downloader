package com.videodownloader.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
                ZaloStylePopupScreen(
                    initialUrl = initialUrl,
                    onClose = { finish() }
                )
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
fun ZaloStylePopupScreen(
    initialUrl: String,
    onClose: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.85f) + fadeIn(),
            exit = scaleOut(targetScale = 0.85f) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.70f else 0.90f)
                    .widthIn(max = 440.dp)
                    .padding(vertical = if (isLandscape) 8.dp else 20.dp)
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.TopCenter
            ) {
                // Main Pop-up Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp)
                ) {
                    ZaloPopupContent(
                        initialUrl = initialUrl,
                        isLandscape = isLandscape,
                        onClose = onClose
                    )
                }

                // Top Circular Floating Icon Avatar
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(8.dp, CircleShape)
                        .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZaloPopupContent(
    initialUrl: String,
    isLandscape: Boolean,
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

    // Auto-fetch if URL in clipboard on open
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
            .padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .heightIn(max = if (isLandscape) 320.dp else 520.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Header Title & Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Video Downloader",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // URL Input Box
        OutlinedTextField(
            value = inputUrl,
            onValueChange = { inputUrl = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste TikTok or Facebook URL...", fontSize = 13.sp) },
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
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Fetch Action Button
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
                .height(42.dp),
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
                Text("Fetch Video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Error Message
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Video Result Section
        AnimatedVisibility(visible = videoInfo != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            videoInfo?.let { info ->
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (info.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = info.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
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
                            Text(
                                text = info.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

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
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background
                            )
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

                    Spacer(modifier = Modifier.height(10.dp))

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
                            .height(42.dp),
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
