package ru.konspekt.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * One place for haptics, so every meaningful tap feels the same across the app.
 *
 * [HapticFeedbackType.TextHandleMove] is intentionally not used: on a lot of
 * devices it maps to a barely noticeable (or disabled) tick. LongPress and
 * Confirm are handled by the system haptic engine and are felt reliably while
 * still respecting the user's system settings.
 */
class AppHaptics(private val feedback: HapticFeedback) {
    fun tap() {
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun confirm() {
        feedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }
}

@Composable
fun rememberHaptics(): AppHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { AppHaptics(feedback) }
}
