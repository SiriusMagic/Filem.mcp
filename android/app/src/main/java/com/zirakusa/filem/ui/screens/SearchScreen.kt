package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.theme.FilemColors

private val dates = listOf(0L to R.string.any_date, 1L to R.string.today, 7L to R.string.last_7_days,
    30L to R.string.last_30_days, 365L to R.string.last_year)

/** «Buscar»: name, plus chips for type, date and order. With nothing typed it shows the newest files. */
@Composable fun SearchScreen(state: FilesState, actions: Actions) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var days by remember { mutableLongStateOf(0L) }
    var newest by remember { mutableStateOf(true) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(query, category, days, newest) {
        val since = if (days == 0L) 0L else System.currentTimeMillis() - days * 86_400_000L
        state.search(query, category, since, newest)
    }
    Column(Modifier.fillMaxSize().background(FilemColors.Background)) {
        Row(Modifier.fillMaxWidth().background(FilemColors.Bar).statusBarsPadding().height(72.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton({ state.back() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = FilemColors.Text) }
            TextField(query, { query = it }, Modifier.weight(1f).focusRequester(focus), singleLine = true,
                placeholder = { Text(stringResource(R.string.search), fontSize = 20.sp) },
                colors = TextFieldDefaults.colors(focusedContainerColor = FilemColors.Bar, unfocusedContainerColor = FilemColors.Bar,
                    focusedIndicatorColor = FilemColors.Bar, unfocusedIndicatorColor = FilemColors.Bar))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Choice(category?.let { stringResource(categoryNames.getValue(it)) } ?: stringResource(R.string.all_types),
                listOf<Pair<Category?, String>>(null to stringResource(R.string.all_types)) +
                    Category.entries.filter { it != Category.DOWNLOADS }.map { it to stringResource(categoryNames.getValue(it)) }) { category = it }
            Choice(stringResource(dates.first { it.first == days }.second), dates.map { it.first to stringResource(it.second) }) { days = it }
            Choice(stringResource(if (newest) R.string.sort_newest else R.string.sort_oldest),
                listOf(true to stringResource(R.string.sort_newest), false to stringResource(R.string.sort_oldest))) { newest = it }
        }
        if (query.isEmpty()) Text(stringResource(R.string.recently_modified), Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = FilemColors.Text, fontSize = 17.sp)
        Listing(state, actions, Modifier.weight(1f))
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable private fun <T> Choice(label: String, options: List<Pair<T, String>>, pick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(true, { open = true }, { Text(label) }, trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FilemColors.Chip, selectedLabelColor = FilemColors.Text,
                selectedTrailingIconColor = FilemColors.Text))
        DropdownMenu(open, { open = false }, containerColor = FilemColors.Scrim) {
            options.forEach { (value, text) -> DropdownMenuItem({ Text(text) }, { open = false; pick(value) }) }
        }
    }
}
