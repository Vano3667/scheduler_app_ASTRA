package ru.dayplanner.app

import android.app.Application
import android.content.Context

class PlannerApp : Application() {
    lateinit var container: AppContainer
        private set

    private var freshStart = true

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.onAppStart()
    }

    /** true только один раз после запуска процесса: экран дня должен открыться на сегодняшней дате. */
    fun consumeFreshStart(): Boolean {
        val value = freshStart
        freshStart = false
        return value
    }
}

val Context.container: AppContainer
    get() = (applicationContext as PlannerApp).container
