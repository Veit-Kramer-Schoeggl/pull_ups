package io.github.veitkramerschoeggl.trainingtracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.veitkramerschoeggl.trainingtracker.R
import io.github.veitkramerschoeggl.trainingtracker.domain.GermanFormat
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.LocalAccent
import io.github.veitkramerschoeggl.trainingtracker.ui.theme.Palette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** "Training eintragen" / "Tag bearbeiten" card. */
@Composable
fun EntryCard(
    form: EntryForm,
    existingTotal: Int?,
    saveFeedback: String?,
    onDateChange: (LocalDate) -> Unit,
    onRepsChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    onSubmit: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val editing = form.editing != null
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }

    Column(modifier.card(18.dp).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(
            if (editing) "Tag bearbeiten" else "Training eintragen",
            modifier = Modifier.padding(bottom = 8.dp),
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            color = Palette.Muted,
        )
        if (existingTotal != null) ExistingHint(existingTotal, Modifier.padding(bottom = 14.dp))
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateField(form.date, onClick = { showDatePicker = true }, Modifier.weight(1f))
            StepperButton("−", "Weniger") { onStep(-1) }
            RepsField(form.reps, onRepsChange, onDone = submit, Modifier.width(72.dp))
            StepperButton("+", "Mehr") { onStep(1) }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteField(form.note, onNoteChange, onDone = submit, Modifier.weight(1f).fillMaxHeight())
            SubmitButton(
                text = saveFeedback ?: if (editing) "Änderung speichern" else "Eintragen",
                enabled = (form.reps.toIntOrNull() ?: 0) >= 1,
                saved = saveFeedback != null,
                onClick = submit,
            )
        }
        if (editing) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Palette.Border, RoundedCornerShape(10.dp))
                    .plainClickable(onClick = onCancelEdit)
                    .padding(7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Bearbeitung abbrechen", fontSize = 12.sp, color = Palette.Subtle)
            }
        }
    }

    if (showDatePicker) {
        EntryDatePicker(
            date = form.date,
            onConfirm = {
                onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .card(10.dp, background = Palette.Background)
            .plainClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(GermanFormat.date(date), modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, color = Palette.Text)
        Icon(
            painterResource(R.drawable.ic_calendar),
            contentDescription = "Datum wählen",
            modifier = Modifier.size(16.dp),
            tint = Palette.White,
        )
    }
}

@Composable
private fun ExistingHint(total: Int, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val bold = SpanStyle(color = accent.main, fontWeight = FontWeight.Bold)
    Text(
        buildAnnotatedString {
            append("An diesem Tag bereits ")
            withStyle(bold) { append(total.toString()) }
            append(" Klimmzüge — wird ")
            withStyle(bold) { append("addiert") }
            append(".")
        },
        modifier = modifier
            .fillMaxWidth()
            .card(8.dp, background = Palette.Background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        fontSize = 12.sp,
        color = Palette.Muted,
    )
}

@Composable
private fun StepperButton(symbol: String, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 40.dp)
            .card(10.dp, background = if (pressed) Palette.ControlPressed else Palette.Control)
            .plainClickable(interaction, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
    }
}

@Composable
private fun RepsField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val style = TextStyle(
        color = accent.main,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Palette.Background)
            .border(2.dp, accent.main, RoundedCornerShape(10.dp))
            .padding(4.dp),
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(accent.main),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                // Browser default placeholder color, as in the prototype
                if (value.isEmpty()) Text("0", style = style.copy(color = Color(0xFF757575)))
                innerTextField()
            }
        },
    )
}

@Composable
private fun NoteField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val style = TextStyle(color = Palette.Text, fontSize = 13.sp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .card(10.dp, background = Palette.Background)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(accent.main),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) Text("Notiz (optional)", style = style.copy(color = Palette.Faint))
                innerTextField()
            }
        },
    )
}

@Composable
private fun SubmitButton(text: String, enabled: Boolean, saved: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val background by animateColorAsState(if (saved) accent.dim else accent.main, tween(300), label = "submitBg")
    val content by animateColorAsState(if (saved) accent.main else Palette.Background, tween(300), label = "submitFg")
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .plainClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold, color = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePicker(date: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val accent = LocalAccent.current
    // The Material date picker works with UTC midnight.
    val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val colors = DatePickerDefaults.colors(
        containerColor = Palette.Card,
        titleContentColor = Palette.Muted,
        headlineContentColor = Palette.Text,
        weekdayContentColor = Palette.Muted,
        subheadContentColor = Palette.Soft,
        navigationContentColor = Palette.Text,
        yearContentColor = Palette.Text,
        currentYearContentColor = accent.main,
        selectedYearContentColor = Palette.Background,
        selectedYearContainerColor = accent.main,
        dayContentColor = Palette.Text,
        selectedDayContentColor = Palette.Background,
        selectedDayContainerColor = accent.main,
        todayContentColor = accent.main,
        todayDateBorderColor = accent.main,
        dividerColor = Palette.Border,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors)
    }
}
