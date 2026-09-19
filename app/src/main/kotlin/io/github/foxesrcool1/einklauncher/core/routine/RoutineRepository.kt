package io.github.foxesrcool1.einklauncher.core.routine

import io.github.foxesrcool1.einklauncher.core.habits.HabitLogFile
import io.github.foxesrcool1.einklauncher.core.habits.HabitMark
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.storage.DataRepository
import io.github.foxesrcool1.einklauncher.core.storage.StorageLayout
import java.time.LocalDate

private const val TAG = "RoutineRepository"

/** One routine item as Today and the Journal tab see it. */
data class RoutineStatus(
    val item: RoutineItem,
    val doneToday: Boolean,
)

/**
 * The routine, on top of the data folder.
 *
 * The list lives in `habits/routine.json`. What was done on which day lives in
 * `habits/routine-log.csv`, in the same shape as the habit log and read by the
 * same code. Two files rather than one, so ticking an item off never rewrites
 * the list itself.
 */
class RoutineRepository(private val data: DataRepository) {

    private val listPath = "${StorageLayout.HABITS}/routine.json"
    private val logPath = "${StorageLayout.HABITS}/routine-log.csv"

    fun load(): RoutineDocument {
        val text = data.store.readText(listPath) ?: return RoutineDocument(emptyList())
        val parsed = RoutineFile.parse(text)
        if (parsed == null) {
            AppLog.w(TAG, "routine.json could not be read. Starting from an empty list.")
            return RoutineDocument(emptyList())
        }
        return parsed
    }

    fun save(document: RoutineDocument): Boolean =
        data.store.writeText(listPath, RoutineFile.serialise(document))

    fun add(label: String, target: String = ""): RoutineDocument {
        val current = load()
        val id = RoutineFile.idFor(label, current.items.map { it.id }.toSet())
        val next = RoutineDocument(current.items + RoutineItem(id, label.trim(), target))
        save(next)
        AppLog.i(TAG, "Added routine item $id")
        return next
    }

    fun remove(id: String): RoutineDocument = load().without(id).also { save(it) }

    fun move(id: String, by: Int): RoutineDocument = load().move(id, by).also { save(it) }

    private fun marks(): List<HabitMark> =
        data.store.readText(logPath)?.let { HabitLogFile.parse(it) } ?: emptyList()

    /** Ticks an item off for [date], or takes the tick away. Returns the new state. */
    fun toggle(id: String, date: LocalDate): Boolean {
        val current = marks().toMutableList()
        val mark = HabitMark(id, date)

        val nowDone = if (current.remove(mark)) {
            false
        } else {
            current += mark
            true
        }

        data.store.writeText(logPath, HabitLogFile.serialise(current))
        return nowDone
    }

    fun statuses(date: LocalDate): List<RoutineStatus> {
        val done = marks().filter { it.date == date }.map { it.habitId }.toSet()
        return load().items.map { item -> RoutineStatus(item, done.contains(item.id)) }
    }

    /**
     * The next thing to do today, or null when the day is finished.
     *
     * "Next" is the first item in the user's own order that is not ticked off.
     * Not the nearest in time, and not the one the app thinks is important.
     * The order is the user's plan for the day.
     */
    fun next(date: LocalDate): RoutineStatus? =
        statuses(date).firstOrNull { !it.doneToday }
}
