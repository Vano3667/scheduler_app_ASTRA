package ru.dayplanner.app.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.ui.components.SectionCard
import ru.dayplanner.app.ui.photos.PhotoImage

/**
 * Раздел «Фото» в редакторе дела: системный выбор фото и системная камера.
 * Разрешения на хранилище и камеру не нужны; файлы сразу копируются во внутреннее хранилище.
 */
@Composable
fun PhotoSection(photos: List<String>, onChange: (List<String>) -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val currentPhotos by rememberUpdatedState(photos)
    val currentOnChange by rememberUpdatedState(onChange)
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        if (uris.isNotEmpty()) {
            container.launch {
                val names = uris.mapNotNull { container.photoStore.importFromUri(it) }
                if (names.size < uris.size) errorRes = R.string.photo_import_failed
                if (names.isNotEmpty()) currentOnChange(currentPhotos + names)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val name = pendingCamera
        pendingCamera = null
        if (name != null) {
            if (saved && container.photoStore.file(name).length() > 0) {
                currentOnChange(currentPhotos + name)
            } else {
                container.photoStore.delete(name)
            }
        }
    }

    SectionCard(title = stringResource(R.string.photos_section)) {
        if (photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photos, key = { it }) { name ->
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .animateItem(),
                    ) {
                        PhotoImage(
                            name = name,
                            maxSizePx = 320,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.small),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onChange(photos - name) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.photo_remove),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    errorRes = null
                    try {
                        pickLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    } catch (e: Exception) {
                        errorRes = R.string.photo_import_failed
                    }
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.photo_gallery), maxLines = 1)
            }
            OutlinedButton(
                onClick = {
                    errorRes = null
                    val name = container.photoStore.newName()
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".files",
                            container.photoStore.file(name),
                        )
                        pendingCamera = name
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        pendingCamera = null
                        errorRes = R.string.photo_no_camera
                    }
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.photo_camera), maxLines = 1)
            }
        }
        val error = errorRes
        if (error != null) {
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
