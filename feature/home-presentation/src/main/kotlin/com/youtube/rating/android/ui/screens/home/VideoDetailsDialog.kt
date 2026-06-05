@file:Suppress("FunctionName")
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.youtube.rating.android.ui.screens.home.dialogs

import com.youtube.rating.core.coroutines.ioDispatcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import com.youtube.rating.android.localization.Strings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.youtube.rating.android.data.ClipsRepository
import com.youtube.rating.android.storage.WatchHistoryEntry
import com.youtube.rating.android.storage.WatchHistoryManager
import com.youtube.rating.android.ui.screens.YouTubePlayerEmbed
import com.youtube.rating.android.util.formatRelativeTime
import com.youtube.rating.android.utils.CompactRatingBadge
import com.youtube.rating.android.utils.DailyActionLimiter
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.utils.rememberHapticFeedback
import com.youtube.rating.android.viewmodel.RatingViewModel
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.shared.api.RatingApiClient
import org.koin.compose.koinInject
import com.youtube.rating.shared.models.VideoSearchResult
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.youtube.rating.android.data.prefs.VideoPrefs
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * Video Details Dialog - Full-screen dialog with video player and ratings
 * Extracted from HomeScreen.kt to improve modularity
 */
@Composable
fun VideoDetailsDialog(
    video: VideoSearchResult,
    onQuickRate: () -> Unit,
    submitState: RatingViewModel.SubmitState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onCloseNoMini: () -> Unit,
    onDismiss: () -> Unit,
    isInPipMode: Boolean,
    onFullScreenVideoActiveChange: (Boolean) -> Unit,
    onPlaybackSecond: (Float) -> Unit = {},
    initialStartSeconds: Int = 0
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deps: com.youtube.rating.android.viewmodel.VideoDetailsDepsViewModel = koinInject()
    val appContext = context.applicationContext
    val haptics = LocalHapticFeedback.current
    val isClipMode = video.totalRatings == 0
    var showClipForm by rememberSaveable(video.videoId) { mutableStateOf(false) }
    val thumbnailRequest = remember(context, video.videoId, video.thumbnail) {
        val thumb = video.thumbnail.ifBlank { ThumbnailHelper.getThumbnailUrl(video.videoId) }
        ImageRequest.Builder(context)
            .data(thumb)
            .size(360, 216)
            .crossfade(false)
            .build()
    }
    val avgText = remember(video.totalRatings, video.avgTotal) {
        if (video.totalRatings > 0) {
            String.format(Locale.getDefault(), "%.1f", video.avgTotal)
        } else {
            "n/a"
        }
    }
    val titleText = remember(video.title) { video.title.ifBlank { "Bez naslova" } }
    val channelText = remember(video.channelName) { video.channelName.ifBlank { "Nepoznat kanal" } }
    val metaText = remember(video.language, video.category, video.createdAt) {
        buildList {
            val lang = video.language.trim()
            if (lang.isNotEmpty() && lang.lowercase() != "unknown") add(lang.uppercase())
            val cat = video.category?.trim().orEmpty()
            if (cat.isNotEmpty()) add(cat)
            if (video.createdAt > 0L) add(video.createdAt.formatRelativeTime())
        }.joinToString(" • ")
    }
    val chipsScrollState = rememberScrollState()
    val resumePlaybackEnabled by VideoPrefs.resumePlaybackEnabledFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val initialStart = remember(video.videoId, initialStartSeconds) {
        initialStartSeconds.coerceAtLeast(0)
    }
    var resumeSecond by remember(video.videoId) { mutableStateOf(0) }
    var lastSavedSecond by remember(video.videoId) { mutableStateOf(0) }

    LaunchedEffect(video.videoId) {
        onFullScreenVideoActiveChange(true)
    }
    LaunchedEffect(video.videoId, resumePlaybackEnabled, initialStart) {
        if (initialStart > 0) {
            resumeSecond = initialStart
            return@LaunchedEffect
        }
        if (resumePlaybackEnabled) {
            val (lastVideoId, lastSecond) = VideoPrefs.getLastPlaybackPosition(appContext)
            resumeSecond = if (lastVideoId == video.videoId) {
                (lastSecond - 5).coerceAtLeast(0)
            } else {
                0
            }
        } else {
            resumeSecond = 0
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            onFullScreenVideoActiveChange(false)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isInPipMode) 0.dp else 10.dp),
            shape = if (isInPipMode) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isInPipMode && submitState is RatingViewModel.SubmitState.Error) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = submitState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (!isInPipMode) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = thumbnailRequest,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 120.dp, height = 72.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = titleText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = channelText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (metaText.isNotBlank()) {
                                        Text(
                                            text = metaText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(chipsScrollState),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val chipColors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        AssistChip(
                                            onClick = {},
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("${Strings.totalRatings} ${video.totalRatings}") },
                                            colors = chipColors
                                        )
                                        AssistChip(
                                            onClick = {},
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.StarBorder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("${Strings.averageLabel} $avgText") },
                                            colors = chipColors
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = onCloseNoMini,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Zatvori")
                                    }
                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Mini player")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isClipMode) {
                                    FilledTonalIconButton(onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleFavorite()
                                    }) {
                                        Icon(
                                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null
                                        )
                                    }
                                    FilledTonalIconButton(onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onShare()
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onQuickRate()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(Strings.rateButton)
                                    }
                                }
                            }
                        }
                    }
                }

                VideoDetailsPlayer(
                    video = video,
                    player = {
                        YouTubePlayerEmbed(
                            videoId = video.videoId,
                            startSeconds = resumeSecond,
                            endSeconds = null,
                            thumbnail = video.thumbnail,
                            autoPlay = true,
                            onViewRectChanged = { rect ->
                            },
                            onCurrentSecond = { second ->
                                onPlaybackSecond(second)
                                if (resumePlaybackEnabled) {
                                    val sec = second.toInt().coerceAtLeast(0)
                                    if (sec - lastSavedSecond >= 5) {
                                        lastSavedSecond = sec
                                        scope.makeIOCall {
                                            VideoPrefs.saveLastPlaybackPosition(appContext, video.videoId, sec)
                                        }
                                    }
                                }
                            }
                        )
                    },
                    overlayContent = {
                        if (!isInPipMode && !isClipMode) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showClipForm = !showClipForm
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = "Otvori unos isječka",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    extraContent = {
                        if (!isInPipMode && !isClipMode && showClipForm) {
                            ClipMakerForm(
                                video = video,
                                onSaved = { start, end ->
                                    scope.launch(ioDispatcher) {
                                        ClipsRepository.createAndSave(
                                            context = context,
                                            videoId = video.videoId,
                                            title = video.title,
                                            thumbnail = video.thumbnail,
                                            channel = video.channelName,
                                            startSeconds = start,
                                            endSeconds = end
                                        )
                                    }
                                }
                            )
                        }
                    },
                    deps = deps
                )
            }
        }
    }
}

