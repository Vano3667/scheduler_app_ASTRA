package ru.dayplanner.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.dayplanner.app.domain.Completion
import ru.dayplanner.app.domain.Photo
import ru.dayplanner.app.domain.SkippedDay
import ru.dayplanner.app.domain.Task
import ru.dayplanner.app.domain.Template

@Database(
    entities = [Task::class, Photo::class, Completion::class, SkippedDay::class, Template::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PlannerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "planner.db").build()
    }
}
