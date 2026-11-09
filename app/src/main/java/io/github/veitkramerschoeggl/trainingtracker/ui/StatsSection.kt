package io.github.veitkramerschoeggl.trainingtracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.time.YearMonth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.veitkramerschoeggl.trainingtracker.domain.GermanFormat
import io.github.veitkramerschoeggl.trainingtracker.domain.MilestoneProgress
import io.github.veitkramerschoeggl.trainingtracker.domain.Milestones
import io.github.veitkramerschoeggl.trainingtracker.domain.MonthTotal
import io.github.veitkramerschoeggl.trainingtracker.domain.TrainingDay
import io.github.veitkramerschoeggl.trainingtracker.domain.TrainingStats
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.LocalAccent
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.Palette

/** Four pinned KPIs, "Mehr Statistiken" toggle and the expandable details. */
@Composable
fun StatsSection(
    stats: TrainingStats,
    expanded: Boolean,
    onToggle: () -> Unit,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        StatRow(spacing = 8) {
            StatBox("Gesamt", GermanFormat.number(stats.total), accentBorder = true)
            StatBox("Bestleistung", stats.best.toString(), sub = "pro Tag")
            StatBox("Ø / Woche", stats.weekAverage.toString())
            StatBox("Ø / Tag", stats.dayAverage.toString())
        }
        ToggleButton(expanded, onToggle, Modifier.padding(top = 10.dp, bottom = 10.dp))
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(350)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(350)) + fadeOut(tween(250)),
        ) {
            StatDetails(stats, onMonthClick)
        }
        Box(Modifier.height(14.dp))
    }
}

@Composable
private fun StatDetails(stats: TrainingStats, onMonthClick: (YearMonth) -> Unit) {
    val streak = stats.longestStreak
    val streakRange = when {
        streak == null -> null
        streak.start == streak.end -> GermanFormat.dayMonth(streak.start)
        else -> "${GermanFormat.dayMonth(streak.start)}–${GermanFormat.dayMonth(streak.end)}"
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatRow {
            StatBox("Tage aktiv", stats.activeDays.toString())
            StatBox("Beste Woche", (stats.bestWeek?.total ?: 0).toString(), sub = stats.bestWeek?.let { GermanFormat.weekTitle(it.week) })
        }
        StatRow {
            StatBox("Längster Streak", GermanFormat.days(streak?.days ?: 0), sub = streakRange)
            StatBox("Reps im längsten Streak", (streak?.reps ?: 0).toString())
        }
        StatRow {
            StatBox("Längste Pause", GermanFormat.days(stats.longestPause), sub = "Tage inaktiv")
            StatBox("Ø / Tag", stats.averageLast10Days.toString(), sub = "letzte 10 Tage")
        }
        StatRow {
            StatBox("Ø / Woche", stats.averageLast4Weeks.toString(), sub = "letzte 4 Wochen")
            StatBox("Ø / Satz (30 Tage)", stats.averagePerSetLast30Days.toString())
        }
        MilestoneBox(stats.milestones)
        MonthList(stats.months, onMonthClick)
        TopDays(stats.top3)
    }
}

/** Grid row: equal widths and equal heights like CSS grid. */
@Composable
private fun StatRow(spacing: Int = 10, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

@Composable
private fun RowScope.StatBox(label: String, value: String, sub: String? = null, accentBorder: Boolean = false) {
    val accent = LocalAccent.current
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .card(14.dp, border = if (accentBorder) accent.dim else Palette.Border)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold, color = accent.main, textAlign = TextAlign.Center)
        Text(
            label,
            modifier = Modifier.padding(top = 3.dp),
            fontSize = 9.sp,
            letterSpacing = 0.4.sp,
            color = Palette.Muted,
            textAlign = TextAlign.Center,
        )
        if (!sub.isNullOrEmpty()) {
            Text(sub, modifier = Modifier.padding(top = 2.dp), fontSize = 9.sp, color = Palette.Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ToggleButton(expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "arrow")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .card(12.dp)
            .plainClickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (expanded) "Weniger" else "Mehr Statistiken", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Palette.Muted)
        DropdownArrow(Palette.Muted, rotation)
    }
}

