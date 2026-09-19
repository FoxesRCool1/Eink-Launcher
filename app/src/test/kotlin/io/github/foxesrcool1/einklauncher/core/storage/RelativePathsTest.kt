package io.github.foxesrcool1.einklauncher.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativePathsTest {

    @Test
    fun `a plain path is kept`() {
        assertEquals("notes/ideas/plan.md", RelativePaths.normalise("notes/ideas/plan.md"))
    }

    @Test
    fun `empty segments and stray separators are removed`() {
        assertEquals("notes/plan.md", RelativePaths.normalise("notes//plan.md"))
        assertEquals("notes/plan.md", RelativePaths.normalise("notes/plan.md/"))
        assertEquals("", RelativePaths.normalise(""))
    }

    @Test
    fun `a path made only of separators is refused, because it is absolute`() {
        // "/" and "///" both name the root of the device, not a place inside
        // the data folder. Treating them as the data folder itself would be a
        // quiet way to let a caller work on the wrong thing.
        assertFalse(RelativePaths.isSafe("/"))
        assertFalse(RelativePaths.isSafe("///"))
    }

    @Test
    fun `a backslash counts as a separator`() {
        assertEquals("notes/plan.md", RelativePaths.normalise("notes\\plan.md"))
    }

    @Test
    fun `every way out of the folder is refused`() {
        val attempts = listOf(
            "../secrets",
            "notes/../../secrets",
            "/etc/passwd",
            "C:/Windows/system32",
            "notes/./plan.md",
            "notes\\..\\..\\secrets",
            " notes/plan.md",
            "notes/ plan.md",
        )
        attempts.forEach { attempt ->
            assertFalse("should be refused: $attempt", RelativePaths.isSafe(attempt))
        }
    }

    @Test(expected = UnsafePathException::class)
    fun `normalise throws on a path that leaves the folder`() {
        RelativePaths.normalise("../outside")
    }

    @Test
    fun `join puts two parts together`() {
        assertEquals("notes/ideas", RelativePaths.join("notes", "ideas"))
        assertEquals("notes", RelativePaths.join("notes", ""))
        assertEquals("ideas", RelativePaths.join("", "ideas"))
        assertEquals("", RelativePaths.join("", ""))
    }

    @Test
    fun `parent name and extension are read off a path`() {
        assertEquals("notes/ideas", RelativePaths.parentOf("notes/ideas/plan.md"))
        assertEquals("", RelativePaths.parentOf("plan.md"))
        assertEquals("plan.md", RelativePaths.nameOf("notes/ideas/plan.md"))
        assertEquals("md", RelativePaths.extensionOf("notes/plan.md"))
        assertEquals("", RelativePaths.extensionOf("notes/plan"))
    }

    @Test
    fun `a file whose name holds dots is not mistaken for a way out`() {
        assertTrue(RelativePaths.isSafe("notes/my...notes.md"))
        assertEquals("notes/my...notes.md", RelativePaths.normalise("notes/my...notes.md"))
    }
}
