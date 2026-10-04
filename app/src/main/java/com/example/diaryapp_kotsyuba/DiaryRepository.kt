package com.example.diaryapp_kotsyuba

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException

class DiaryRepository(context: Context) {

    // Приватная папка приложения
    private val directory = context.applicationContext.filesDir

    // Формат имени: время_заголовок.txt
    private val fileNamePattern = Regex("\\d+_.+\\.txt")

    // Загрузка записей из папки.
    // Позже будем вызывать её один раз — при создании ViewModel.
    @Synchronized
    fun loadEntries(): List<DiaryEntry> {
        val files = directory.listFiles()
            ?: throw IOException("Не удалось прочитать папку дневника")

        return files
            .filter { it.isFile && fileNamePattern.matches(it.name) }
            .mapNotNull { file ->
                val createdAt = file.name
                    .substringBefore("_")
                    .toLongOrNull()
                    ?: return@mapNotNull null

                val content = AtomicFile(file)
                    .readFully()
                    .toString(Charsets.UTF_8)

                // Первая строка файла — заголовок,
                // всё остальное — текст записи.
                DiaryEntry(
                    fileName = file.name,
                    title = content.substringBefore("\n"),
                    text = content.substringAfter("\n", ""),
                    createdAt = createdAt
                )
            }
            .sortedByDescending { it.createdAt }
    }

    // Создание новой записи или изменение существующей
    @Synchronized
    fun saveEntry(
        title: String,
        text: String,
        existingEntry: DiaryEntry? = null
    ): DiaryEntry {
        require(text.isNotBlank()) {
            "Введите текст записи"
        }

        // Заголовок сохраняем одной строкой
        val normalizedTitle = title
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()

        // Оставляем в имени файла буквы, цифры, _ и -
        val safeTitle = normalizedTitle
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(40)
            .ifBlank { "entry" }

        var createdAt = existingEntry?.createdAt
            ?: System.currentTimeMillis()

        // При редактировании сохраняем прежнее имя файла
        var fileName = existingEntry?.fileName
            ?: "${createdAt}_${safeTitle}.txt"

        // Защита от совпадения имён новых записей
        if (existingEntry == null) {
            while (File(directory, fileName).exists()) {
                createdAt++
                fileName = "${createdAt}_${safeTitle}.txt"
            }
        }

        require(fileNamePattern.matches(fileName)) {
            "Некорректное имя файла"
        }

        val file = File(directory, fileName)
        val atomicFile = AtomicFile(file)
        val content = "$normalizedTitle\n$text"

        // Сначала записываем новые данные,
        // затем подтверждаем завершение записи.
        val output = atomicFile.startWrite()

        try {
            output.write(content.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (error: Exception) {
            atomicFile.failWrite(output)
            throw error
        }

        return DiaryEntry(
            fileName = fileName,
            title = normalizedTitle,
            text = text,
            createdAt = createdAt
        )
    }

    // Удаление одной записи по имени файла
    @Synchronized
    fun deleteEntry(fileName: String) {
        require(fileNamePattern.matches(fileName)) {
            "Некорректное имя файла"
        }

        val file = File(directory, fileName)
        AtomicFile(file).delete()

        if (file.exists()) {
            throw IOException("Не удалось удалить запись")
        }
    }
}