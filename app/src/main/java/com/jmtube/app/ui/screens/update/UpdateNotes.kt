package com.jmtube.app.ui.screens.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private val LinePadding = 16.dp
private val LineVerticalPadding = 12.dp
private val BadgePadding = 10.dp
private val BadgeVerticalPadding = 6.dp
private val BadgeSpacing = 6.dp
private val BadgeIconSize = 16.dp

/** The release notes as list items: category header, then rounded cards for each item. */
internal fun LazyListScope.releaseNotes(notes: List<String>) {
    item(key = "section-header") {
        Text(
            text = "New",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFFF0000),
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    itemsIndexed(notes, key = { index, _ -> "note-$index" }) { _, noteText ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            NoteParagraph(noteText, Modifier.padding(horizontal = LinePadding, vertical = LineVerticalPadding))
        }
    }
}

@Composable
private fun NoteParagraph(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = parseNoteWithHighlights(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

private fun parseNoteWithHighlights(text: String): AnnotatedString =
    buildAnnotatedString {
        val regex = Regex("""(#\d+)""")
        var lastIndex = 0
        for (match in regex.findAll(text)) {
            append(text.substring(lastIndex, match.range.first))
            withStyle(SpanStyle(color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

/** A small fact pill about the release: version change, size, date. */
@Composable
internal fun ReleaseFact(
    icon: ImageVector,
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BadgePadding, vertical = BadgeVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(BadgeSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(BadgeIconSize))
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
        }
    }
}
