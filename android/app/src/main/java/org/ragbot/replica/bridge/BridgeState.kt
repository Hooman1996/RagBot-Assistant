package org.ragbot.replica.bridge

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/*
 * What ReplicaApp publishes for the bridge: the current page and the Home tab.
 * Written by ReplicaApp after each composition, read by ComposeControls.
 * The page's real state stays in ReplicaApp. Nothing here can change the screen.
 */
internal object BridgeState {
    @Volatile var screen = "unknown"
    @Volatile var homeTab = "HOME"
    @Volatile var activity = WeakReference<ComponentActivity>(null)
}
