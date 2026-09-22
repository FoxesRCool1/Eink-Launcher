package io.github.foxesrcool1.margin.core.ink

/**
 * The ink of one page while it is being edited, with undo and redo.
 *
 * Every change is "these strokes were added" or "these strokes were taken
 * away". Undo turns one into the other. That is all the eraser and the pen
 * ever do, so that is all this needs to know.
 *
 * Not thread safe. The canvas calls it from the main thread only. A save on
 * another thread takes [strokes], which is a fresh list each time.
 */
class InkPageEditor(initial: List<InkStroke> = emptyList()) {

    private sealed interface Change {
        class Added(val stroke: InkStroke) : Change

        /** Each stroke with the place it had, so undo can put it back under the right neighbours. */
        class Removed(val strokes: List<Pair<Int, InkStroke>>) : Change
    }

    private val current = ArrayList(initial)
    private val undoStack = ArrayDeque<Change>()
    private val redoStack = ArrayDeque<Change>()

    /** Goes up by one on every change. A save compares it to know whether there is anything new. */
    var revision: Long = 0
        private set

    /** A copy. Safe to hand to another thread. */
    val strokes: List<InkStroke> get() = ArrayList(current)

    val strokeCount: Int get() = current.size

    val canUndo: Boolean get() = undoStack.isNotEmpty()

    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun add(stroke: InkStroke) {
        current += stroke
        push(Change.Added(stroke))
    }

    /** Takes strokes away. Returns the ones that really were on the page. */
    fun remove(targets: Collection<InkStroke>): List<InkStroke> {
        if (targets.isEmpty()) return emptyList()
        // Identity, not equality: two strokes can hold the same points.
        val wanted = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<InkStroke, Boolean>())
        wanted.addAll(targets)
        val removed = ArrayList<Pair<Int, InkStroke>>()
        current.forEachIndexed { index, stroke -> if (stroke in wanted) removed += index to stroke }
        if (removed.isEmpty()) return emptyList()
        current.removeAll { it in wanted }
        push(Change.Removed(removed))
        return removed.map { it.second }
    }

    fun clear(): List<InkStroke> = remove(ArrayList(current))

    /** Returns the strokes whose pixels changed, so the canvas repaints only there. */
    fun undo(): List<InkStroke> {
        val change = undoStack.removeLastOrNull() ?: return emptyList()
        redoStack.addLast(change)
        revision++
        return revert(change)
    }

    fun redo(): List<InkStroke> {
        val change = redoStack.removeLastOrNull() ?: return emptyList()
        undoStack.addLast(change)
        revision++
        return apply(change)
    }

    private fun push(change: Change) {
        undoStack.addLast(change)
        if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        // A new change after an undo starts a new future.
        redoStack.clear()
        revision++
    }

    private fun apply(change: Change): List<InkStroke> = when (change) {
        is Change.Added -> {
            current += change.stroke
            listOf(change.stroke)
        }

        is Change.Removed -> {
            val gone = change.strokes.map { it.second }
            current.removeAll { stroke -> gone.any { it === stroke } }
            gone
        }
    }

    private fun revert(change: Change): List<InkStroke> = when (change) {
        is Change.Added -> {
            val index = current.indexOfLast { it === change.stroke }
            if (index >= 0) current.removeAt(index)
            listOf(change.stroke)
        }

        is Change.Removed -> {
            // Lowest place first, so every later place is still right.
            change.strokes.sortedBy { it.first }.forEach { (index, stroke) ->
                current.add(index.coerceIn(0, current.size), stroke)
            }
            change.strokes.map { it.second }
        }
    }

    companion object {
        const val MAX_UNDO = 200
    }
}
