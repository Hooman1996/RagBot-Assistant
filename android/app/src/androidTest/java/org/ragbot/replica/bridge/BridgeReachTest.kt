package org.ragbot.replica.bridge

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.ragbot.replica.HomeDestinations
import org.ragbot.replica.MainActivity

/** Asks the executor for every screen: each one is reached, or stops for a stated reason. */
@RunWith(AndroidJUnit4::class)
class BridgeReachTest {
    @Test
    fun everyScreenIsReachedOrStopsForAStatedReason() {
        val rig = BridgeRig(patienceMs = 800)
        val broken = mutableListOf<String>()
        for (screen in HomeDestinations.implemented.sorted()) {
            ActivityScenario.launch(MainActivity::class.java).use {
                val result = rig.navigate(screen)
                val at = result.observation.screen
                Log.i("BridgeReach", "$screen\t${result.outcome}\t$at\t${result.needs}")
                val reached = result.outcome == "succeeded" && at == screen
                if (!reached && result.outcome != "blocked")
                    broken += "$screen: ${result.outcome} ${result.error_code} at $at"
            }
        }
        assertEquals(broken.joinToString("\n"), 0, broken.size)
    }
}
