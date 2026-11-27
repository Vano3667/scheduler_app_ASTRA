package ru.dayplanner.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.dayplanner.app.ui.calendar.CalendarScreen
import ru.dayplanner.app.ui.day.DayScreen
import ru.dayplanner.app.ui.editor.EditorScreen
import ru.dayplanner.app.ui.settings.SettingsScreen
import ru.dayplanner.app.ui.stats.StatsScreen
import ru.dayplanner.app.ui.task.TaskScreen
import ru.dayplanner.app.ui.templates.TemplateEditorScreen
import ru.dayplanner.app.ui.templates.TemplatesScreen

object Routes {
    const val DAY = "day"
    const val CALENDAR = "calendar/{day}"
    const val EDITOR = "editor/{taskId}/{day}"
    const val TASK = "task/{taskId}/{day}"
    const val TEMPLATES = "templates"
    const val TEMPLATE_EDITOR = "template/{id}"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    fun calendar(day: Long) = "calendar/$day"
    fun editor(taskId: Long, day: Long) = "editor/$taskId/$day"
    fun task(taskId: Long, day: Long) = "task/$taskId/$day"
    fun templateEditor(id: Long) = "template/$id"
}

/** Закрывает экран, только если он сейчас верхний (защита от двойного нажатия). */
private fun NavController.popIfCurrent(route: String) {
    if (currentDestination?.route == route) popBackStack()
}

private fun NavController.go(route: String) {
    navigate(route) { launchSingleTop = true }
}

@Composable
fun PlannerNavHost() {
    val nav = rememberNavController()
    // День, который должен показать экран дня (после выбора даты в календаре).
    var pendingDay by rememberSaveable { mutableStateOf<Long?>(null) }

    NavHost(
        navController = nav,
        startDestination = Routes.DAY,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 10 } },
    ) {
        composable(Routes.DAY) {
            DayScreen(
                pendingDay = pendingDay,
                onPendingDayConsumed = { pendingDay = null },
                onOpenCalendar = { day -> nav.go(Routes.calendar(day)) },
                onAdd = { day -> nav.go(Routes.editor(0, day)) },
                onOpenTask = { taskId, day -> nav.go(Routes.task(taskId, day)) },
                onTemplates = { nav.go(Routes.TEMPLATES) },
                onStats = { nav.go(Routes.STATS) },
                onSettings = { nav.go(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.CALENDAR,
            arguments = listOf(navArgument("day") { type = NavType.LongType }),
        ) { entry ->
            CalendarScreen(
                initialDay = entry.arguments?.getLong("day") ?: 0L,
                onBack = { nav.popIfCurrent(Routes.CALENDAR) },
                onPickDay = { day ->
                    pendingDay = day
                    nav.popBackStack(Routes.DAY, inclusive = false)
                },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType },
                navArgument("day") { type = NavType.LongType },
            ),
        ) { entry ->
            EditorScreen(
                taskId = entry.arguments?.getLong("taskId") ?: 0L,
                day = entry.arguments?.getLong("day") ?: 0L,
                onClose = { nav.popIfCurrent(Routes.EDITOR) },
            )
        }
        composable(
            Routes.TASK,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType },
                navArgument("day") { type = NavType.LongType },
            ),
        ) { entry ->
            val taskId = entry.arguments?.getLong("taskId") ?: 0L
            val day = entry.arguments?.getLong("day") ?: 0L
            TaskScreen(
                taskId = taskId,
                day = day,
                onBack = { nav.popIfCurrent(Routes.TASK) },
                onGone = { nav.popBackStack(Routes.DAY, inclusive = false) },
                onEdit = { nav.go(Routes.editor(taskId, day)) },
            )
        }
        composable(Routes.TEMPLATES) {
            TemplatesScreen(
                onBack = { nav.popIfCurrent(Routes.TEMPLATES) },
                onEdit = { id -> nav.go(Routes.templateEditor(id)) },
            )
        }
        composable(
            Routes.TEMPLATE_EDITOR,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            TemplateEditorScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                onBack = { nav.popIfCurrent(Routes.TEMPLATE_EDITOR) },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { nav.popIfCurrent(Routes.STATS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popIfCurrent(Routes.SETTINGS) })
        }
    }
}
