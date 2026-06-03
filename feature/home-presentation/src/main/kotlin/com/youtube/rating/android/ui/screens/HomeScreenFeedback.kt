@file:Suppress("FunctionName")

package com.youtube.rating.android.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.ui.models.RatingDraft
import com.youtube.rating.android.util.extractYouTubeVideoId
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.android.utils.getLanguageName
import com.youtube.rating.android.viewmodel.RatingViewModel
import com.youtube.rating.core.designsystem.components.SimpleRatingSelector
import kotlinx.coroutines.delay

@Composable
internal fun LoadingCard(text: String, url: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(text, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(8.dp))
            Text(url, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
internal fun ErrorCard(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onManualEntry: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.tryAgain)
                }
                if (onManualEntry != null) {
                    OutlinedButton(onClick = onManualEntry) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.enterLink)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RatingFormScreen(
    infoTitle: String,
    infoChannel: String,
    infoLanguage: String,
    infoThumbnail: String,
    draft: RatingDraft,
    onDraftChange: (RatingDraft) -> Unit,
    submitState: RatingViewModel.SubmitState,
    onSubmit: () -> Unit,
    urlForFallbackThumb: String,
    onHapticLight: () -> Unit
) {
    val ctx = LocalContext.current
    RatingSection(
        infoTitle = infoTitle,
        infoChannel = infoChannel,
        infoLanguage = infoLanguage,
        infoThumbnail = infoThumbnail,
        draft = draft,
        onDraftChange = onDraftChange,
        submitState = submitState,
        onSubmit = onSubmit,
        context = ctx,
        urlForFallbackThumb = urlForFallbackThumb,
        onHapticLight = onHapticLight
    )
}

@Composable
private fun RatingSection(
    infoTitle: String,
    infoChannel: String,
    infoLanguage: String,
    infoThumbnail: String,
    draft: RatingDraft,
    onDraftChange: (RatingDraft) -> Unit,
    submitState: RatingViewModel.SubmitState,
    onSubmit: () -> Unit,
    context: Context,
    urlForFallbackThumb: String,
    onHapticLight: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(
                                    infoThumbnail.ifEmpty {
                                        val id = extractYouTubeVideoId(urlForFallbackThumb)
                                        if (id != null) ThumbnailHelper.getThumbnailUrl(id) else infoThumbnail
                                    }
                                )
                                .size(Size.ORIGINAL)
                                .precision(Precision.INEXACT)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(false)
                                .error(android.R.drawable.ic_menu_gallery)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .fallback(android.R.drawable.ic_menu_gallery)
                                .build()
                        ),
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(infoTitle, fontWeight = FontWeight.Normal)
                    Spacer(Modifier.height(4.dp))
                    Text(infoChannel, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (infoLanguage != "unknown") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Jezik: ${getLanguageName(infoLanguage)}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (infoLanguage == "unknown") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(Strings.languageNotRecognized, fontWeight = FontWeight.Normal)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = draft.manualLanguage,
                            onValueChange = { onDraftChange(draft.copy(manualLanguage = it)) },
                            label = { Text(Strings.enterLanguageHint) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Koliko raste Vjera, Ljubav i Nada?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal
            )
        }

        item {
            SimpleRatingSelector(
                title = "❤️ Ljubav",
                rating = draft.love,
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Filled.FavoriteBorder,
                onRatingChange = {
                    onHapticLight()
                    onDraftChange(draft.copy(love = it))
                }
            )
        }

        item {
            SimpleRatingSelector(
                title = "✝️ Vjera",
                rating = draft.faith,
                selectedIcon = Icons.Filled.Star,
                unselectedIcon = Icons.Outlined.StarBorder,
                onRatingChange = {
                    onHapticLight()
                    onDraftChange(draft.copy(faith = it))
                }
            )
        }

        item {
            SimpleRatingSelector(
                title = "⭐ Nada",
                rating = draft.hope,
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                onRatingChange = {
                    onHapticLight()
                    onDraftChange(draft.copy(hope = it))
                }
            )
        }

        item {
            val canSend = draft.love > 0 && draft.faith > 0 && draft.hope > 0
            val missing = buildList {
                if (draft.love == 0) add(Strings.ratingTypeLove.substring(3))
                if (draft.faith == 0) add(Strings.ratingTypeFaith.substring(3))
                if (draft.hope == 0) add(Strings.ratingTypeHope.substring(2))
            }.joinToString()

            if (!canSend) {
                Text(
                    text = if (missing.isNotBlank()) "${Strings.selectVideos} $missing" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSend && submitState !is RatingViewModel.SubmitState.Loading
            ) {
                if (submitState is RatingViewModel.SubmitState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.sending)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.sendRating)
                }
            }
        }
    }
}

@Composable
internal fun SubmitBanner(submitState: RatingViewModel.SubmitState) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(submitState) {
        visible = submitState is RatingViewModel.SubmitState.Success || submitState is RatingViewModel.SubmitState.Error
        if (visible) {
            delay(1600)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)) + slideInVertically(tween(220)) { -it / 2 },
        exit = fadeOut(tween(120)) + slideOutVertically(tween(200)) { -it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val container = when (submitState) {
                is RatingViewModel.SubmitState.Success -> Color(0xFF2E7D32)
                is RatingViewModel.SubmitState.Error -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.primaryContainer
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = container),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (submitState) {
                            is RatingViewModel.SubmitState.Success -> Icons.Default.CheckCircle
                            is RatingViewModel.SubmitState.Error -> Icons.Default.Error
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = Color.White
                    )

                    Text(
                        text = when (submitState) {
                            is RatingViewModel.SubmitState.Success -> "Ocjena uspješno poslana! ✓"
                            is RatingViewModel.SubmitState.Error -> submitState.message
                            else -> ""
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
