package io.github.veitkramerschoeggl.trainingtracker.domain

import java.time.LocalTime

/** Typed time of day ("hhmm" digits only) for entries on another day than today. */
object TimeInput {
    const val MAX_DIGITS = 4

    /** Keeps only the digits that can still form a time, at most four. */
    fun sanitize(text: String): String = text.filter(Char::isDigit).take(MAX_DIGITS)

    /** "1" -> "1_:__", "" -> "__:__", "1230" -> "12:30" */
    fun mask(digits: String): String {
        val padded = digits.padEnd(MAX_DIGITS, '_')
        return padded.substring(0, 2) + ":" + padded.substring(2)
    }

    /** The time for exactly four digits, null when incomplete or not a real time (e.g. "2561"). */
    fun parse(digits: String): LocalTime? {
        if (digits.length != MAX_DIGITS || !digits.all(Char::isDigit)) return null
        val hour = digits.substring(0, 2).toInt()
        val minute = digits.substring(2).toInt()
        return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
    }
}
