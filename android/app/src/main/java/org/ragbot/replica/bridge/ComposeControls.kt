package org.ragbot.replica.bridge

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.annotation.RequiresApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/*
 * Reads and drives the real Compose screens through their semantics tree.
 * Called by Actions and Observer on the poller thread. Every read and action runs on the main
 * thread. click runs the control's own onClick and setText its own onValueChange, so all state and
 * validation stay the screen's. It never reads a password field's text.
 * The bindings to an app's real screens live here, so this file is what an app team replaces.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class ComposeControls : Controls {
    private val main = Handler(Looper.getMainLooper())

    /** Runs [work] on the main thread and waits for its answer. */
    private fun <T> onMain(work: () -> T): T {
        var answer: Result<T>? = null
        val done = CountDownLatch(1)
        main.post {
            answer = runCatching(work)
            done.countDown()
        }
        check(done.await(WAIT_SECONDS, TimeUnit.SECONDS)) { "The main thread did not answer" }
        return checkNotNull(answer).getOrThrow()
    }

    /** The Compose roots of the app's windows, oldest first. A dialog has its own window. */
    private fun roots() = WindowInspector.getGlobalWindowViews().flatMap(::composeRoots)

    /** Finds the Compose root inside one window's view tree. */
    private fun composeRoots(view: View): List<RootForTest> {
        if (view is RootForTest) return listOf(view)
        if (view !is ViewGroup) return emptyList()
        return (0 until view.childCount).flatMap { composeRoots(view.getChildAt(it)) }
    }

    /** Every node of the top window: a user cannot reach behind an open dialog. */
    private fun topNodes(): List<SemanticsNode> {
        val top = roots().lastOrNull() ?: return emptyList()
        return nodes(top.semanticsOwner.rootSemanticsNode)
    }

    /** A node and everything under it. */
    private fun nodes(node: SemanticsNode): List<SemanticsNode> =
        listOf(node) + node.children.flatMap(::nodes)

    /** A node's test tag, if it has one. */
    private fun tagOf(node: SemanticsNode) = node.config.getOrNull(SemanticsProperties.TestTag)

    /** The top window's node with this tag. */
    private fun node(tag: String) = topNodes().firstOrNull { tagOf(it) == tag }

    /** Reads a node into a [Control]. A password field's text is never read. */
    private fun describe(tag: String, config: SemanticsConfiguration): Control {
        val password = SemanticsProperties.Password in config
        val edited = config.getOrNull(SemanticsProperties.EditableText)?.text
        val shown = config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ")
        return Control(
            tag,
            if (password) "" else edited ?: shown,
            SemanticsProperties.Disabled !in config,
            SemanticsActions.OnClick in config,
            SemanticsActions.SetText in config,
            config.getOrNull(SemanticsProperties.Selected) == true,
            password,
            SemanticsProperties.Error in config,
        )
    }

    /** The published page, plus what only the windows can show: a dialog or a spinner. */
    override fun state(): ScreenState = onMain {
        val windows = roots()
        val modal =
            if (windows.size < 2) null
            else nodes(windows.last().semanticsOwner.rootSemanticsNode)
                .firstNotNullOfOrNull(::tagOf) ?: "dialog"
        val loading =
            topNodes().any {
                it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) ==
                    ProgressBarRangeInfo.Indeterminate
            }
        ScreenState(BridgeState.screen, BridgeState.homeTab, modal, loading)
    }

    /** See [Controls.find]. */
    override fun find(tag: String): Control? = onMain {
        node(tag)?.let { describe(tag, it.config) }
    }

    /** See [Controls.withPrefix]. */
    override fun withPrefix(prefix: String): List<Control> = onMain {
        topNodes().mapNotNull { node ->
            tagOf(node)?.takeIf { it.startsWith(prefix) }?.let { describe(it, node.config) }
        }
    }

    /** Runs the node's own onClick. Actions has already checked it is allowed and enabled. */
    override fun click(tag: String) = onMain {
        val config = checkNotNull(node(tag)) { "No control $tag" }.config
        config[SemanticsActions.OnClick].action?.invoke()
        Unit
    }

    /** Runs the field's own onValueChange, and refuses a password field. */
    override fun setText(tag: String, text: String) = onMain {
        val config = checkNotNull(node(tag)) { "No control $tag" }.config
        check(SemanticsProperties.Password !in config) { "Password field $tag" }
        config[SemanticsActions.SetText].action?.invoke(AnnotatedString(text))
        Unit
    }

    /** Goes through the same back dispatcher as the system Back button. */
    override fun back() = onMain {
        BridgeState.activity.get()?.onBackPressedDispatcher?.onBackPressed()
        Unit
    }

    private companion object {
        const val WAIT_SECONDS = 5L
    }
}
