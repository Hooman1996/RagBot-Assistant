package org.ragbot.replica.bridge

import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.ragbot.replica.BuildConfig

/** Rule 3: the bridge talks only to a local development address, and only in debug builds. */
class BridgeConfigTest {
    @Test
    fun theBackendIsALocalAddress() {
        assertTrue(BuildConfig.BRIDGE_BACKEND, BuildConfig.BRIDGE_BACKEND.startsWith("http://127.0.0.1:"))
    }

    @Test
    fun onlyTheDebugBuildMayUseTheNetwork() {
        val main = File("src/main/AndroidManifest.xml").readText()
        assertFalse("android.permission.INTERNET" in main)
        assertTrue("android:usesCleartextTraffic=\"false\"" in main)
        val network = File("src/debug/res/xml/bridge_network.xml").readText()
        val domains = Regex(">([^<]+)</domain>").findAll(network).map { it.groupValues[1] }.toList()
        assertEquals(listOf("127.0.0.1"), domains)
    }
}
