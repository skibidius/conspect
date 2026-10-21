package ru.konspekt.app.util

fun pluralRu(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

fun notesCountText(count: Int): String =
    "$count ${pluralRu(count, "конспект", "конспекта", "конспектов")}"

fun subjectsCountText(count: Int): String =
    "$count ${pluralRu(count, "предмет", "предмета", "предметов")}"

fun fragmentsHiddenText(count: Int): String =
    "Скрыто фрагментов: $count"
