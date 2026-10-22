package ru.konspekt.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Translate

enum class SubjectIcon {
    FUNCTIONS, SCIENCE, TERMINAL, LANDMARK, LANGUAGE, BIOTECH;

    val vector: ImageVector
        get() = when (this) {
            FUNCTIONS -> Icons.Rounded.Functions
            SCIENCE -> Icons.Rounded.Science
            TERMINAL -> Icons.Rounded.Terminal
            LANDMARK -> Icons.Rounded.AccountBalance
            LANGUAGE -> Icons.Rounded.Translate
            BIOTECH -> Icons.Rounded.Biotech
        }
}

enum class SubjectTone {
    SAGE, SKY, LILAC, AMBER, CLAY;

    fun container(dark: Boolean): Color = when (this) {
        SAGE -> if (dark) Color(0xFF243B28) else Color(0xFFDCEDD6)
        SKY -> if (dark) Color(0xFF1F3445) else Color(0xFFDCE9F5)
        LILAC -> if (dark) Color(0xFF312A40) else Color(0xFFE8E1F3)
        AMBER -> if (dark) Color(0xFF3B301D) else Color(0xFFF4E7C9)
        CLAY -> if (dark) Color(0xFF3C2A23) else Color(0xFFF3DFD5)
    }

    fun onContainer(dark: Boolean): Color = when (this) {
        SAGE -> if (dark) Color(0xFFC8E5C3) else Color(0xFF1E3821)
        SKY -> if (dark) Color(0xFFC7DEF2) else Color(0xFF17334A)
        LILAC -> if (dark) Color(0xFFDCD1EF) else Color(0xFF302544)
        AMBER -> if (dark) Color(0xFFEAD8B3) else Color(0xFF44320F)
        CLAY -> if (dark) Color(0xFFEED4C7) else Color(0xFF44251A)
    }

    fun accent(dark: Boolean): Color = when (this) {
        SAGE -> if (dark) Color(0xFF8FBE93) else Color(0xFF4C7853)
        SKY -> if (dark) Color(0xFF8BB8DE) else Color(0xFF4C7394)
        LILAC -> if (dark) Color(0xFFB8A6D8) else Color(0xFF6A588D)
        AMBER -> if (dark) Color(0xFFD8BC82) else Color(0xFF86672C)
        CLAY -> if (dark) Color(0xFFD5A995) else Color(0xFF8D5E4B)
    }
}

data class Subject(
    val id: String,
    val name: String,
    val shortName: String,
    val teacher: String,
    val nextClass: String,
    val tone: SubjectTone,
    val icon: SubjectIcon
)

enum class SectionKind {
    TEXT, DEFINITION, THEOREM, EXAMPLE, SUMMARY
}

data class NoteSection(
    val title: String,
    val kind: SectionKind = SectionKind.TEXT,
    val body: String = "",
    val formula: String? = null,
    val formulaLabel: String? = null,
    val bullets: List<String> = emptyList()
)

data class SkippedFragment(
    val time: String,
    val quote: String,
    val reason: String
)

data class Note(
    val id: String,
    val subjectId: String,
    val title: String,
    val subtitle: String = "",
    val summary: String,
    val createdAt: Long,
    val dateLabel: String,
    val timeLabel: String,
    val durationMinutes: Int,
    val sections: List<NoteSection>,
    val keyTerms: List<String> = emptyList(),
    val skipped: List<SkippedFragment> = emptyList(),
    val latexSource: String = "",
    val wordCount: Int = 0
)

data class ScheduleItem(
    val time: String,
    val endTime: String,
    val subjectId: String,
    val topic: String,
    val isNow: Boolean = false
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AiMode { LOCAL, DEEPSEEK }

enum class Palette { SAGE, SKY, LAVENDER, CLAY }

data class UserSettings(
    val userName: String = "Студент",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: Palette = Palette.SAGE,
    val dynamicColor: Boolean = true,
    val filterTangents: Boolean = true,
    val aiMode: AiMode = AiMode.LOCAL
)

data class NoteGenerationResult(
    val noteId: String,
    val warning: String? = null
)
