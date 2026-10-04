package com.example.diaryapp_kotsyuba

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    viewModel: DiaryViewModel,
    onNewEntry: () -> Unit,
    onOpenEntry: (DiaryEntry) -> Unit
) {
    val canInteract = viewModel.isReady && !viewModel.isBusy

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Мой дневник")
                }
            )
        },
        floatingActionButton = {
            if (canInteract) {
                ExtendedFloatingActionButton(
                    onClick = onNewEntry
                ) {
                    Text("+ Новая запись")
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Первоначальная загрузка файлов
                viewModel.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Не удалось загрузить записи
                !viewModel.isReady -> {
                    Text(
                        text = "Не удалось загрузить дневник.\n" +
                                "Закройте приложение и откройте снова.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                // Записей пока нет
                viewModel.entries.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "У вас пока нет записей",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Нажмите +, чтобы создать первую",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Список сохранённых записей
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = viewModel.entries,
                            key = { it.fileName }
                        ) { entry ->
                            DiaryEntryCard(
                                entry = entry,
                                enabled = canInteract,
                                onOpen = {
                                    onOpenEntry(entry)
                                },
                                onDelete = {
                                    viewModel.deleteEntry(entry)
                                }
                            )
                        }
                    }
                }
            }

            // Индикатор сохранения или удаления
            if (viewModel.isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryEntryCard(
    entry: DiaryEntry,
    enabled: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Переводим время создания в понятную дату
    val dateText = remember(entry.createdAt) {
        SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        ).format(Date(entry.createdAt))
    }

    // Показываем первые 40 символов текста
    val previewText = entry.text
        .replace('\n', ' ')
        .replace('\r', ' ')
        .take(40) + if (entry.text.length > 40) "…" else ""

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled,
                    onClick = onOpen,
                    onLongClick = {
                        menuExpanded = true
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title.ifBlank { "Без заголовка" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Меню можно открыть и обычной кнопкой
                    TextButton(
                        enabled = enabled,
                        onClick = {
                            menuExpanded = true
                        }
                    ) {
                        Text("Меню")
                    }
                }

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Контекстное меню записи
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("Удалить")
                },
                enabled = enabled,
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}