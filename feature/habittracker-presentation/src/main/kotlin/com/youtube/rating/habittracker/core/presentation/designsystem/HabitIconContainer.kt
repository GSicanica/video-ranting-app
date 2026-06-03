package com.youtube.rating.habittracker.core.presentation.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.youtube.rating.habittracker.feature.habits.domain.HabitIcon
import com.youtube.rating.habittracker.ui.theme.HabitTrackerTheme

@Composable
fun HabitIconContainer(
    icon: HabitIcon,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = HabitTrackerTheme.colors.surfaceElevated,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    borderStroke: BorderStroke? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (borderStroke != null) Modifier.border(borderStroke, shape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = HabitIcons.painter(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
