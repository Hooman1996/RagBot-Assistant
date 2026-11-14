package org.ragbot.replica.bridge

import android.os.Build
import android.util.Log
import java.io.IOException

/*
 * The pull loop: report the app's state, run any command, post its result, repeat.
 * Started once by MainActivity, and only when BridgeConfig.enabled is true.
 * One background thread runs every command, so commands never overlap. It does no work of its
 * own on the screen: Executor does that.
 */
internal class CommandPoller(private val executor: Executor, private val backend: Backend) {
    /** One round: runs commands until the backend has no next one. */
    fun pollOnce() {
        val seen = executor.observer.observe()
        lastObservation = seen
        var command = backend.resume(seen)
        while (command != null) {
            val result = executor.execute(command)
            lastObservation = result.observation
            command = backend.postResult(result)
        }
    }

    /** Polls forever on the background thread. */
    private fun loop() {
        while (true) {
            try {
                pollOnce()
            } catch (error: IOException) {
                // The backend is a development server: it may simply not be running yet.
                Log.d(TAG, "Backend not reachable: ${error.message}")
            }
            Thread.sleep(POLL_MS)
        }
    }

    companion object {
        private const val TAG = "CommandPoller"
        private const val POLL_MS = 500L
        private var started = false

        /** The loop's latest observation, which the assistant panel sends with each message. */
        @Volatile var lastObservation: Observation? = null

        /** Starts the loop once per process. A rotation must not start a second one. */
        fun start() {
            if (started || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
            started = true
            val executor = Executor(ComposeControls(), BridgeConfig.DEVICE_ID)
            val poller = CommandPoller(executor, BackendClient())
            Thread(poller::loop, "command-poller").apply { isDaemon = true }.start()
        }
    }
}
