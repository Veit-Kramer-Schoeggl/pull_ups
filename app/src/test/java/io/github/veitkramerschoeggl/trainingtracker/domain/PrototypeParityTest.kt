package io.github.veitkramerschoeggl.trainingtracker.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Compares the app's statistics with what the HTML prototype (klimmzug-tracker-v11.html) renders
 * for the same data. `prototype-golden.json` is produced by tools/prototype-golden.js, which runs
 * the prototype's own JavaScript on a synthetic dataset (444 sets across a year boundary, pauses,
 * streaks, ties).
 */
class PrototypeParityTest {
    private val golden = JSONObject(javaClass.classLoader!!.getResource("prototype-golden.json")!!.readText())
    private val expected = golden.getJSONObject("expected")
    private val today = LocalDate.parse(golden.getString("today"))

    private val sets = golden.getJSONArray("sets").objects { i, row ->
        val set = row as JSONArray
        TrainingSet(
            id = i.toLong(),
            date = LocalDate.parse(set.getString(0)),
            reps = set.getInt(1),
            note = set.getString(2),
            createdAt = LocalDateTime.parse(set.getString(3)).toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
    }
    private val days = Stats.days(sets)
    private val weeks = Stats.weeks(days)
    private val stats = Stats.compute(days, weeks, today)

    @Test
    fun kpis() {
        val s = expected.getJSONObject("stats")
        assertEquals(s.getString("stat-total"), GermanFormat.number(stats.total))
        assertEquals(s.getString("stat-max"), stats.best.toString())
        assertEquals(s.getString("stat-week-avg"), stats.weekAverage.toString())
        assertEquals(s.getString("stat-avg"), stats.dayAverage.toString())
        assertEquals(s.getString("stat-count"), stats.activeDays.toString())
        assertEquals(s.getString("stat-best-week"), stats.bestWeek!!.total.toString())
        assertEquals(s.getString("stat-best-week-sub"), GermanFormat.weekTitle(stats.bestWeek!!.week))
        assertEquals(s.getString("stat-streak-reps"), stats.longestStreak!!.reps.toString())
        assertEquals(s.getString("stat-streak"), GermanFormat.days(stats.longestStreak!!.days))
        assertEquals(s.getString("stat-inactive"), GermanFormat.days(stats.longestPause))
        assertEquals(s.getString("stat-avg10"), stats.averageLast10Days.toString())
        assertEquals(s.getString("stat-avg4w"), stats.averageLast4Weeks.toString())
        assertEquals(s.getString("stat-avg-set"), stats.averagePerSetLast30Days.toString())
    }

    @Test
    fun streakCoversTheSameDays() {
        // The prototype cuts its date labels ("Sa., 12.–Mo., 28."); the app shows "12.09.–28.09.".
        val streak = stats.longestStreak!!
        val prototypeLabel = "${GermanFormat.dayLabel(streak.start).take(8)}–${GermanFormat.dayLabel(streak.end).take(8)}"
        assertEquals(expected.getJSONObject("stats").getString("stat-streak-sub"), prototypeLabel)
    }

    @Test
    fun monthList() {
        val months = expected.getJSONArray("months").objects { _, row -> (row as JSONArray).let { Triple(it.getString(0), it.getInt(1), it.getInt(2)) } }
        val max = stats.months.maxOf { it.total }
        assertEquals(
            months,
            stats.months.map { Triple(GermanFormat.month(it.month), Math.round(it.total * 100.0 / max).toInt(), it.total) },
        )
    }

    @Test
    fun topThreeDays() {
        // The prototype only shows the weekday of those days ("Mi."), the app the full date.
        val top3 = expected.getJSONArray("top3").objects { _, row -> (row as JSONArray).let { it.getInt(0) to it.getString(1) } }
        assertEquals(top3, stats.top3.map { it.total to GermanFormat.weekdayShort(it.date) })
    }

    @Test
    fun dayList() {
        val items = expected.getJSONArray("days").objects { _, item -> item as JSONObject }
        val newestFirst = days.asReversed()
        assertEquals(items.size, newestFirst.size)
        items.forEachIndexed { i, item ->
            val day = newestFirst[i]
            assertEquals(item.getString("label"), GermanFormat.dayLabel(day.date))
            assertEquals(item.getInt("total"), day.total)
            assertEquals(item.getString("sets"), GermanFormat.sets(day))
            assertEquals(item.getString("notes"), day.notes.joinToString(" · "))
            assertEquals(item.getBoolean("isMax"), day.total == stats.best)
            assertEquals(item.getBoolean("isNew"), i == 0)
            assertEquals(item.getBoolean("isToday"), day.date == today)
        }
    }

    @Test
    fun weekListWithBestWeekMarked() {
        // (label, total, is the best week — the prototype appends " ★" to its label)
        val expectedWeeks = expected.getJSONArray("weeks").objects { _, row ->
            (row as JSONArray).let {
                val label = it.getString(0)
                Triple(label.removeSuffix(" ★").replace(Regex("\\s+"), " "), it.getInt(1), label.endsWith(" ★"))
            }
        }
        assertEquals(
            expectedWeeks,
            weeks.asReversed().map { Triple(GermanFormat.weekLabel(it.week), it.total, it.week == stats.bestWeek?.week) },
        )
    }

    @Test
    fun milestones() {
        val expectedMilestones = expected.getJSONObject("milestones")
        val reached = expectedMilestones.getJSONArray("reached").objects { _, row -> (row as JSONArray).let { Triple(it.getString(0), it.getString(1), it.getString(2)) } }
        val progress = stats.milestones
        assertEquals(
            reached,
            progress.reached.map {
                val sinceLast = it.daysSincePrevious?.let { d -> " · +$d Tage seit ${GermanFormat.number(it.value - Milestones.STEP)}" }.orEmpty()
                Triple(GermanFormat.number(it.value), "in ${it.sinceStart} erreicht", "am ${GermanFormat.date(it.date)}$sinceLast")
            },
        )
        val next = expectedMilestones.getJSONArray("next")
        assertEquals(next.getString(0), GermanFormat.number(progress.next))
        assertEquals(next.getString(1), "noch ${GermanFormat.number(progress.remaining)}")
        assertEquals(next.getInt(2), progress.percent)
        val eta = "Bei aktuellem Tempo (Ø ${Milestones.oneDecimal(progress.ratePerDay)}/Tag, letzte 28 Tage) ca. am ${GermanFormat.date(progress.forecast!!)}"
        assertEquals(next.getString(3), "${progress.percent} % · $eta")
    }

    @Test
    fun monthSheets() {
        val sheets = expected.getJSONObject("monthSheets")
        val keys = sheets.keys().asSequence().toList()
        assertEquals(stats.months.size, keys.size)
        for (key in keys) {
            val sheet = sheets.getJSONObject(key)
            val detail = Stats.monthDetail(days, java.time.YearMonth.parse(key))
            assertEquals(sheet.getString("title"), GermanFormat.monthLong(detail.month))
            assertEquals(
                sheet.getJSONArray("stats").objects { _, v -> v as String },
                listOf(detail.total, detail.trainingDays, detail.averagePerDay, detail.best, detail.sets, detail.averagePerSet).map { it.toString() },
            )
            assertEquals(
                sheet.getJSONArray("days").objects { _, row -> (row as JSONArray).let { Triple(it.getInt(0), it.getString(1), it.getString(2)) } },
                detail.days.map { day ->
                    val notes = day.notes
                    Triple(day.total, GermanFormat.dayLabel(day.date), GermanFormat.sets(day) + if (notes.isNotEmpty()) " · " + notes.joinToString(" · ") else "")
                },
            )
        }
    }

    @Test
    fun movingAverageLine() {
        val average = expected.getJSONObject("average")
        assertEquals("flex", average.getString("legend")) // shown for the day chart with enough data
        val values = days.map { it.total }
        val maxValue = average.getInt("maxValue")
        assertEquals(values.max(), maxValue)
        // The prototype's canvas y coordinates (height 100): H - 4 - round(avg / max * (H - 12)).
        val ys = Stats.movingAverage(values).map { 100 - 4 - Math.round(it / maxValue * (100 - 12)).toInt() }
        assertEquals(average.getJSONArray("ys").objects { _, v -> (v as Number).toInt() }, ys)
    }

    private fun <T> JSONArray.objects(transform: (Int, Any) -> T): List<T> = (0 until length()).map { transform(it, get(it)) }
}
