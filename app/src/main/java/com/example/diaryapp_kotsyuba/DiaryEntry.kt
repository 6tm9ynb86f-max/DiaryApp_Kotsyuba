package com.example.diaryapp_kotsyuba

// Данные одной записи дневника
data class DiaryEntry(
    // Имя файла, в котором сохранена запись
    val fileName: String,

    // Заголовок записи — может быть пустым
    val title: String,

    // Полный текст записи
    val text: String,

    // Дата создания в миллисекундах
    val createdAt: Long
)