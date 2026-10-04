package com.example.diaryapp_kotsyuba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import com.example.diaryapp_kotsyuba.ui.theme.DiaryApp_KotsyubaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Фабрика передаёт Application в нашу ViewModel
        val factory =
            ViewModelProvider.AndroidViewModelFactory
                .getInstance(application)

        // Получаем ViewModel, связанную с этой Activity
        val diaryViewModel = ViewModelProvider(
            this,
            factory
        )[DiaryViewModel::class.java]

        setContent {
            DiaryApp_KotsyubaTheme {
                DiaryApp(viewModel = diaryViewModel)
            }
        }
    }
}

@Composable
private fun DiaryApp(viewModel: DiaryViewModel) {

    // Открыт ли экран создания или редактирования
    var editorOpened by rememberSaveable {
        mutableStateOf(false)
    }

    // Имя файла выбранной записи.
    // null означает создание новой записи.
    var selectedFileName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val selectedEntry = viewModel.entries.firstOrNull {
        it.fileName == selectedFileName
    }

    // Если выбранная запись больше не существует,
    // возвращаемся к списку после загрузки.
    LaunchedEffect(
        editorOpened,
        viewModel.isReady,
        selectedFileName,
        selectedEntry?.fileName
    ) {
        if (
            editorOpened &&
            viewModel.isReady &&
            selectedFileName != null &&
            selectedEntry == null
        ) {
            editorOpened = false
            selectedFileName = null
        }
    }

    val canShowEditor =
        editorOpened &&
                viewModel.isReady &&
                (selectedFileName == null || selectedEntry != null)

    if (canShowEditor) {
        DiaryEditorScreen(
            viewModel = viewModel,
            entry = selectedEntry,
            onBack = {
                editorOpened = false
                selectedFileName = null
            }
        )
    } else {
        DiaryListScreen(
            viewModel = viewModel,
            onNewEntry = {
                selectedFileName = null
                editorOpened = true
            },
            onOpenEntry = { entry ->
                selectedFileName = entry.fileName
                editorOpened = true
            }
        )
    }

    // Показываем ошибки чтения, сохранения или удаления
    viewModel.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                viewModel.clearError()
            },
            title = {
                Text("Ошибка")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearError()
                    }
                ) {
                    Text("Понятно")
                }
            }
        )
    }
}