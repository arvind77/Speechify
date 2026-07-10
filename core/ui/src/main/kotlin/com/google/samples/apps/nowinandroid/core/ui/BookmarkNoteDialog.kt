/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTextButton

private const val MAX_NOTE_LENGTH = 280

@Composable
fun BookmarkNoteDialog(
    initialNote: String = "",
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    isEditMode: Boolean = false,
    onDeleteNote: (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(initialNote) }
    val configuration = LocalConfiguration.current

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = configuration.screenWidthDp.dp - 80.dp),
        onDismissRequest = {
            if (!isEditMode) {
                onSave("")
            }
            onDismiss()
        },
        title = {
            Text(
                text = if (isEditMode) {
                    stringResource(R.string.core_ui_bookmark_note_edit_title)
                } else {
                    stringResource(R.string.core_ui_bookmark_note_dialog_title)
                },
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_NOTE_LENGTH) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.core_ui_bookmark_note_dialog_hint))
                    },
                    minLines = 3,
                    maxLines = 5,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.core_ui_bookmark_note_char_count, text.length),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (text.length >= MAX_NOTE_LENGTH) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        confirmButton = {
            NiaTextButton(
                onClick = {
                    onSave(text)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.core_ui_bookmark_note_dialog_save))
            }
        },
        dismissButton = {
            if (isEditMode && onDeleteNote != null) {
                NiaTextButton(
                    onClick = {
                        onDeleteNote()
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.core_ui_bookmark_note_delete))
                }
            } else {
                NiaTextButton(
                    onClick = {
                        onSave("")
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.core_ui_bookmark_note_dialog_skip))
                }
            }
        },
    )
}
