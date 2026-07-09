/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.feature.bookmarks.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditNoteState(val resourceId: String, val currentNote: String)

/** Holds an id and its note for undo purposes. */
data class RemovedBookmark(val id: String, val note: String?)

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
) : ViewModel() {

    var shouldDisplayUndoBookmark by mutableStateOf(false)
        private set
    private var lastRemovedBookmarks: List<RemovedBookmark> = emptyList()

    var editingNoteState: EditNoteState? by mutableStateOf(null)
        private set

    /** IDs of cards currently checked by the user. */
    var selectedIds: Set<String> by mutableStateOf(emptySet())
        private set

    val feedUiState: StateFlow<NewsFeedUiState> =
        userNewsResourceRepository.observeAllBookmarked()
            .map<List<UserNewsResource>, NewsFeedUiState>(NewsFeedUiState::Success)
            .onStart { emit(Loading) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Loading,
            )

    fun removeFromSavedResources(newsResourceId: String) {
        val note = (feedUiState.value as? NewsFeedUiState.Success)
            ?.feed?.find { it.id == newsResourceId }?.bookmarkNote
        viewModelScope.launch {
            shouldDisplayUndoBookmark = true
            lastRemovedBookmarks = listOf(RemovedBookmark(newsResourceId, note))
            userDataRepository.setNewsResourceBookmarked(newsResourceId, false)
        }
        selectedIds = selectedIds - newsResourceId
    }

    fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setNewsResourceViewed(newsResourceId, viewed)
        }
    }

    fun undoBookmarkRemoval() {
        viewModelScope.launch {
            lastRemovedBookmarks.forEach { removed ->
                userDataRepository.setNewsResourceBookmarked(removed.id, true)
                removed.note?.takeIf { it.isNotBlank() }?.let {
                    userDataRepository.setBookmarkNote(removed.id, it)
                }
            }
        }
        clearUndoState()
    }

    fun clearUndoState() {
        shouldDisplayUndoBookmark = false
        lastRemovedBookmarks = emptyList()
    }

    // Note editing

    fun startEditNote(resourceId: String, currentNote: String) {
        editingNoteState = EditNoteState(resourceId, currentNote)
    }

    fun saveNote(resourceId: String, note: String) {
        viewModelScope.launch {
            userDataRepository.setBookmarkNote(resourceId, note)
        }
        editingNoteState = null
    }

    fun cancelEditNote() {
        editingNoteState = null
    }

    // Checkbox selection

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun selectAll(allIds: List<String>) {
        selectedIds = allIds.toSet()
    }

    fun clearSelection() {
        selectedIds = emptySet()
    }

    fun removeSelectedBookmarks() {
        val currentFeed = (feedUiState.value as? NewsFeedUiState.Success)?.feed ?: return
        val removed = currentFeed
            .filter { it.id in selectedIds }
            .map { RemovedBookmark(it.id, it.bookmarkNote) }
        if (removed.isEmpty()) return

        viewModelScope.launch {
            removed.forEach { userDataRepository.setNewsResourceBookmarked(it.id, false) }
        }
        lastRemovedBookmarks = removed
        shouldDisplayUndoBookmark = true
        selectedIds = emptySet()
    }
}
