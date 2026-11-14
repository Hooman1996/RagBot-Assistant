package org.ragbot.replica.assistant

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/*
 * The assistant's floating bubble: drag it anywhere, tap it to open or close the chat box. It shows
 * the task state while there is one. Like the panel it sits in the app's own window, and it never
 * covers more than its own small size; a tap on it only opens or closes the box.
 */
@Composable
internal fun Bubble(state: AssistantPanelState) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val margin = with(density) { 16.dp.toPx() }
        val reach = with(density) { 120.dp.toPx() } // keeps the whole bubble on screen
        val x = if (state.bubbleX < 0) margin else state.bubbleX
        val y = if (state.bubbleY < 0) height * .72f else state.bubbleY
        Surface(
            { toggle(state) },
            // Measured from the screen's left edge whatever the layout direction, as a finger drags it.
            Modifier.align(AbsoluteAlignment.TopLeft)
                .absoluteOffset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .pointerInput(width, height) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        state.bubbleX = ((if (state.bubbleX < 0) margin else state.bubbleX) + amount.x)
                            .coerceIn(0f, (width - reach).coerceAtLeast(0f))
                        state.bubbleY = ((if (state.bubbleY < 0) height * .72f else state.bubbleY) + amount.y)
                            .coerceIn(0f, (height - reach / 2).coerceAtLeast(0f))
                    }
                }
                .testTag("assistant-bubble"),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF145D32),
            contentColor = Color.White,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("دستیار", style = MaterialTheme.typography.labelLarge)
                STATE_WORDS[state.history.shownState]?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** Opens or closes the chat box; the user's own choice ends any stepping aside. */
private fun toggle(state: AssistantPanelState) {
    state.open = !state.open
    state.autoClosed = false
}

/** Closes the box once the agent moves the app to another screen, and opens it again when it ends. */
internal fun stepAside(state: AssistantPanelState) {
    val history = state.history
    val moved = AssistantClient.lastObservation()?.screen.let { it != null && it != state.startScreen }
    if (history.busy && moved && state.open) {
        state.open = false
        state.autoClosed = true
    } else if (!history.busy && state.autoClosed) {
        state.open = true
        state.autoClosed = false
    }
}
