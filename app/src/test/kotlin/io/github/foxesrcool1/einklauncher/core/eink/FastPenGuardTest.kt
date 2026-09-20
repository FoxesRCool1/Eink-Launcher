package io.github.foxesrcool1.einklauncher.core.eink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FastPenGuardTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun guard() = FastPenGuard(temp.root.resolve("eink"))

    @Test
    fun aPathNobodyTriedMayBeTried() {
        assertEquals(FastPenGuard.State.Untried, guard().state(FastPenPath.Writing))
        assertTrue(guard().mayTry(FastPenPath.Writing))
    }

    @Test
    fun aCallThatCameBackIsFine() {
        val guard = guard()
        guard.markTrying(FastPenPath.Writing)
        guard.markFine(FastPenPath.Writing)
        assertEquals(emptyList<FastPenPath>(), guard().settleAfterStart())
        assertEquals(FastPenGuard.State.Fine, guard().state(FastPenPath.Writing))
    }

    @Test
    fun aCallThatNeverCameBackIsACrash() {
        guard().markTrying(FastPenPath.Writing)
        // The process died here. A new process starts and settles.
        val crashed = guard().settleAfterStart()
        assertEquals(listOf(FastPenPath.Writing), crashed)
        assertFalse(guard().mayTry(FastPenPath.Writing))
    }

    @Test
    fun oneCrashedPathDoesNotBlockTheOther() {
        guard().markTrying(FastPenPath.Writing)
        guard().settleAfterStart()
        assertTrue(guard().mayTry(FastPenPath.AutoDraw))
    }

    @Test
    fun aCrashStaysACrashAcrossStarts() {
        guard().markTrying(FastPenPath.AutoDraw)
        guard().settleAfterStart()
        assertEquals(emptyList<FastPenPath>(), guard().settleAfterStart())
        assertFalse(guard().mayTry(FastPenPath.AutoDraw))
    }

    @Test
    fun resetAllowsAnotherTry() {
        guard().markTrying(FastPenPath.Writing)
        guard().settleAfterStart()
        guard().reset(FastPenPath.Writing)
        assertTrue(guard().mayTry(FastPenPath.Writing))
    }

    @Test
    fun aDamagedFileCountsAsUntried() {
        val folder = temp.root.resolve("eink").apply { mkdirs() }
        folder.resolve("fastpen-writing.state").writeText("???")
        assertEquals(FastPenGuard.State.Untried, guard().state(FastPenPath.Writing))
    }
}
