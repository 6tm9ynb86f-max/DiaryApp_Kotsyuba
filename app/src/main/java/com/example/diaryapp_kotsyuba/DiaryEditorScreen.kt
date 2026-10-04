package com.example.diaryapp_kotsyuba

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditorScreen(
    viewModel: DiaryViewModel,
    entry: DiaryEntry?,
    onBack: () -> Unit
) {
    // Для новой записи поля пустые.
    // Для существующей — подставляем сохранённые значения.
    var title by rememberSaveable(entry?.fileName) {
        mutableStateOf(entry?.title.orEmpty())
    }

    var text by rememberSaveable(entry?.fileName) {
        mutableStateOf(entry?.text.orEmpty())
    }

    val keyboard = LocalSoftwareKeyboardController.current

    // Обработка системной кнопки «Назад»
    BackHandler {
        if (!viewModel.isBusy) {
            keyboard?.hide()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (entry == null) {
                            "Новая запись"
                        } else {
                            "Редактирование записи"
                        }
                    )
                },
                navigationIcon = {
                    TextButton(
                        enabled = !viewModel.isBusy,
                        onClick = {
                            keyboard?.hide()
                            onBack()
                        }
                    ) {
                        Text("Назад")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Необязательный заголовок
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                label = {
                    Text("Заголовок (необязательно)")
                },
                singleLine = true,
                enabled = !viewModel.isBusy,
                modifier = Modifier.fillMaxWidth()
            )

            // Большое многострочное поле
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                },
                label = {
                    Text("Текст записи")
                },
                placeholder = {
                    Text("Что произошло сегодня?")
                },
                minLines = 8,
                maxLines = 12,
                enabled = !viewModel.isBusy,
                modifier = Modifier.fillMaxWidth()
            )

            // Сохранение через ViewModel
            Button(
                onClick = {
                    viewModel.saveEntry(
                        title = title,
                        text = text,
                        existingEntry = entry,
                        onSaved = {
                            keyboard?.hide()
                            onBack()
                        }
                    )
                },
                enabled = viewModel.isReady &&
                        !viewModel.isBusy &&
                        text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        viewModel.isBusy -> "Сохраняем…"
                        entry == null -> "Сохранить запись"
                        else -> "Сохранить изменения"
                    }
                )
            }

            if (text.isBlank()) {
                Text(
                    text = "Введите текст, чтобы сохранить запись.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}