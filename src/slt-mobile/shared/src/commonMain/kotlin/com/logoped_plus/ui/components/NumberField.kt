package com.logoped_plus.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

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
