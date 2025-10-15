package com.ioannapergamali.mysmartroute.view.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * Απλός αναπαραστάτης ενός φακέλου πολυμέσων.
 */
data class GalleryFolder(
    val id: Long,
    val name: String,
    val previewUri: Uri? = null,
)

/**
 * Εσωτερική κατάσταση για το slide bar φακέλων.
 */
class GallerySlideBarState internal constructor(
    initialFolders: List<GalleryFolder>,
    initialSelectionId: Long?,
) {
    var folders by mutableStateOf(initialFolders)
        private set

    var selectedFolderId by mutableStateOf(initialSelectionId)
        private set

    val selectedFolder: GalleryFolder?
        get() = folders.firstOrNull { it.id == selectedFolderId }

    fun updateFolders(newFolders: List<GalleryFolder>) {
        folders = newFolders
        if (selectedFolderId == null || newFolders.none { it.id == selectedFolderId }) {
            selectedFolderId = newFolders.firstOrNull()?.id
        }
    }

    fun select(folder: GalleryFolder) {
        selectedFolderId = folder.id
    }
}

@Composable
fun rememberGallerySlideBarState(
    folders: List<GalleryFolder>,
    initialSelection: GalleryFolder? = folders.firstOrNull(),
): GallerySlideBarState {
    val state = remember {
        GallerySlideBarState(
            initialFolders = folders,
            initialSelectionId = initialSelection?.id,
        )
    }

    LaunchedEffect(folders) {
        state.updateFolders(folders)
    }

    return state
}

/**
 * Συνιστώσα Jetpack Compose που επιτρέπει την επιλογή φακέλου εικόνων.
 */
@Composable
fun GallerySlideBar(
    folders: List<GalleryFolder>,
    onFolderSelected: (GalleryFolder) -> Unit,
    modifier: Modifier = Modifier,
    initialSelection: GalleryFolder? = folders.firstOrNull(),
) {
    val state = rememberGallerySlideBarState(
        folders = folders,
        initialSelection = initialSelection,
    )

    GallerySlideBar(
        state = state,
        onFolderSelected = onFolderSelected,
        modifier = modifier,
    )
}

/**
 * Παραλλαγή που λαμβάνει ήδη απομνημονευμένη κατάσταση ώστε να επαναχρησιμοποιείται σε σύνθετες διατάξεις.
 */
@Composable
fun GallerySlideBar(
    state: GallerySlideBarState,
    onFolderSelected: (GalleryFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(vertical = 12.dp),
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                items(state.folders, key = { it.id }) { folder ->
                    val selected = folder.id == state.selectedFolderId
                    AssistChip(
                        onClick = {
                            state.select(folder)
                            onFolderSelected(folder)
                        },
                        label = { Text(text = folder.name) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            labelColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private class FakeGalleryFoldersProvider : PreviewParameterProvider<List<GalleryFolder>> {
    override val values: Sequence<List<GalleryFolder>> = sequenceOf(
        listOf(
            GalleryFolder(id = 1L, name = "Κάμερα"),
            GalleryFolder(id = 2L, name = "Λήψεις"),
            GalleryFolder(id = 3L, name = "Screenshots"),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun GallerySlideBarPreview(
    @PreviewParameter(FakeGalleryFoldersProvider::class) folders: List<GalleryFolder>,
) {
    MaterialTheme {
        GallerySlideBar(
            folders = folders,
            onFolderSelected = {},
        )
    }
}
