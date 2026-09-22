package io.github.foxesrcool1.margin.core.reading

import java.time.LocalDate

/**
 * How long the user read, per day.
 *
 * `annotations/reading-log.csv`, one line per day and book:
 *
 * ```
 * date,book,seconds
 * 2026-09-20,walden-482113,1260
 * ```
 *
 * Lines are only ever added, so a crash in the middle of a write can lose at
 * most the last session. A line that cannot be read is skipped.
 */
object ReadingLog {

    const val HEADER = "date,book,seconds"

    data class Entry(val date: LocalDate, val bookId: String, val seconds: Long)

    fun parse(text: String): List<Entry> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != HEADER }
            .mapNotNull { line ->
                val parts = line.split(',')
                if (parts.size != 3) return@mapNotNull null
                val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
                val seconds = parts[2].toLongOrNull()?.takeIf { it in 0..86_400 } ?: return@mapNotNull null
                Entry(date, parts[1], seconds)
            }
            .toList()

    fun line(entry: Entry): String =
        // A comma in a book id would break the line. Book ids never hold one,
        // but this file is too small a place to find that out the hard way.
        "${entry.date},${entry.bookId.replace(',', '-')},${entry.seconds}"

    fun secondsOn(entries: List<Entry>, date: LocalDate): Long =
        entries.filter { it.date == date }.sumOf { it.seconds }

    /** 0 to 1. A goal of zero minutes means no goal, and reads as not reached. */
    fun goalFraction(secondsToday: Long, goalMinutes: Int): Float =
        if (goalMinutes <= 0) 0f else (secondsToday / (goalMinutes * 60f)).coerceIn(0f, 1f)
}

/**
 * Counts reading time for one open book.
 *
 * It counts only while the reader is on screen, and it stops counting when
 * no page was turned for [idleLimitSeconds]: a tablet left open on the table
 * is not reading. Time is handed in by the caller, so a test needs no clock.
 */
class ReadingTimer(private val idleLimitSeconds: Long = 300) {

    private var runningSince: Long? = null
    private var lastActivity: Long = 0
    private var counted: Long = 0

    fun resume(nowSeconds: Long) {
        if (runningSince == null) {
            runningSince = nowSeconds
            lastActivity = nowSeconds
        }
    }

    /** A page turn, a highlight, anything that shows someone is there. */
    fun activity(nowSeconds: Long) {
        val since = runningSince ?: return
        if (nowSeconds - lastActivity > idleLimitSeconds) {
            // The gap was a break. Count up to the limit and start again.
            counted += (lastActivity + idleLimitSeconds - since).coerceAtLeast(0)
            runningSince = nowSeconds
        }
        lastActivity = nowSeconds
    }

    fun pause(nowSeconds: Long) {
        val since = runningSince ?: return
        val end = minOf(nowSeconds, lastActivity + idleLimitSeconds)
        counted += (end - since).coerceAtLeast(0)
        runningSince = null
    }

    /** Seconds counted so far and not yet taken. Taking them sets the count back to zero. */
    fun take(): Long = counted.also { counted = 0 }

    fun peek(nowSeconds: Long): Long {
        val since = runningSince ?: return counted
        val end = minOf(nowSeconds, lastActivity + idleLimitSeconds)
        return counted + (end - since).coerceAtLeast(0)
    }
}
