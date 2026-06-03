package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.utils.BibleApiService
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BibleTabsScreen(
    onOpenPsalm: (Int) -> Unit,
    refreshSignal: StateFlow<Long>? = null,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Biblija, 1: Psalmi
    var bibleLang by rememberSaveable { mutableStateOf(BibleApiService.BibleLanguage.CROATIAN) }
    var psalmLang by rememberSaveable { mutableStateOf(BibleApiService.BibleLanguage.CROATIAN) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(Strings.bibleTitle) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(Strings.psalms) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                LanguageDropdown(
                    label = "Bible",
                    value = bibleLang,
                    onChange = { bibleLang = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LanguageDropdown(
                    label = "Psalms",
                    value = psalmLang,
                    onChange = { psalmLang = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (selectedTab) {
            0 -> BibleHomeScreen(
                onOpenPsalm = onOpenPsalm,
                languageOverride = bibleLang,
                useEnglishLabels = bibleLang == BibleApiService.BibleLanguage.ENGLISH,
                onOpenLeftDrawer = onOpenLeftDrawer,
                onOpenRightDrawer = onOpenRightDrawer
            )
            1 -> MainPsalmScreen(
                refreshSignal = refreshSignal,
                languageOverride = psalmLang,
                useEnglishLabels = psalmLang == BibleApiService.BibleLanguage.ENGLISH
            )
        }
    }
}

@Composable
private fun LanguageDropdown(
    label: String,
    value: BibleApiService.BibleLanguage,
    onChange: (BibleApiService.BibleLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        val display = when (value) {
            BibleApiService.BibleLanguage.ENGLISH -> "English"
            BibleApiService.BibleLanguage.GERMAN -> "Deutsch"
            BibleApiService.BibleLanguage.CROATIAN -> "Hrvatski"
        }
        AssistChip(
            onClick = { expanded = true },
            label = { Text("$label: $display", maxLines = 1) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(Strings.croatian) },
                onClick = {
                    onChange(BibleApiService.BibleLanguage.CROATIAN)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(Strings.english) },
                onClick = {
                    onChange(BibleApiService.BibleLanguage.ENGLISH)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(Strings.german) },
                onClick = {
                    onChange(BibleApiService.BibleLanguage.GERMAN)
                    expanded = false
                }
            )
        }
    }
}