@Composable
private fun MonthList(months: List<MonthTotal>, onMonthClick: (YearMonth) -> Unit) {
    val accent = LocalAccent.current
    val max = months.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
    Column(Modifier.fillMaxWidth().card(14.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        BoxTitle("KLIMMZÜGE PRO MONAT")
        for (month in months) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).plainClickable { onMonthClick(month.month) }.padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(GermanFormat.month(month.month), fontSize = 13.sp, color = Palette.Soft)
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Palette.Border),
                ) {
                    val fraction = Math.round(month.total * 100f / max) / 100f
                    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(accent.dim))
                }
                Text(month.total.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent.main)
                Text("›", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp, color = Palette.Faint)
            }
        }
    }
}

@Composable
private fun TopDays(days: List<TrainingDay>) {
    val accent = LocalAccent.current
    Column(Modifier.fillMaxWidth().card(14.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        BoxTitle("TOP 3 TAGE")
        days.forEachIndexed { index, day ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row {
                    Text("${index + 1}.", modifier = Modifier.width(16.dp), fontSize = 13.sp, color = Palette.Muted)
                    Text(day.total.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent.main)
                }
                Text(GermanFormat.dayLabel(day.date), fontSize = 11.sp, color = Palette.Subtle)
            }
        }
    }
}

@Composable
private fun BoxTitle(text: String) {
    Text(text, modifier = Modifier.padding(bottom = 8.dp), fontSize = 10.sp, letterSpacing = 0.5.sp, color = Palette.Muted)
}

private val MilestoneDivider = Color(0xFF22222C)

/** "MEILENSTEINE": every reached 1000 reps, newest first, and the progress to the next one. */
@Composable
private fun MilestoneBox(progress: MilestoneProgress) {
    val accent = LocalAccent.current
    Column(Modifier.fillMaxWidth().card(14.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        BoxTitle("MEILENSTEINE")
        for (milestone in progress.reached) {
            Row(
                modifier = Modifier.fillMaxWidth().drawBehind {
                    drawLine(MilestoneDivider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }.padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    GermanFormat.number(milestone.value),
                    modifier = Modifier
                        .widthIn(min = 52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.main)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Palette.Background,
                    textAlign = TextAlign.Center,
                )
                Column(Modifier.weight(1f)) {
                    Text("in ${milestone.sinceStart} erreicht", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Palette.DayText)
                    val sinceLast = milestone.daysSincePrevious?.let {
                        " · +$it Tage seit ${GermanFormat.number(milestone.value - Milestones.STEP)}"
                    }.orEmpty()
                    Text("am ${GermanFormat.date(milestone.date)}$sinceLast", modifier = Modifier.padding(top = 2.dp), fontSize = 10.sp, color = Palette.Muted)
                }
            }
        }
        Column(Modifier.padding(top = 10.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    buildAnnotatedString {
                        append("Nächster Meilenstein: ")
                        withStyle(SpanStyle(color = accent.main, fontSize = 14.sp, fontWeight = FontWeight.Bold)) {
                            append(GermanFormat.number(progress.next))
                        }
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.Soft,
                )
                Text("noch ${GermanFormat.number(progress.remaining)}", fontSize = 11.sp, color = Palette.Muted)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Palette.Border)) {
                Box(Modifier.fillMaxWidth(progress.percent / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(accent.main))
            }
            val eta = progress.forecast?.let {
                "Bei aktuellem Tempo (Ø ${Milestones.oneDecimal(progress.ratePerDay)}/Tag, letzte 28 Tage) ca. am ${GermanFormat.date(it)}"
            } ?: "Keine Einträge in den letzten 28 Tagen – keine Prognose möglich"
            Text("${progress.percent} % · $eta", modifier = Modifier.padding(top = 6.dp), fontSize = 10.sp, color = Palette.Muted)
        }
    }
}
