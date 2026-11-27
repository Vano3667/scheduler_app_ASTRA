package ru.dayplanner.app.ui.editor

import androidx.compose.runtime.Composable

// Типовые дела появятся на этапе 4.

/** Кнопка «Выбрать из типовых» (в начале формы). */
@Composable
fun TemplatePickButton(state: EditorState, onChange: (EditorState) -> Unit) {
}

/** Переключатель «Сохранить как типовое» (в конце формы). */
@Composable
fun TemplateSection(state: EditorState, onChange: (EditorState) -> Unit) {
}
