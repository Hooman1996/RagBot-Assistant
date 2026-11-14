package org.ragbot.replica.devtools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/*
 * Debug only: offers Android a full Persian physical-keyboard layout (res/raw), so the replica
 * viewer's simulated keyboard can type every Persian letter on the emulator. Android reads the
 * layout from this receiver's metadata; the receiver itself never does anything.
 */
class KeyboardLayouts : BroadcastReceiver() {
    /** Never called with work to do: Android only reads the metadata. */
    override fun onReceive(context: Context, intent: Intent) = Unit
}
