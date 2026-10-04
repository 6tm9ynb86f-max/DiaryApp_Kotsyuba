package com.example.diaryapp_kotsyuba

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiaryViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository = DiaryRepository(application)

    // Изменения этого списка автоматически замечает Compose
    private val _entries = mutableStateListOf<DiaryEntry>()

    val entries: List<DiaryEntry>
        get() = _entries

    // Идёт первоначальная загрузка
    var isLoading by mutableStateOf(true)
        private set

    // Первоначальная загрузка прошла успешно
    var isReady by mutableStateOf(false)
        private set

    // Выполняется сохранение или удаление
    var isBusy by mutableStateOf(false)
        private set

    // Сообщение об ошибке для интерфейса
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Выполняется один раз при создании ViewModel
    init {
        viewModelScope.launch {
            try {
                // Читаем файлы в фоновом потоке
                val loadedEntries = withContext(Dispatchers.IO) {
                    repository.loadEntries()
                }

                _entries.addAll(loadedEntries)
                isReady = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message
                    ?: "Не удалось загрузить записи"
            } finally {
                isLoading = false
            }
        }
    }

    // Создание или редактирование записи
    fun saveEntry(
        title: String,
        text: String,
        existingEntry: DiaryEntry? = null,
        onSaved: () -> Unit = {}
    ) {
        // Защита от повторного нажатия во время операции
        if (!isReady || isBusy) return

        if (text.isBlank()) {
            errorMessage = "Введите текст записи"
            return
        }

        isBusy = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val savedEntry = withContext(Dispatchers.IO) {
                    repository.saveEntry(
                        title = title,
                        text = text,
                        existingEntry = existingEntry
                    )
                }

                if (existingEntry == null) {
                    // Новую запись добавляем в начало списка
                    _entries.add(0, savedEntry)
                } else {
                    // Заменяем только отредактированную запись
                    val index = _entries.indexOfFirst {
                        it.fileName == existingEntry.fileName
                    }

                    if (index >= 0) {
                        _entries[index] = savedEntry
                    } else {
                        _entries.add(0, savedEntry)
                    }
                }

                // Сообщаем экрану об успешном сохранении
                onSaved()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message
                    ?: "Не удалось сохранить запись"
            } finally {
                isBusy = false
            }
        }
    }

    // Удаление записи
    fun deleteEntry(entry: DiaryEntry) {
        if (!isReady || isBusy) return

        isBusy = true
        errorMessage = null

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteEntry(entry.fileName)
                }

                // Убираем запись из текущего списка
                _entries.removeAll {
                    it.fileName == entry.fileName
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message
                    ?: "Не удалось удалить запись"
            } finally {
                isBusy = false
            }
        }
    }

    // Скрытие сообщения после его прочтения
    fun clearError() {
        errorMessage = null
    }
}