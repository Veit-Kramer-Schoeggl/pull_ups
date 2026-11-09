package io.github.veitkramerschoeggl.trainingtracker.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class Streak(val days: Int, val start: LocalDate, val end: LocalDate, val reps: Int)

data class MonthTotal(val month: YearMonth, val total: Int)

data class TrainingStats(
    val total: Int,
    val best: Int,
    val weekAverage: Int,
    val dayAverage: Int,
    val activeDays: Int,
    val bestWeek: TrainingWeek?,
    val longestStreak: Streak?,
    /** Longest gap between two training days, in days. */
    val longestPause: Int,
    val averageLast10Days: Int,
    val averageLast4Weeks: Int,
    val averagePerSetLast30Days: Int,
    /** Newest month first. */
    val months: List<MonthTotal>,
    val top3: List<TrainingDay>,
    val milestones: MilestoneProgress,
)

/** Statistics of one month for the detail sheet. */
data class MonthDetail(
    val month: YearMonth,
    val total: Int,
    val trainingDays: Int,
    val averagePerDay: Int,
    val best: Int,
    val sets: Int,
    val averagePerSet: Int,
    /** Newest first. */
    val days: List<TrainingDay>,
)

enum class ChartRange { ALL, THREE_MONTHS, ONE_MONTH, TWO_WEEKS }

/**
 * Statistics exactly as the HTML prototype (klimmzug-tracker-v11.html) computes them — averages are
 * rounded like JavaScript's Math.round and ties keep the earliest entry.
 */
object Stats {

    /** Groups sets (sorted by date, then recording time) into days, oldest first. */
    fun days(sets: List<TrainingSet>): List<TrainingDay> =
        sets.groupBy { it.date }
            .map { (date, daySets) -> TrainingDay(date, daySets) }
            .sortedBy { it.date }

    /** Groups days into ISO weeks, oldest first. Only weeks with training appear. */
    fun weeks(days: List<TrainingDay>): List<TrainingWeek> =
        days.groupBy { IsoWeek.of(it.date) }
            .map { (week, weekDays) -> TrainingWeek(week, weekDays.sortedBy { it.date }) }
            .sortedBy { it.week }

    fun compute(days: List<TrainingDay>, weeks: List<TrainingWeek>, today: LocalDate): TrainingStats {
        val total = days.sumOf { it.total }

        // Longest run of consecutive training days, and longest gap between two of them.
        var longest: Streak? = null
        var longestPause = 0
        var runStart = 0
        for (i in 1..days.size) {
            val gap = if (i < days.size) daysBetween(days[i - 1].date, days[i].date) else -1
            if (gap == 1) continue
            val run = days.subList(runStart, i)
            if (run.size > (longest?.days ?: 0)) {
                longest = Streak(run.size, run.first().date, run.last().date, run.sumOf { it.total })
            }
            if (gap > 1) longestPause = maxOf(longestPause, gap - 1)
            runStart = i
        }

        val cutoff = today.minusDays(30)
        val recentSets = days.filter { it.date >= cutoff }.flatMap { it.sets }
        val bestWeekTotal = weeks.maxOfOrNull { it.total } ?: 0

        return TrainingStats(
            total = total,
            best = days.maxOfOrNull { it.total } ?: 0,
            weekAverage = average(weeks.sumOf { it.total }, weeks.size),
            dayAverage = average(total, days.size),
            activeDays = days.size,
            bestWeek = weeks.firstOrNull { it.total == bestWeekTotal && bestWeekTotal > 0 },
            longestStreak = longest,
            longestPause = longestPause,
            averageLast10Days = days.takeLast(10).let { average(it.sumOf { d -> d.total }, it.size) },
            averageLast4Weeks = weeks.takeLast(4).let { average(it.sumOf { w -> w.total }, it.size) },
            averagePerSetLast30Days = average(recentSets.sumOf { it.reps }, recentSets.size),
            months = days.groupBy { YearMonth.from(it.date) }
                .map { (month, monthDays) -> MonthTotal(month, monthDays.sumOf { it.total }) }
                .sortedByDescending { it.month },
            top3 = days.sortedByDescending { it.total }.take(3),
            milestones = Milestones.compute(days, today),
        )
    }

    fun monthDetail(days: List<TrainingDay>, month: YearMonth): MonthDetail {
        val inMonth = days.filter { YearMonth.from(it.date) == month }
        val total = inMonth.sumOf { it.total }
        val setCount = inMonth.sumOf { it.sets.size }
        return MonthDetail(
            month = month,
            total = total,
            trainingDays = inMonth.size,
            averagePerDay = average(total, inMonth.size),
            best = inMonth.maxOfOrNull { it.total } ?: 0,
            sets = setCount,
            averagePerSet = average(total, setCount),
            days = inMonth.asReversed(),
        )
    }

    /**
     * Centered moving average shown in the day chart ("Ø 10 Tage (gleitend)"): each value is
     * averaged with its four neighbours on either side, fewer at the edges.
     */
    fun movingAverage(values: List<Int>): List<Double> =
        values.indices.map { i ->
            val window = values.subList(maxOf(0, i - 4), minOf(values.size - 1, i + 4) + 1)
            window.sum().toDouble() / window.size
        }

    /** The prototype draws the average only for daily values and from five points on. */
    const val MOVING_AVERAGE_MIN_POINTS = 5

    /** Values for the history chart: per day on the "Tage" tab, per week on the "Wochen" tab. */
    fun chartValues(
        days: List<TrainingDay>,
        weeks: List<TrainingWeek>,
        perWeek: Boolean,
        range: ChartRange,
        today: LocalDate,
    ): List<Int> =
        if (perWeek) {
            val count = when (range) {
                ChartRange.ALL -> weeks.size
                ChartRange.THREE_MONTHS -> 13
                ChartRange.ONE_MONTH -> 4
                ChartRange.TWO_WEEKS -> 2
            }
            weeks.takeLast(count).map { it.total }
        } else {
            val maxAge = when (range) {
                ChartRange.ALL -> null
                ChartRange.THREE_MONTHS -> 90
                ChartRange.ONE_MONTH -> 30
                ChartRange.TWO_WEEKS -> 14
            }
            days.filter { maxAge == null || daysBetween(it.date, today) <= maxAge }.map { it.total }
        }

    fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()

    /** Rounded average like JavaScript's Math.round (half up); 0 when there is nothing to average. */
    fun average(sum: Int, count: Int): Int =
        if (count == 0) 0 else Math.round(sum.toDouble() / count).toInt()
}
