package io.github.foxesrcool1.einklauncher.core.habits

import java.time.LocalDate

/** One habit the user is tracking. */
data class Habit(
    val id: String,
    val name: String,
    val createdOn: LocalDate,
    val archived: Boolean = false,
)

/** One day on which one habit was done. */
data class HabitMark(
    val habitId: String,
    val date: LocalDate,
)

/** The whole of `habits/habits.json`. */
data class HabitsDocument(
    val habits: List<Habit>,
    val dayBoundaryHour: Int,
) {
    fun active(): List<Habit> = habits.filter { !it.archived }
}
