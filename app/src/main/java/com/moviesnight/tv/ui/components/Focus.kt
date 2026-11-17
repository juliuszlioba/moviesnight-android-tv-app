package com.moviesnight.tv.ui.components

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

/**
 * Lazy items are only attached after the first layout pass, so a focus request made
 * right after composition can miss. Retry for a few frames before giving up.
 */
suspend fun FocusRequester.requestFocusWhenReady(maxFrames: Int = 10): Boolean {
    repeat(maxFrames) {
        if (runCatching { requestFocus() }.isSuccess) return true
        withFrameNanos { }
    }
    return false
}