@Composable
internal fun VideoDetailsPlayer(
    video: VideoSearchResult,
    player: @Composable () -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit = {},
    extraContent: @Composable () -> Unit = {},
    deps: com.youtube.rating.android.viewmodel.VideoDetailsDepsViewModel
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val watchHistoryRepository = deps.watchHistoryRepository

    DisposableEffect(video.videoId) {
        val historyScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + ioDispatcher)
        val startedAt = System.currentTimeMillis()
        onDispose {
            val watchedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
            val job = historyScope.launch {
                val historyEnabled = VideoPrefs.getWatchHistoryEnabled(appContext)
                if (!historyEnabled) return@launch

                val entry = WatchHistoryEntry(
                    videoId = video.videoId,
                    title = video.title,
                    thumbnail = video.thumbnail,
                    channelName = video.channelName,
                    category = null,
                    watchDuration = watchedMs,
                    totalDuration = 0L,
                    viewedAt = System.currentTimeMillis()
                )
                WatchHistoryManager.getInstance(appContext).addToHistory(entry)
                runCatching { watchHistoryRepository.recordView(entry) }
            }
            job.invokeOnCompletion { historyScope.cancel() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .weight(1f, fill = true)
                .background(Color.Black)
        ) {
            player()
            overlayContent()
        }
        Spacer(Modifier.height(8.dp))
        extraContent()
    }
}

@Composable
internal fun VideoRatingsPanel(
    video: VideoSearchResult,
    onQuickRate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Ukupno ocjena: ${video.totalRatings}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactRatingBadge("❤️", video.avgLove, Color(0xFFE91E63))
            CompactRatingBadge("✝️", video.avgFaith, Color(0xFF2196F3))
            CompactRatingBadge("⭐", video.avgHope, Color(0xFFFFC107))
        }

        Button(
            onClick = onQuickRate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(Strings.rateVideo)
        }
    }
}

