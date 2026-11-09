package io.github.veitkramerschoeggl.trainingtracker.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.veitkramerschoeggl.trainingtracker.domain.GermanFormat
import io.github.veitkramerschoeggl.trainingtracker.domain.MonthDetail
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.LocalAccent
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.Palette
import kotlinx.coroutines.launch

private val SheetBackground = Color(0xFF13131A)

/** Bottom sheet with the statistics and training days of one month (tap a month in the statistics). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSheet(detail: MonthDetail, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 480.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = SheetBackground,
        scrimColor = Color(0xAA000000),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 14.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Palette.Border))
        },
    ) {
        // The sheet lives in its own window: keep its navigation bar dark like the app's.
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                window.navigationBarColor = SheetBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(GermanFormat.monthLong(detail.month), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.White)
                Text(
                    "✕",
                    modifier = Modifier.plainClickable(onClick = close).padding(horizontal = 6.dp, vertical = 2.dp).semantics { contentDescription = "Schließen" },
                    fontSize = 20.sp,
                    color = Palette.Muted,
                )
            }
            SheetStatRow(Modifier.padding(bottom = 8.dp)) {
                SheetStat("Gesamt", detail.total.toString(), accentBorder = true)
                SheetStat("Trainingstage", detail.trainingDays.toString())
                SheetStat("Ø / Tag", detail.averagePerDay.toString())
            }
            SheetStatRow(Modifier.padding(bottom = 14.dp)) {
                SheetStat("Bestleistung", detail.best.toString())
                SheetStat("Sätze", detail.sets.toString())
                SheetStat("Ø / Satz", detail.averagePerSet.toString())
            }
            SectionLabel("Trainingstage", Modifier.padding(bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (day in detail.days) SheetDayRow(day)
            }
        }
    }
}

@Composable
private fun SheetStatRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun RowScope.SheetStat(label: String, value: String, accentBorder: Boolean = false) {
    val accent = LocalAccent.current
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .card(14.dp, border = if (accentBorder) accent.dim else Palette.Border)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold, color = accent.main, textAlign = TextAlign.Center)
        Text(label, modifier = Modifier.padding(top = 3.dp), fontSize = 9.sp, letterSpacing = 0.4.sp, color = Palette.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SheetDayRow(day: io.github.veitkramerschoeggl.trainingtracker.domain.TrainingDay) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().card(12.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(day.total.toString(), modifier = Modifier.widthIn(min = 34.dp), fontSize = 22.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = accent.main)
        Column(Modifier.weight(1f)) {
            Text(GermanFormat.dayLabel(day.date), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Palette.DayText)
            val notes = day.notes
            Text(
                GermanFormat.sets(day) + if (notes.isNotEmpty()) " · " + notes.joinToString(" · ") else "",
                modifier = Modifier.padding(top = 1.dp),
                fontSize = 11.sp,
                color = Palette.Muted,
            )
        }
    }
}
