package com.youtube.rating.android.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompactRatingBadge(
    emoji: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
    showWhenZero: Boolean = false
) {
    if (!showWhenZero && value <= 0.0) return
    val surface = MaterialTheme.colorScheme.surface
    val fallbackContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f).compositeOver(surface)
    val containerColor = if (color.luminance() > 0.86f) {
        fallbackContainer
    } else {
        color.copy(alpha = 0.16f).compositeOver(surface)
    }
    val textColor = if (color.luminance() > 0.86f) {
        MaterialTheme.colorScheme.onSurface
    } else {
        color
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
        modifier = modifier
    ) {
        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) {
            Text(text = emoji, fontSize = 11.sp)
            Text(
                text = " ${"%.1f".format(value)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
