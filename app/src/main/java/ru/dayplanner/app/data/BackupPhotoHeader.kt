package ru.dayplanner.app.data

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import java.io.File

/** Ограничения относятся к исходному изображению, а не к размеру уменьшенного превью. */
internal object BackupPhotoLimits {
    const val MAX_FILE_BYTES = 128L * 1024 * 1024
    const val MAX_SIDE = 65_535
    const val MAX_PIXELS = 268_435_456L // 16 384 × 16 384; вмещает и снимки камер на 200 Мп.

    fun isImageMime(mime: String?): Boolean = mime != null &&
        mime.startsWith("image/", ignoreCase = true) && mime.substringAfter('/').isNotBlank()

    fun accepts(bytes: Long, width: Int, height: Int, mime: String?): Boolean =
        bytes in 1L..MAX_FILE_BYTES && width in 1..MAX_SIDE && height in 1..MAX_SIDE &&
            width.toLong() * height <= MAX_PIXELS && isImageMime(mime)
}

// Останавливает запасной декодер прямо в обработчике заголовка, до декодирования пикселей.
private class ValidHeader : RuntimeException()

internal fun validateBackupPhoto(file: File) {
    val bytes = file.length()
    require(bytes in 1L..BackupPhotoLimits.MAX_FILE_BYTES) { "Недопустимый размер файла фото" }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (BackupPhotoLimits.isImageMime(bounds.outMimeType) && bounds.outWidth > 0 && bounds.outHeight > 0) {
        require(BackupPhotoLimits.accepts(bytes, bounds.outWidth, bounds.outHeight, bounds.outMimeType)) {
            "Размер изображения превышает предел резервной копии"
        }
        return
    }

    // Просмотрщик использует ImageDecoder. Не отбрасываем поддерживаемый им формат
    // из-за особенностей BitmapFactory на конкретной версии Android. Имя .jpg не учитываем.
    try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { _, info, _ ->
            require(BackupPhotoLimits.accepts(bytes, info.size.width, info.size.height, info.mimeType)) {
                "Вложение не является поддерживаемым изображением допустимого размера"
            }
            throw ValidHeader()
        }
    } catch (_: ValidHeader) {
        return
    }
    error("Декодер не предоставил заголовок изображения")
}
