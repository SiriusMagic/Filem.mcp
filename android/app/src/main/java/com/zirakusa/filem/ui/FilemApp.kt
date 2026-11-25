package com.zirakusa.filem.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zirakusa.filem.R
import com.zirakusa.filem.ui.screens.*
import com.zirakusa.filem.ui.theme.FilemColors

/** Whichever screen [FilesState.screen] says, with the bottom bar on the two tabs; back goes through the state. */
@Composable fun FilemApp(state: FilesState, actions: Actions, onExit: () -> Unit) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it); state.message = null } }
    BackHandler { if (!state.back()) onExit() }
    Box(Modifier.fillMaxSize().background(FilemColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (val s = state.screen) {
                    Screen.Navigator -> NavigatorScreen(state)
                    Screen.Recent -> RecentScreen(state, actions)
                    is Screen.Browse -> BrowserScreen(state, actions)
                    is Screen.CategoryList -> BrowserScreen(state, actions, stringResource(categoryNames.getValue(s.category)))
                    Screen.Starred -> BrowserScreen(state, actions, stringResource(R.string.favorites))
                    Screen.Search -> SearchScreen(state, actions)
                    Screen.Trash -> TrashScreen(state)
                    Screen.Settings -> SettingsScreen(state, actions::openArka)
                }
            }
            val s = state.screen
            if (s == Screen.Navigator || s == Screen.Recent) TabBar(s, state::tab) { state.go(Screen.Search) }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}
