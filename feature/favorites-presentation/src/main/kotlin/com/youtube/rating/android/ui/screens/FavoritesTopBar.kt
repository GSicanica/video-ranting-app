@file:Suppress("FunctionName")

package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider as M2Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.theme.spacing

@Composable
internal fun FavoritesTopBarV2(
    favoritesCount: Int,
    galleryCount: Int,
    onOpenGallery: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onGroupModeChange: (GroupMode) -> Unit,
    onClearAll: (() -> Unit)?,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val spacing = MaterialTheme.spacing
    RatingTopAppBar(
        title = {
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(Strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = Strings.clearSearch)
                                }
                            }
                            IconButton(onClick = { searchOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = Strings.close)
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.large
                )
            } else {
                Text(
                    text = "${Strings.myFavorites} ($favoritesCount)",
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            if (!searchOpen) {
                if (galleryCount > 0) {
                    AssistChip(
                        onClick = onOpenGallery,
                        label = { Text(Strings.galleryCountLabel(galleryCount)) },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                        modifier = Modifier
                            .padding(end = spacing.xs)
                            .height(28.dp)
                    )
                }

                IconButton(onClick = { searchOpen = true }) {
                    Icon(Icons.Default.Search, contentDescription = Strings.search)
                }

                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = Strings.quickMenu)
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    Text(
                        Strings.sorting,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DropdownMenuItem(
                        text = { Text(Strings.latest) },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, null) },
                        onClick = { menuOpen = false; onSortChange(SortMode.NEWEST) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.oldest) },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, null) },
                        onClick = { menuOpen = false; onSortChange(SortMode.OLDEST) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.sortBestRated) },
                        leadingIcon = { Icon(Icons.Default.Star, null) },
                        onClick = { menuOpen = false; onSortChange(SortMode.BEST) }
                    )

                    M2Divider(Modifier.padding(vertical = 6.dp))

                    Text(
                        Strings.display,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.groups) },
                        leadingIcon = { Icon(Icons.Default.Menu, null) },
                        onClick = { menuOpen = false; onGroupModeChange(GroupMode.GROUPED) }
                    )
                    DropdownMenuItem(
                        text = { Text(Strings.singleList) },
                        leadingIcon = { Icon(Icons.Default.Menu, null) },
                        onClick = { menuOpen = false; onGroupModeChange(GroupMode.NONE) }
                    )
                    if (onClearAll != null) {
                        M2Divider(Modifier.padding(vertical = 6.dp))
                        DropdownMenuItem(
                            text = { Text(Strings.deleteAllSaved) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                            onClick = { menuOpen = false; onClearAll() }
                        )
                    }
                }
            }
        }
    )
}
