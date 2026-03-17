package com.dotmatrix.app.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Modifier extension that triggers haptic feedback on click.
 * Use this to replace plain `.clickable {}` on interactive cards and buttons.
 */
@Composable
fun Modifier.hapticClick(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val haptic = LocalHapticFeedback.current
    return this.clickable(enabled = enabled) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    }
}
