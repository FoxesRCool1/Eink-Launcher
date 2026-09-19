package io.github.foxesrcool1.einklauncher.core.log

/** One line in the log file. Kept small so it is cheap to hold in memory. */
data class LogLine(
    val timeMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
) {
    fun format(): String = buildString {
        append(LogFormat.timestamp(timeMillis))
        append(' ')
        append(level.short)
        append(' ')
        append(tag)
        append(": ")
        append(message)
    }
}

enum class LogLevel(val short: Char) {
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
}
