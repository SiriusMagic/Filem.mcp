package com.zirakusa.filem.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.ui.screens.*
import com.zirakusa.filem.ui.theme.FilemColors
import kotlinx.coroutines.launch

/** Drawer (Papelera, Configuración) around whichever screen [FilesState.screen] says; back goes through the state. */
@Composable fun FilemApp(state: FilesState, actions: Actions, onExit: () -> Unit) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it); state.message = null } }
    BackHandler {
        if (drawer.isOpen) scope.launch { drawer.close() } else if (!state.back()) onExit()
    }
    ModalNavigationDrawer(drawerState = drawer, gesturesEnabled = state.screen == Screen.Home, drawerContent = {
        ModalDrawerSheet(drawerContainerColor = FilemColors.Scrim) {
            Text(stringResource(R.string.app_name), Modifier.padding(28.dp), fontSize = 28.sp, color = FilemColors.Text)
            fun go(screen: Screen) { scope.launch { drawer.close() }; state.go(screen) }
            NavigationDrawerItem({ Column { Text(stringResource(R.string.trash)); Text(FileRules.size(state.trash.sumOf { it.size }), color = FilemColors.Muted) } },
                false, { go(Screen.Trash) }, icon = { Icon(Icons.Outlined.Delete, null) }, modifier = Modifier.padding(horizontal = 12.dp))
            NavigationDrawerItem({ Text(stringResource(R.string.settings)) }, false, { go(Screen.Settings) },
                icon = { Icon(Icons.Outlined.Settings, null) }, modifier = Modifier.padding(horizontal = 12.dp))
        }
    }) {
        Box(Modifier.fillMaxSize().background(FilemColors.Background)) {
            when (val s = state.screen) {
                Screen.Home -> HomeScreen(state, actions) { scope.launch { drawer.open() } }
                is Screen.Browse -> BrowserScreen(state, actions)
                is Screen.CategoryList -> BrowserScreen(state, actions, stringResource(categoryNames.getValue(s.category)))
                Screen.Starred -> BrowserScreen(state, actions, stringResource(R.string.starred))
                Screen.Search -> SearchScreen(state, actions)
                Screen.Trash -> TrashScreen(state)
                Screen.Settings -> SettingsScreen(state)
                Screen.Mcp -> McpScreen(actions::copyLink, actions::goToHub) { state.back() }
            }
            SnackbarHost(snack, Modifier.align(androidx.compose.ui.Alignment.BottomCenter).navigationBarsPadding())
        }
    }
}
