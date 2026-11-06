package org.ragbot.replica.bridge

import android.content.Context
import java.io.File
import org.ragbot.replica.BuildConfig

/*
 * The debug setting and the fixed development identity of the bridge.
 * Read by MainActivity at launch. The address comes from the build (BuildConfig.BRIDGE_BACKEND) and
 * is always a local development address, never a bank's. The loop stays off without the flag file.
 */
internal object BridgeConfig {
    /** Also the session ID: the contract's resume call names only a session. */
    const val DEVICE_ID = "replica-dev"

    const val FLAG_FILE = "bridge.enabled"

    /** True only in a debug build, after `adb shell touch` of the flag file. */
    fun enabled(context: Context) =
        BuildConfig.DEBUG && File(context.getExternalFilesDir(null), FLAG_FILE).exists()
}
