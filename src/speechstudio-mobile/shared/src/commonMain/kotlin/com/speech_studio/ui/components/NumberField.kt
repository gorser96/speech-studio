package com.speech_studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxDigits: Int = 4,
    isError: Boolean = false,
    supportingText: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    var editedInFocus by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(value) }
    var selection by remember { mutableStateOf(TextRange(0)) }
    if (text != value) {
        text = value
        selection = TextRange(value.length)
    }
    // While focused, before the first accepted change or press the whole value
    // stays selected, so typing a new value replaces the old one. A press on an
    // already focused field switches to cursor editing at the press position;
    // the whole value is selected again on the next focus from elsewhere.
    val displaySelection = if (focused && !editedInFocus) TextRange(0, text.length) else selection
    OutlinedTextField(
        value = TextFieldValue(text, displaySelection),
        onValueChange = { newValue ->
            val digits = newValue.text.filter { it.isDigit() }.take(maxDigits)
            if (digits != text) {
                text = digits
                editedInFocus = true
                selection = TextRange(text.length)
                if (text != value) onValueChange(text)
            } else {
                selection = newValue.selection
            }
        },
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    editedInFocus = focused
                }
            }
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
                if (focusState.isFocused) editedInFocus = false
            }
    )
}

/**
 * Компактный числовой ввод для плотных строк списка: без плавающей подписи,
 * единица измерения показана справа от значения внутри рамки.
 * Поведение ввода совпадает с [NumberField].
 */
@Composable
fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxDigits: Int = 4
) {
    var focused by remember { mutableStateOf(false) }
    var editedInFocus by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(value) }
    var selection by remember { mutableStateOf(TextRange(0)) }
    if (text != value) {
        text = value
        selection = TextRange(value.length)
    }
    val displaySelection = if (focused && !editedInFocus) TextRange(0, text.length) else selection
    val borderColor = if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant
    val valueColor = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        BasicTextField(
            value = TextFieldValue(text, displaySelection),
            onValueChange = { newValue ->
                val digits = newValue.text.filter { it.isDigit() }.take(maxDigits)
                if (digits != text) {
                    text = digits
                    editedInFocus = true
                    selection = TextRange(text.length)
                    if (text != value) onValueChange(text)
                } else {
                    selection = newValue.selection
                }
            },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = valueColor),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .semantics { this.contentDescription = contentDescription }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        editedInFocus = focused
                    }
                }
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    if (focusState.isFocused) editedInFocus = false
                }
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
