package ru.dayplanner.app.ui.photos

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.dayplanner.app.container
import java.io.File

/** Загрузка фото из внутреннего хранилища с уменьшением и небольшим кэшем в памяти. */
object PhotoLoader {
    private val cache = object : LruCache<String, Bitmap>(32 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun cached(name: String, maxSize: Int): Bitmap? = cache.get("$name@$maxSize")

    /** Вызывать не из главного потока. Поворот по EXIF учитывается автоматически. */
    fun load(file: File, maxSize: Int): Bitmap? {
        val key = file.name + "@" + maxSize
        cache.get(key)?.let { return it }
        return try {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = maxOf(w, h)
                if (largest > maxSize) {
                    val scale = maxSize.toFloat() / largest
                    decoder.setTargetSize(maxOf(1, (w * scale).toInt()), maxOf(1, (h * scale).toInt()))
                }
            }
            cache.put(key, bitmap)
            bitmap
        } catch (t: Throwable) {
            null
        }
    }
}

/** Картинка дела по имени файла. Пока грузится (или если файл повреждён) — значок-заглушка. */
@Composable
fun PhotoImage(
    name: String,
    maxSizePx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(
        initialValue = PhotoLoader.cached(name, maxSizePx)?.asImageBitmap(),
        name,
        maxSizePx,
    ) {
        if (value == null) {
            val file = context.container.photoStore.file(name)
            val loaded = withContext(Dispatchers.IO) { PhotoLoader.load(file, maxSizePx) }
            value = loaded?.asImageBitmap()
        }
    }
    Box(modifier = modifier.background(background), contentAlignment = Alignment.Center) {
        val shown = bitmap
        if (shown != null) {
            Image(
                bitmap = shown,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
