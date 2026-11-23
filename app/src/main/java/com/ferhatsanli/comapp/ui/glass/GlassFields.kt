package com.ferhatsanli.comapp.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/*
 * The app's standard building blocks for anything that appears in more than one place.
 * New screens use these instead of the Material components directly, so every text field,
 * section header and small icon action looks and behaves the same everywhere.
 */

/**
 * The app's text field: glass colors and shape, and a bare "x" at its end that clears it
 * whenever it holds text ([clearable], on by default; off for tiny fields such as hours).
 * [placeholder] shows inside an empty field without a floating label; use [label] for forms.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    clearable: Boolean = true,
    clearDescription: String = "Clear the text",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = if (clearable && enabled && value.isNotEmpty()) {
            { ClearMark(clearDescription) { onValueChange("") } }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = GlassFieldShape,
        colors = glassTextFieldColors(),
    )
}

/** The bare "x" that clears a field: no background, no ripple ring, a 40 dp touch target. */
@Composable
private fun ClearMark(description: String, onClear: () -> Unit) {
    val tint = LocalGlassPalette.current.inkMuted
    Box(
        Modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = description,
                onClick = onClear,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(12.dp)) {
            val stroke = 1.8.dp.toPx()
            drawLine(tint, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(tint, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}

/**
 * A small monotone icon button that sits flush with a card's right edge (the pencil next to
 * "Recent", for example). [description] is what screen readers say.
 */
@Composable
fun GlassIconAction(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(32.dp)
            .offset(x = 8.dp),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = LocalGlassPalette.current.inkMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A section title inside a card ("Favorites", "Recent"), with an optional action at the right. */
@Composable
fun GlassSectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = LocalGlassPalette.current.inkMuted,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}
