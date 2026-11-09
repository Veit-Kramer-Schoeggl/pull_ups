package io.github.veitkramerschoeggl.trainingtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import io.github.veitkramerschoeggl.trainingtracker.domain.TimeInput
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.LocalAccent
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.veitkramerschoeggl.trainingtracker.data.ImportMode
import io.github.veitkramerschoeggl.trainingtracker.domain.GermanFormat
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.Palette

/** Asked when an imported file contains days that already exist in the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(preview: ImportPreview, onConfirm: (ImportMode) -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Palette.Card) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Excel-Import", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                Text(
                    "Die Datei enthält ${GermanFormat.days(preview.days)} mit ${GermanFormat.number(preview.reps)} Klimmzügen. " +
                        "${GermanFormat.days(preview.existingDays)} davon ${if (preview.existingDays == 1) "ist" else "sind"} bereits in der App.",
                    fontSize = 14.sp,
                    color = Palette.Soft,
                )
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { onConfirm(ImportMode.ADD_MISSING_DAYS) }) { Text("Nur neue Tage hinzufügen") }
                    TextButton(onClick = { onConfirm(ImportMode.REPLACE_ALL) }) { Text("Alles durch Datei ersetzen") }
                    TextButton(onClick = onDismiss) { Text("Abbrechen", color = Palette.Subtle) }
                }
            }
        }
    }
}

/** Explains the one-time "install unknown apps" permission before opening the settings. */
@Composable
fun InstallPermissionDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Updates erlauben") },
        text = {
            Text(
                "Damit sich die App selbst aktualisieren kann, muss sie einmalig Apps installieren dürfen. " +
                    "Aktiviere in den Einstellungen „Aus dieser Quelle zulassen“ und kehre dann zurück – " +
                    "das Update startet automatisch.",
            )
        },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Einstellungen öffnen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen", color = Palette.Subtle) } },
    )
}

/** Asked when an entry is made for another day than today: the time is typed, "__:__" is preset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(date: LocalDate, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val accent = LocalAccent.current
    var digits by rememberSaveable { mutableStateOf("") }
    val time = TimeInput.parse(digits)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val style = TextStyle(color = accent.main, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Palette.Card) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Uhrzeit eingeben", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                Text(
                    "Der Eintrag gilt für ${GermanFormat.dayLabel(date)}. Um wie viel Uhr war das Training?",
                    fontSize = 14.sp,
                    color = Palette.Soft,
                )
                BasicTextField(
                    value = digits,
                    onValueChange = { digits = TimeInput.sanitize(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.Background)
                        .border(2.dp, accent.main, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp),
                    textStyle = style,
                    singleLine = true,
                    cursorBrush = SolidColor(accent.main),
                    visualTransformation = TimeMask,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { time?.let(onConfirm) }),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen", color = Palette.Subtle) }
                    TextButton(onClick = { time?.let(onConfirm) }, enabled = time != null) { Text("Eintragen") }
                }
            }
        }
    }
}

/** Shows the typed digits inside "__:__". */
private object TimeMask : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        AnnotatedString(TimeInput.mask(text.text)),
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset >= 2) offset + 1 else offset
            override fun transformedToOriginal(offset: Int) = (if (offset > 2) offset - 1 else offset).coerceAtMost(text.length)
        },
    )
}