@Composable
private fun ClipMakerForm(
    video: VideoSearchResult,
    onSaved: (startSeconds: Int, endSeconds: Int) -> Unit
) {
    var startMinutesText by remember(video.videoId) { mutableStateOf("") }
    var startSecondsText by remember(video.videoId) { mutableStateOf("") }
    var endMinutesText by remember(video.videoId) { mutableStateOf("") }
    var endSecondsText by remember(video.videoId) { mutableStateOf("") }

    val startValue = remember(startMinutesText, startSecondsText) {
        parseMinutesSecondsToSecondsOrNull(minutesText = startMinutesText, secondsText = startSecondsText)
    }
    val endValue = remember(endMinutesText, endSecondsText) {
        parseMinutesSecondsToSecondsOrNull(minutesText = endMinutesText, secondsText = endSecondsText)
    }
    val isValidRange = startValue != null && endValue != null && endValue > startValue
    val showValidationError = (
        startMinutesText.isNotBlank() ||
            startSecondsText.isNotBlank() ||
            endMinutesText.isNotBlank() ||
            endSecondsText.isNotBlank()
        ) && !isValidRange
    val spacing = 12.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Text(
            text = "Izradi isječak",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = "Početak", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = startMinutesText,
                onValueChange = { startMinutesText = it.digitsOnly(maxLen = 6) },
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = startSecondsText,
                onValueChange = { input ->
                    val (newMinutes, newSeconds) = normalizeSecondsIntoMinutes(
                        minutesText = startMinutesText,
                        secondsText = input
                    )
                    startMinutesText = newMinutes
                    startSecondsText = newSeconds
                },
                label = { Text("Sek") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "Možeš unijeti i ukupne sekunde u polje sekundi (npr. 83 → 1 min 23 sek).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(text = "Kraj", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = endMinutesText,
                onValueChange = { endMinutesText = it.digitsOnly(maxLen = 6) },
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endSecondsText,
                onValueChange = { input ->
                    val (newMinutes, newSeconds) = normalizeSecondsIntoMinutes(
                        minutesText = endMinutesText,
                        secondsText = input
                    )
                    endMinutesText = newMinutes
                    endSecondsText = newSeconds
                },
                label = { Text("Sek") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (showValidationError) {
            Text(
                text = "Unesi valjan raspon (kraj mora biti veći od početka).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = {
                val start = startValue ?: return@Button
                val end = endValue ?: return@Button
                if (end <= start) return@Button
                onSaved(start, end)
                startMinutesText = ""
                startSecondsText = ""
                endMinutesText = ""
                endSecondsText = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isValidRange
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Spremi isječak")
        }
    }
}

private fun String.digitsOnly(maxLen: Int? = null): String {
    val digits = buildString(length) {
        for (ch in this@digitsOnly) if (ch.isDigit()) append(ch)
    }
    return if (maxLen != null) digits.take(maxLen) else digits
}

private fun normalizeSecondsIntoMinutes(minutesText: String, secondsText: String): Pair<String, String> {
    val minutesDigits = minutesText.digitsOnly(maxLen = 6)
    val secondsDigits = secondsText.digitsOnly(maxLen = 6)

    if (secondsDigits.isBlank()) return minutesDigits to ""

    val secondsValue = secondsDigits.toLongOrNull() ?: return minutesDigits to secondsDigits.take(2)
    if (secondsValue < 60L) return minutesDigits to secondsDigits.take(2)

    val baseMinutes = minutesDigits.toLongOrNull() ?: 0L
    val carriedMinutes = secondsValue / 60L
    val normalizedMinutes = (baseMinutes + carriedMinutes).coerceAtMost(999_999L)
    val normalizedSeconds = (secondsValue % 60L).toString().padStart(2, '0')
    return normalizedMinutes.toString() to normalizedSeconds
}

private fun parseMinutesSecondsToSecondsOrNull(minutesText: String, secondsText: String): Int? {
    val minutesRaw = minutesText.trim()
    val secondsRaw = secondsText.trim()
    if (minutesRaw.isBlank() && secondsRaw.isBlank()) return null

    val minutes = when {
        minutesRaw.isBlank() -> 0
        else -> minutesRaw.toIntOrNull() ?: return null
    }
    val seconds = when {
        secondsRaw.isBlank() -> 0
        else -> secondsRaw.toIntOrNull() ?: return null
    }
    if (minutes < 0) return null
    if (seconds !in 0..59) return null

    val total = minutes.toLong() * 60L + seconds.toLong()
    return total.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}
