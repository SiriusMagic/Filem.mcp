package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.theme.FilemColors

private val dates = listOf(0L to R.string.any_date, 1L to R.string.today, 7L to R.string.last_7_days,
    30L to R.string.last_30_days, 365L to R.string.last_year)

/**
 * «Buscar»: chips for type, date and order on top, «Buscar historial» and the results, and the field at the bottom
 * (next to the thumb) with the back button. With nothing typed it shows the newest files.
 */
@Composable fun SearchScreen(state: FilesState, actions: Actions) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var days by remember { mutableLongStateOf(0L) }
    var newest by remember { mutableStateOf(true) }
    val focus = remember { FocusRequester() }
    val current by rememberUpdatedState(query)
    // Opening a result keeps the query in the history.
    val recording = remember(actions) { object : Actions by actions {
        override fun open(item: FileItem) { state.rememberSearch(current); actions.open(item) }
    } }
    LaunchedEffect(query, category, days, newest) {
        val since = if (days == 0L) 0L else System.currentTimeMillis() - days * 86_400_000L
        state.search(query, category, since, newest)
    }
    Column(Modifier.fillMaxSize().background(FilemColors.Background).statusBarsPadding()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Choice(category?.let { stringResource(categoryNames.getValue(it)) } ?: stringResource(R.string.all_types),
                listOf<Pair<Category?, String>>(null to stringResource(R.string.all_types)) +
                    Category.entries.filter { !it.isPlace }.map { it to stringResource(categoryNames.getValue(it)) }) { category = it }
            Choice(stringResource(dates.first { it.first == days }.second), dates.map { it.first to stringResource(it.second) }) { days = it }
            Choice(stringResource(if (newest) R.string.sort_newest else R.string.sort_oldest),
                listOf(true to stringResource(R.string.sort_newest), false to stringResource(R.string.sort_oldest))) { newest = it }
        }
        if (query.isEmpty() && state.history.isNotEmpty()) History(state.history, { query = it }, state::clearHistory)
        if (query.isEmpty()) Text(stringResource(R.string.recently_modified), Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = FilemColors.Text, fontSize = 17.sp)
        Listing(state, recording, Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), size = 56) { state.back() }
            TextField(query, { query = it }, Modifier.weight(1f).height(56.dp).focusRequester(focus).testTag("search_field"),
                singleLine = true, shape = CircleShape,
                placeholder = { Text(stringResource(R.string.search_hint), fontSize = 17.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = FilemColors.Muted) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.clear)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { state.rememberSearch(query) }),
                colors = TextFieldDefaults.colors(focusedContainerColor = FilemColors.Card, unfocusedContainerColor = FilemColors.Card,
                    focusedIndicatorColor = FilemColors.Card, unfocusedIndicatorColor = FilemColors.Card))
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** «Buscar historial»: past queries as chips (tap to search again) and a bin to forget them. */
@Composable private fun History(history: List<String>, pick: (String) -> Unit, clear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.search_history), Modifier.weight(1f), fontSize = 15.sp, color = FilemColors.Muted)
        IconButton(clear) { Icon(Icons.Outlined.Delete, stringResource(R.string.clear_history), tint = FilemColors.Muted) }
    }
    FlowRow(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        history.forEach { q ->
            Text(q, Modifier.widthIn(max = 220.dp).clip(CircleShape).background(FilemColors.Card).clickable { pick(q) }
                .padding(horizontal = 16.dp, vertical = 9.dp), color = FilemColors.Text, fontSize = 15.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
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
