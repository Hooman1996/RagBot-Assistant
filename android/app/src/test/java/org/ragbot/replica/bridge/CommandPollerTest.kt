package org.ragbot.replica.bridge

import org.junit.Assert.*
import org.junit.Test

class CommandPollerTest {
    private class ScriptedBackend(commands: List<Command>) : Backend {
        val queue = ArrayDeque(commands)
        val results = mutableListOf<CommandResult>()
        var resumed = 0

        override fun resume(observation: Observation): Command? {
            resumed += 1
            return queue.removeFirstOrNull()
        }

        override fun postResult(result: CommandResult): Command? {
            results += result
            return queue.removeFirstOrNull()
        }
    }

    private val executor = Executor(FakeControls(), "device-1", { 1_000 }, patienceMs = 0)

    @Test
    fun runsEachCommandAndPostsItsResult() {
        val version = executor.observer.observe().observation_version
        val backend =
            ScriptedBackend(
                listOf(
                    command("observe", "one", observation = version),
                    command("observe", "two", observation = version),
                )
            )
        CommandPoller(executor, backend).pollOnce()
        assertEquals(listOf("one", "two"), backend.results.map { it.command_id })
        assertEquals(listOf("succeeded", "succeeded"), backend.results.map { it.outcome })
        assertEquals(1, backend.resumed)
    }

    @Test
    fun anEmptyReplyEndsTheRound() {
        val backend = ScriptedBackend(emptyList())
        CommandPoller(executor, backend).pollOnce()
        assertTrue(backend.results.isEmpty())
    }
}
