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

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.samples.apps.nowinandroid.core.analytics.LocalAnalyticsHelper
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaLoadingWheel
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.DraggableScrollbar
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.rememberDraggableScroller
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.scrollbarState
import com.google.samples.apps.nowinandroid.core.designsystem.theme.LocalTintTheme
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.ui.BookmarkNoteDialog
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Success
import com.google.samples.apps.nowinandroid.core.ui.NewsResourceCardExpanded
import com.google.samples.apps.nowinandroid.core.ui.TrackScreenViewEvent
import com.google.samples.apps.nowinandroid.core.ui.TrackScrollJank
import com.google.samples.apps.nowinandroid.core.ui.UserNewsResourcePreviewParameterProvider
import com.google.samples.apps.nowinandroid.core.ui.launchCustomChromeTab
import com.google.samples.apps.nowinandroid.core.ui.logNewsResourceOpened
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.R

@Composable
internal fun BookmarksScreen(
    onTopicClick: (String) -> Unit,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val feedState by viewModel.feedUiState.collectAsStateWithLifecycle()
    BookmarksScreen(
        feedState = feedState,
        onShowSnackbar = onShowSnackbar,
        removeFromBookmarks = viewModel::removeFromSavedResources,
        onNewsResourceViewed = { viewModel.setNewsResourceViewed(it, true) },
        onTopicClick = onTopicClick,
        modifier = modifier,
        shouldDisplayUndoBookmark = viewModel.shouldDisplayUndoBookmark,
        undoBookmarkRemoval = viewModel::undoBookmarkRemoval,
        clearUndoState = viewModel::clearUndoState,
        editingNoteState = viewModel.editingNoteState,
        onNoteClick = viewModel::startEditNote,
        onSaveNote = viewModel::saveNote,
        onCancelEditNote = viewModel::cancelEditNote,
        selectedIds = viewModel.selectedIds,
        onToggleSelection = viewModel::toggleSelection,
        onRemoveSelected = viewModel::removeSelectedBookmarks,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
    )
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun BookmarksScreen(
    feedState: NewsFeedUiState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    removeFromBookmarks: (String) -> Unit,
    onNewsResourceViewed: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    shouldDisplayUndoBookmark: Boolean = false,
    undoBookmarkRemoval: () -> Unit = {},
    clearUndoState: () -> Unit = {},
    editingNoteState: EditNoteState? = null,
    onNoteClick: (String, String) -> Unit = { _, _ -> },
    onSaveNote: (String, String) -> Unit = { _, _ -> },
    onCancelEditNote: () -> Unit = {},
    selectedIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    onRemoveSelected: () -> Unit = {},
    onSelectAll: (List<String>) -> Unit = {},
    onClearSelection: () -> Unit = {},
) {
    val bookmarkRemovedMessage = stringResource(id = R.string.feature_bookmarks_api_removed)
    val undoText = stringResource(id = R.string.feature_bookmarks_api_undo)

    LaunchedEffect(shouldDisplayUndoBookmark) {
        if (shouldDisplayUndoBookmark) {
            val snackBarResult = onShowSnackbar(bookmarkRemovedMessage, undoText)
            if (snackBarResult) undoBookmarkRemoval() else clearUndoState()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        clearUndoState()
    }

    editingNoteState?.let { noteState ->
        BookmarkNoteDialog(
            initialNote = noteState.currentNote,
            onSave = { note -> onSaveNote(noteState.resourceId, note) },
            onDismiss = onCancelEditNote,
            isEditMode = true,
            onDeleteNote = { onSaveNote(noteState.resourceId, "") },
        )
    }

    when (feedState) {
        Loading -> LoadingState(modifier)
        is Success -> if (feedState.feed.isNotEmpty()) {
            BookmarksGrid(
                feedState = feedState,
                removeFromBookmarks = removeFromBookmarks,
                onNewsResourceViewed = onNewsResourceViewed,
                onTopicClick = onTopicClick,
                onNoteClick = onNoteClick,
                selectedIds = selectedIds,
                onToggleSelection = onToggleSelection,
                onRemoveSelected = onRemoveSelected,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                modifier = modifier,
            )
        } else {
            EmptyState(modifier)
        }
    }

    TrackScreenViewEvent(screenName = "Saved")
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    NiaLoadingWheel(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .testTag("forYou:loading"),
        contentDesc = stringResource(id = R.string.feature_bookmarks_api_loading),
    )
}

@Composable
private fun BookmarksGrid(
    feedState: NewsFeedUiState,
    removeFromBookmarks: (String) -> Unit,
    onNewsResourceViewed: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    onNoteClick: (String, String) -> Unit,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onRemoveSelected: () -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollableState = rememberLazyStaggeredGridState()
    TrackScrollJank(scrollableState = scrollableState, stateName = "bookmarks:grid")

    // Extra bottom padding so cards don't hide behind the remove bar
    val bottomBarVisible = selectedIds.isNotEmpty()
    val gridBottomPadding = if (bottomBarVisible) 80.dp else 16.dp

    // Outer Box: hosts the column layout + the floating remove bar overlay.
    // AnimatedVisibility lives here (not inside the Column) to avoid resolving
    // to ColumnScope.AnimatedVisibility from an outer implicit receiver.
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            BookmarksTopBar(
                feedState = feedState,
                selectedIds = selectedIds,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(300.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = gridBottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalItemSpacing = 24.dp,
                    state = scrollableState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("bookmarks:feed"),
                ) {
                    when (feedState) {
                        Loading -> Unit
                        is Success -> {
                            items(
                                items = feedState.feed,
                                key = { it.id },
                                contentType = { "bookmarkFeedItem" },
                            ) { userNewsResource ->
                                val context = LocalContext.current
                                val analyticsHelper = LocalAnalyticsHelper.current
                                val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
                                val isChecked = userNewsResource.id in selectedIds

                                Box(modifier = Modifier.animateItem()) {
                                    Column {
                                        NewsResourceCardExpanded(
                                            userNewsResource = userNewsResource,
                                            isBookmarked = userNewsResource.isSaved,
                                            onClick = {
                                                analyticsHelper.logNewsResourceOpened(
                                                    newsResourceId = userNewsResource.id,
                                                )
                                                launchCustomChromeTab(
                                                    context,
                                                    Uri.parse(userNewsResource.url),
                                                    backgroundColor,
                                                )
                                                onNewsResourceViewed(userNewsResource.id)
                                            },
                                            hasBeenViewed = userNewsResource.hasBeenViewed,
                                            onToggleBookmark = { removeFromBookmarks(userNewsResource.id) },
                                            onTopicClick = onTopicClick,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                        userNewsResource.bookmarkNote?.let { note ->
                                            NoteRow(
                                                note = note,
                                                onClick = { onNoteClick(userNewsResource.id, note) },
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                            )
                                        }
                                    }
                                    // Checkbox always visible in the top-start corner of each card
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onToggleSelection(userNewsResource.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 12.dp, top = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                    }
                }

                val itemsAvailable = when (feedState) {
                    Loading -> 1
                    is Success -> feedState.feed.size
                }
                val scrollbarState = scrollableState.scrollbarState(itemsAvailable = itemsAvailable)
                scrollableState.DraggableScrollbar(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 2.dp)
                        .align(Alignment.CenterEnd),
                    state = scrollbarState,
                    orientation = Orientation.Vertical,
                    onThumbMoved = scrollableState.rememberDraggableScroller(
                        itemsAvailable = itemsAvailable,
                    ),
                )
            }
        }

        // Floating remove bar — AnimatedVisibility at Box level avoids ColumnScope ambiguity
        AnimatedVisibility(
            visible = bottomBarVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            RemoveSelectionBar(
                selectedCount = selectedIds.size,
                onRemoveSelected = onRemoveSelected,
            )
        }
    }
}

@Composable
private fun BookmarksTopBar(
    feedState: NewsFeedUiState,
    selectedIds: Set<String>,
    onSelectAll: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.feature_bookmarks_api_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (feedState is Success && feedState.feed.isNotEmpty()) {
            val allSelected = selectedIds.size == feedState.feed.size &&
                feedState.feed.all { it.id in selectedIds }
            TextButton(
                onClick = {
                    if (allSelected) onClearSelection()
                    else onSelectAll(feedState.feed.map { it.id })
                },
            ) {
                Text(
                    text = if (allSelected) {
                        stringResource(R.string.feature_bookmarks_api_deselect_all)
                    } else {
                        stringResource(R.string.feature_bookmarks_api_select_all)
                    },
                )
            }
        }
    }
}

@Composable
private fun RemoveSelectionBar(
    selectedCount: Int,
    onRemoveSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Button(
            onClick = onRemoveSelected,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = stringResource(R.string.feature_bookmarks_api_remove_selected, selectedCount),
            )
        }
    }
}

@Composable
private fun NoteRow(
    note: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = note,
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .testTag("bookmarks:empty"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconTint = LocalTintTheme.current.iconTint
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.feature_bookmarks_api_mg_empty_bookmarks),
            colorFilter = if (iconTint != Color.Unspecified) ColorFilter.tint(iconTint) else null,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_error),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun LoadingStatePreview() {
    NiaTheme {
        LoadingState()
    }
}

@Preview
@Composable
private fun BookmarksGridPreview(
    @PreviewParameter(UserNewsResourcePreviewParameterProvider::class)
    userNewsResources: List<UserNewsResource>,
) {
    NiaTheme {
        BookmarksGrid(
            feedState = Success(userNewsResources),
            removeFromBookmarks = {},
            onNewsResourceViewed = {},
            onTopicClick = {},
            onNoteClick = { _, _ -> },
            selectedIds = emptySet(),
            onToggleSelection = {},
            onRemoveSelected = {},
            onSelectAll = {},
            onClearSelection = {},
        )
    }
}

@Preview
@Composable
private fun BookmarksGridWithSelectionPreview(
    @PreviewParameter(UserNewsResourcePreviewParameterProvider::class)
    userNewsResources: List<UserNewsResource>,
) {
    NiaTheme {
        BookmarksGrid(
            feedState = Success(userNewsResources),
            removeFromBookmarks = {},
            onNewsResourceViewed = {},
            onTopicClick = {},
            onNoteClick = { _, _ -> },
            selectedIds = setOf(userNewsResources.first().id),
            onToggleSelection = {},
            onRemoveSelected = {},
            onSelectAll = {},
            onClearSelection = {},
        )
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    NiaTheme {
        EmptyState()
    }
}
