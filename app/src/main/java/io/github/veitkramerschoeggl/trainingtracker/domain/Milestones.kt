package io.github.veitkramerschoeggl.trainingtracker.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** A reached multiple of [Milestones.STEP] reps. */
data class Milestone(
    val value: Int,
    val date: LocalDate,
    /** "2 Monaten und 5 Tagen" — time since the first training day. */
    val sinceStart: String,
    /** Days since the previous milestone; null for the first one. */
    val daysSincePrevious: Int?,
)

data class MilestoneProgress(
    /** Newest first. */
    val reached: List<Milestone>,
    val next: Int,
    val remaining: Int,
    val percent: Int,
    /** Average reps per day over the last 28 days. */
    val ratePerDay: Double,
    /** Expected day of reaching [next]; null without entries in the last 28 days. */
    val forecast: LocalDate?,
)

/** "Meilensteine" of the prototype (klimmzug-tracker-v11.html): every 1000 reps. */
object Milestones {
    const val STEP = 1000

    fun compute(days: List<TrainingDay>, today: LocalDate): MilestoneProgress {
        var cumulative = 0
        var nextValue = STEP
        val reached = mutableListOf<Milestone>()
        val startDate = days.firstOrNull()?.date
        for (day in days) {
            cumulative += day.total
            while (cumulative >= nextValue) {
                reached += Milestone(
                    value = nextValue,
                    date = day.date,
                    sinceStart = duration(startDate!!, day.date),
                    daysSincePrevious = reached.lastOrNull()?.let { Stats.daysBetween(it.date, day.date) },
                )
                nextValue += STEP
            }
        }
        val remaining = nextValue - cumulative
        val percent = Math.round((cumulative - (nextValue - STEP)).toDouble() / STEP * 100).toInt().coerceIn(0, 100)
        val since = today.minusDays(27)
        val rate = days.filter { it.date >= since }.sumOf { it.total } / 28.0
        return MilestoneProgress(
            reached = reached.asReversed(),
            next = nextValue,
            remaining = remaining,
            percent = percent,
            ratePerDay = rate,
            forecast = if (rate > 0) today.plusDays(ceil(remaining / rate).toLong()) else null,
        )
    }

    /**
     * "2 Monaten und 5 Tagen" / "1 Monat und 1 Tag" / "12 Tagen" — whole months from [from], then
     * the remaining days. Adding months overflows like JavaScript's `setMonth` (31 Jan + 1 month =
     * 3 Mar) so the result matches the prototype.
     */
    fun duration(from: LocalDate, to: LocalDate): String {
        var months = (to.year - from.year) * 12 + (to.monthValue - from.monthValue)
        var anchor = jsAddMonths(from, months)
        if (anchor > to) {
            months--
            anchor = jsAddMonths(from, months)
        }
        val days = ChronoUnit.DAYS.between(anchor, to).toInt()
        val monthText = "$months " + if (months == 1) "Monat" else "Monaten"
        val dayText = "$days " + if (days == 1) "Tag" else "Tagen"
        return if (months > 0) "$monthText und $dayText" else dayText
    }

    private fun jsAddMonths(date: LocalDate, months: Int): LocalDate =
        date.withDayOfMonth(1).plusMonths(months.toLong()).plusDays(date.dayOfMonth - 1L)

    /** JavaScript's `toFixed(1)` with a decimal comma: "7,4". */
    fun oneDecimal(value: Double): String =
        BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
}
