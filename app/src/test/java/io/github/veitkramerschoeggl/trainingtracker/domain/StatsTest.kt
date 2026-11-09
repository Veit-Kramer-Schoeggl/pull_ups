package io.github.veitkramerschoeggl.trainingtracker.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsTest {
    private val today = LocalDate.of(2026, 9, 28)

    private fun sets(vararg entries: Pair<String, List<Int>>): List<TrainingSet> {
        var id = 0L
        return entries.flatMap { (date, reps) ->
            reps.map { TrainingSet(id++, LocalDate.parse(date), it, "", id * 1000) }
        }
    }

    private fun compute(sets: List<TrainingSet>): TrainingStats {
        val days = Stats.days(sets)
        return Stats.compute(days, Stats.weeks(days), today)
    }

    @Test
    fun emptyDataGivesZeros() {
        val stats = compute(emptyList())
        assertEquals(0, stats.total)
        assertEquals(0, stats.best)
        assertEquals(0, stats.weekAverage)
        assertNull(stats.longestStreak)
        assertNull(stats.bestWeek)
        assertEquals(0, stats.longestPause)
    }

    @Test
    fun streakAndPause() {
        val stats = compute(
            sets(
                "2026-09-01" to listOf(5),
                "2026-09-02" to listOf(5, 5),
                "2026-09-03" to listOf(5),
                "2026-09-10" to listOf(8),
                "2026-09-11" to listOf(8),
            ),
        )
        assertEquals(Streak(3, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), 20), stats.longestStreak)
        assertEquals(6, stats.longestPause) // 4th to 9th
    }

    @Test
    fun averagesRoundHalfUpLikeJavaScript() {
        assertEquals(3, Stats.average(5, 2)) // 2.5 -> 3
        assertEquals(2, Stats.average(7, 3)) // 2.33 -> 2
        assertEquals(0, Stats.average(0, 0))
    }

    @Test
    fun monthsNewestFirst() {
        val stats = compute(sets("2026-08-31" to listOf(10), "2026-09-01" to listOf(3, 4)))
        assertEquals(listOf(MonthTotal(YearMonth.of(2026, 9), 7), MonthTotal(YearMonth.of(2026, 8), 10)), stats.months)
    }

    @Test
    fun isoWeeksAcrossTheYearBoundary() {
        val week = IsoWeek.of(LocalDate.of(2027, 1, 1)) // a Friday that still belongs to 2026
        assertEquals(2026, week.year)
        assertEquals(53, week.week)
        assertEquals(LocalDate.of(2026, 12, 28), week.monday)
        assertEquals("KW 53/2026 (28.12. – 03.01.)", GermanFormat.weekLabel(week))
    }

    @Test
    fun chartValuesPerDayRespectTheRange() {
        // KW 23, KW 37 (Sunday 13.09.) and KW 38
        val days = Stats.days(sets("2026-06-01" to listOf(1), "2026-09-13" to listOf(2), "2026-09-20" to listOf(3)))
        val weeks = Stats.weeks(days)
        assertEquals(listOf(1, 2, 3), Stats.chartValues(days, weeks, perWeek = false, range = ChartRange.ALL, today = today))
        // Days: at most 14 days old. Weeks: the last two weeks that have training.
        assertEquals(listOf(3), Stats.chartValues(days, weeks, perWeek = false, range = ChartRange.TWO_WEEKS, today = today))
        assertEquals(listOf(2, 3), Stats.chartValues(days, weeks, perWeek = true, range = ChartRange.TWO_WEEKS, today = today))
        assertEquals(listOf(3), Stats.chartValues(days, weeks, perWeek = false, range = ChartRange.ONE_MONTH, today = LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun germanFormats() {
        assertEquals("1 129", GermanFormat.number(1129))
        assertEquals("999", GermanFormat.number(999))
        assertEquals("Jän. 2026", GermanFormat.month(YearMonth.of(2026, 1)))
        assertEquals("Do., 01.01.2026", GermanFormat.dayLabel(LocalDate.of(2026, 1, 1)))
        assertEquals("1 Tag", GermanFormat.days(1))
        assertEquals("0 Tage", GermanFormat.days(0))
        val day = Stats.days(sets("2026-09-01" to listOf(10, 5, 8))).single()
        assertEquals("10 + 5 + 8 Sätze", GermanFormat.sets(day))
    }

    @Test
    fun milestonesEveryThousandReps() {
        val days = Stats.days(sets("2026-06-01" to listOf(600), "2026-06-10" to listOf(450), "2026-07-02" to listOf(1100)))
        val progress = Milestones.compute(days, today)
        assertEquals(listOf(2000, 1000), progress.reached.map { it.value })
        assertEquals(LocalDate.of(2026, 7, 2), progress.reached[0].date) // 2150 passes 2000 on that day
        assertEquals("1 Monat und 1 Tag", progress.reached[0].sinceStart)
        assertEquals(22, progress.reached[0].daysSincePrevious)
        assertEquals(null, progress.reached[1].daysSincePrevious)
        assertEquals(3000, progress.next)
        assertEquals(850, progress.remaining)
        assertEquals(15, progress.percent)
        assertNull(progress.forecast) // nothing in the last 28 days
    }

    @Test
    fun milestoneDurationOverflowsLikeJavaScript() {
        // Values computed by the prototype's own fmtDur (setMonth overflow: 31 Jan + 1 month = 3 Mar).
        assertEquals("1 Monat und -2 Tagen", Milestones.duration(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1)))
        assertEquals("2 Monaten und 0 Tagen", Milestones.duration(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 31)))
        assertEquals("1 Monat und 28 Tagen", Milestones.duration(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 2, 28)))
        assertEquals("0 Tagen", Milestones.duration(LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 22)))
    }

    @Test
    fun oneDecimalRoundsLikeToFixed() {
        assertEquals("7,5", Milestones.oneDecimal(7.45))
        assertEquals("21,4", Milestones.oneDecimal(21.392857142857142))
    }

    @Test
    fun movingAverageUsesNineValueWindow() {
        val values = (1..12).toList()
        val average = Stats.movingAverage(values)
        assertEquals(3.0, average[0], 1e-9)   // 1..5
        assertEquals(7.0, average[6], 1e-9)   // 3..11
        assertEquals(10.0, average[11], 1e-9) // 8..12
    }

    @Test
    fun monthDetailCountsSetsAndDays() {
        val days = Stats.days(sets("2026-08-31" to listOf(50), "2026-09-01" to listOf(5, 6), "2026-09-05" to listOf(10)))
        val detail = Stats.monthDetail(days, YearMonth.of(2026, 9))
        assertEquals(21, detail.total)
        assertEquals(2, detail.trainingDays)
        assertEquals(11, detail.averagePerDay) // 10.5 -> 11
        assertEquals(11, detail.best)
        assertEquals(3, detail.sets)
        assertEquals(7, detail.averagePerSet)
        assertEquals(listOf(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1)), detail.days.map { it.date })
    }
}
