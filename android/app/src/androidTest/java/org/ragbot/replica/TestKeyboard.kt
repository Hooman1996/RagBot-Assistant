package org.ragbot.replica

import android.app.Activity
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Closes the keyboard and waits for the page to take its full height again. A tap during that
 * animation can miss, or fail to inject, so call this before a tap that follows typing.
 */
internal fun ComposeTestRule.closeKeyboard(activity: Activity) {
    val decor = activity.window.decorView
    runOnIdle { WindowCompat.getInsetsController(activity.window, decor).hide(WindowInsetsCompat.Type.ime()) }
    waitUntil(5_000) {
        ViewCompat.getRootWindowInsets(decor)?.let {
            !it.isVisible(WindowInsetsCompat.Type.ime()) &&
                it.getInsets(WindowInsetsCompat.Type.ime()).bottom == 0
        } == true
    }
    waitForIdle()
}
