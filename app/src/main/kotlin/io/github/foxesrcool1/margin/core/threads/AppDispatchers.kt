package io.github.foxesrcool1.margin.core.threads

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Where the screens do their slow work: reading a folder, writing a note.
 *
 * On the tablet this is `Dispatchers.IO` and nothing more. It is a variable
 * for the sake of the screenshot tests. There, a screen that comes back from a
 * background thread goes on with its work on that thread, and writes its state
 * in the middle of a layout pass on the main thread. Every so often the screen
 * then never hears of the new state, and the picture shows an empty list. That
 * cannot happen on the tablet, where a screen always goes on with its work on
 * the main thread. A test sets [io] to a dispatcher that runs the work in
 * place, and the race is gone.
 *
 * Screens use `withContext(AppDispatchers.io)`, never `Dispatchers.IO`.
 */
object AppDispatchers {

    @Volatile
    var io: CoroutineDispatcher = Dispatchers.IO
}
