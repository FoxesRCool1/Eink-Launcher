package io.github.foxesrcool1.einklauncher.core.habits

import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.storage.DataRepository
import io.github.foxesrcool1.einklauncher.core.storage.StorageLayout
import java.time.LocalDate

private const val TAG = "HabitsRepository"

/**
 * Habits on top of the data folder.
 *
 * Two files, both meant to be read by a person: `habits/habits.json` holds the
 * list, `habits/log.csv` holds one line per day done. Splitting them keeps the
 * log append friendly and keeps a year of ticks out of the file the user is
 * most likely to edit by hand.
 *
 * Step 9 builds the screens on top of this.
 */
class HabitsRepository(private val data: DataRepository) {

    fun load(): HabitsDocument {
        val text = data.store.readText(StorageLayout.habitsPath())
        if (text == null) return HabitsDocument(emptyList(), DayBoundary.DEFAULT_HOUR)

        val parsed = HabitsFile.parse(text)
        if (parsed == null) {
            AppLog.w(TAG, "habits.json could not be read. Starting from an empty list.")
            return HabitsDocument(emptyList(), DayBoundary.DEFAULT_HOUR)
        }
        return parsed
    }

    fun save(document: HabitsDocument): Boolean =
        data.store.writeText(StorageLayout.habitsPath(), HabitsFile.serialise(document))

    fun add(name: String, today: LocalDate): HabitsDocument {
        val current = load()
        val id = HabitsFile.idFor(name, current.habits.map { it.id }.toSet())
        val next = current.copy(habits = current.habits + Habit(id, name.trim(), today))
        save(next)
        AppLog.i(TAG, "Added habit $id")
        return next
    }

    fun rename(habitId: String, newName: String): HabitsDocument {
        val current = load()
        val next = current.copy(
            habits = current.habits.map { habit ->
                if (habit.id == habitId) habit.copy(name = newName.trim()) else habit
            },
        )
        save(next)
        return next
    }

    /**
     * Archives rather than deletes.
     *
     * The log keeps the days that were done. Deleting the habit would leave
     * lines in the log that point at nothing, and the user would lose the
     * record of a year of work to one tap.
     */
    fun setArchived(habitId: String, archived: Boolean): HabitsDocument {
        val current = load()
        val next = current.copy(
            habits = current.habits.map { habit ->
                if (habit.id == habitId) habit.copy(archived = archived) else habit
            },
        )
        save(next)
        AppLog.i(TAG, "Habit $habitId archived: $archived")
        return next
    }

    fun setDayBoundaryHour(hour: Int): HabitsDocument {
        require(hour in 0..23) { "The day boundary must be an hour of the day" }
        val next = load().copy(dayBoundaryHour = hour)
        save(next)
        return next
    }

    fun loadMarks(): List<HabitMark> {
        val text = data.store.readText(StorageLayout.habitLogPath()) ?: return emptyList()
        return HabitLogFile.parse(text)
    }

    fun saveMarks(marks: Collection<HabitMark>): Boolean =
        data.store.writeText(StorageLayout.habitLogPath(), HabitLogFile.serialise(marks))

    /** Marks a day done, or takes the mark off again. Returns the new state. */
    fun toggle(habitId: String, date: LocalDate): Boolean {
        val marks = loadMarks().toMutableList()
        val mark = HabitMark(habitId, date)

        val nowDone = if (marks.remove(mark)) {
            false
        } else {
            marks += mark
            true
        }

        saveMarks(marks)
        AppLog.i(TAG, "Habit $habitId on $date is now ${if (nowDone) "done" else "not done"}")
        return nowDone
    }

    fun datesFor(habitId: String): Set<LocalDate> = HabitLogFile.datesFor(loadMarks(), habitId)

    /** Everything one row of the Journal tab needs, worked out in one read. */
    fun summaries(today: LocalDate): List<HabitSummary> {
        val document = load()
        val marks = loadMarks()

        return document.active().map { habit ->
            val dates = HabitLogFile.datesFor(marks, habit.id)
            HabitSummary(
                habit = habit,
                doneToday = dates.contains(today),
                currentStreak = HabitStreaks.currentStreak(dates, today),
                longestStreak = HabitStreaks.longestStreak(dates),
                lastDays = HabitStreaks.lastDays(dates, today),
            )
        }
    }
}

/** One habit as a Journal row sees it. */
data class HabitSummary(
    val habit: Habit,
    val doneToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastDays: List<Boolean>,
)
