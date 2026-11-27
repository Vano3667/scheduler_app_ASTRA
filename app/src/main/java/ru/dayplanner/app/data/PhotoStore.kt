package ru.dayplanner.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Фото дел во внутреннем хранилище приложения (каталог files/photos). */
class PhotoStore(private val context: Context) {

    val dir: File
        get() = File(context.filesDir, "photos").also { it.mkdirs() }

    fun file(name: String): File = File(dir, name)

    fun newName(): String =
        "p_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(8) + ".jpg"

    fun delete(name: String) {
        try {
            file(name).delete()
        } catch (_: Exception) {
        }
    }

    /** Копирует картинку по Uri во внутреннее хранилище. Возвращает имя файла или null при ошибке. */
    suspend fun importFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val name = newName()
        val target = file(name)
        try {
            val input = context.contentResolver.openInputStream(uri)
            if (input == null) {
                null
            } else {
                input.use { source -> target.outputStream().use { out -> source.copyTo(out) } }
                if (target.length() > 0) {
                    name
                } else {
                    target.delete()
                    null
                }
            }
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    /** Удаляет файлы, на которые нет ссылок в базе и которые старше [minAgeMs]. */
    fun cleanupOrphans(referenced: Set<String>, minAgeMs: Long) {
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.name !in referenced && now - f.lastModified() > minAgeMs) f.delete()
        }
    }
}
