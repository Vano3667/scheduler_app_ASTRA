package ru.dayplanner.app.ui.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.ui.components.SectionCard
import ru.dayplanner.app.ui.photos.PhotoImage
import ru.dayplanner.app.ui.photos.PhotoViewer

/** Фото на странице дела; по нажатию открываются во весь экран. */
@Composable
fun TaskPhotos(taskId: Long) {
    val container = LocalContext.current.container
    val flow = remember(taskId) { container.repository.observePhotoNames(taskId) }
    val photos by flow.collectAsStateWithLifecycle(initialValue = emptyList())
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    if (photos.isNotEmpty()) {
        SectionCard(title = stringResource(R.string.photos_section)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(photos, key = { _, name -> name }) { index, name ->
                    PhotoImage(
                        name = name,
                        maxSizePx = 480,
                        modifier = Modifier
                            .size(132.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { viewerIndex = index },
                    )
                }
            }
        }
    }

    val index = viewerIndex
    if (index != null && photos.isNotEmpty()) {
        PhotoViewer(photos = photos, startIndex = index, onDismiss = { viewerIndex = null })
    }
}
