package ru.dayplanner.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dayplanner.app.data.AppDatabase
import ru.dayplanner.app.data.PhotoStore
import ru.dayplanner.app.data.PlannerRepository
import ru.dayplanner.app.data.SettingsStore
import ru.dayplanner.app.signals.SignalScheduler
import ru.dayplanner.app.widget.WidgetUpdater

/** Простейший контейнер зависимостей: один экземпляр на процесс. */
class AppContainer(private val app: Context) {

    private val errorHandler = CoroutineExceptionHandler { _, error ->
        Log.e("Planner", "Ошибка фоновой операции", error)
    }

    /** Область для операций, которые должны завершиться, даже если экран уже закрыт. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + errorHandler)

    val db: AppDatabase by lazy { AppDatabase.create(app) }
    val photoStore: PhotoStore = PhotoStore(app)
    val settings: SettingsStore = SettingsStore(app)
    val repository: PlannerRepository by lazy {
        PlannerRepository(db, photoStore, appScope, ::notifyChanged)
    }

    /** Вызывается после любого изменения дел: пересчитать сигналы и обновить виджет. */
    fun notifyChanged() {
        SignalScheduler.requestReschedule(app)
        WidgetUpdater.requestUpdate(app)
    }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = appScope.launch(block = block)

    fun onAppStart() {
        launch {
            withContext(Dispatchers.IO) {
                try {
                    val referenced = db.dao().getAllPhotos().mapTo(HashSet()) { it.fileName }
                    photoStore.cleanupOrphans(referenced, 24L * 60 * 60 * 1000)
                } catch (e: Exception) {
                    Log.w("Planner", "Не удалось очистить лишние фото", e)
                }
            }
        }
        notifyChanged()
    }
}
