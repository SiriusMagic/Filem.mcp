package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.data.Sort
import com.zirakusa.filem.data.SortBy
import com.zirakusa.filem.ui.theme.FilemColors

/** «Carpeta nueva» / «Cambiar nombre»: one field, the button only works with a valid name. */
@Composable fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val dot = initial.lastIndexOf('.').takeIf { it > 0 } ?: initial.length
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, dot))) }
    val focus = remember { FocusRequester() }
    val valid = FileRules.nameProblem(value.text.trim()) == null && value.text.trim() != initial
    AlertDialog(onDismiss, shape = RoundedCornerShape(28.dp), containerColor = FilemColors.Scrim,
        title = { Text(title, fontSize = 26.sp) },
        text = {
            OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onDone(value.text) }))
        },
        confirmButton = { Button({ onDone(value.text) }, enabled = valid) { Text(confirm) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable fun SortDialog(current: Sort, onDismiss: () -> Unit, onPick: (Sort) -> Unit) {
    val options = listOf(Sort(SortBy.NAME) to R.string.sort_name_az, Sort(SortBy.NAME, true) to R.string.sort_name_za,
        Sort(SortBy.DATE, true) to R.string.sort_newest, Sort(SortBy.DATE) to R.string.sort_oldest,
        Sort(SortBy.SIZE, true) to R.string.sort_largest, Sort(SortBy.SIZE) to R.string.sort_smallest, Sort(SortBy.TYPE) to R.string.sort_type)
    AlertDialog(onDismiss, containerColor = FilemColors.Scrim, title = { Text(stringResource(R.string.sort_by)) },
        text = {
            Column {
                options.forEach { (sort, label) ->
                    Row(Modifier.fillMaxWidth().selectable(sort == current) { onPick(sort) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(sort == current, null)
                        Text(stringResource(label), Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable fun InfoDialog(item: FileItem, onDismiss: () -> Unit) = AlertDialog(onDismiss, containerColor = FilemColors.Scrim,
    title = { Text(item.name) },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.info_location, "/" + FileRules.parent(item.path)))
            if (item.isDir) Text(stringResource(R.string.info_items, item.children))
            else Text(stringResource(R.string.info_size, FileRules.size(item.size)))
            Text(stringResource(R.string.info_modified, friendlyDate(item.modified)))
        }
    },
    confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.close)) } })

@Composable fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) =
    AlertDialog(onDismiss, containerColor = FilemColors.Scrim, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = FilemColors.Danger) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
