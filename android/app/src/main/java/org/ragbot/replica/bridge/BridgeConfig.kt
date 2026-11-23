package org.ragbot.replica.bridge

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.ragbot.replica.BuildConfig

/*
 * The debug setting and the fixed development identity of the bridge.
 * Read by MainActivity at launch. The address comes from the build (BuildConfig.BRIDGE_BACKEND) and
 * is always a local development address, never a bank's. The loop stays off without the flag file.
 */
internal object BridgeConfig {
    const val DEVICE_ID = "replica-dev"
    const val SESSION_ID = BuildConfig.BRIDGE_SESSION

    const val FLAG_FILE = "bridge.enabled"

    /** The panel and bridge loop identify the same live session on every request. */
    fun connection(path: String): HttpURLConnection =
        (URL(BuildConfig.BRIDGE_BACKEND + path).openConnection() as HttpURLConnection).apply {
            setRequestProperty("X-Assistant-Session", SESSION_ID)
        }

    /** True only in a debug build, after `adb shell touch` of the flag file. */
    fun enabled(context: Context) =
        BuildConfig.DEBUG && File(context.getExternalFilesDir(null), FLAG_FILE).exists()
}
