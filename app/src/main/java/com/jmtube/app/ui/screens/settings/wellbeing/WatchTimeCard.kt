package com.jmtube.app.ui.screens.settings.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jmtube.app.R
import com.jmtube.app.ui.components.shared.FlowLoadingIndicator
import com.jmtube.app.ui.components.stats.spentTimeLabel
import java.time.format.TextStyle
import java.util.Locale

private val CardPadding = 20.dp
private val ChartHeight = 132.dp
private val ChartTopSpacing = 20.dp
private val BarSpacing = 8.dp
private val LabelSpacing = 6.dp
private const val MIN_BAR_FRACTION = 0.02f
private const val BAR_WIDTH_FRACTION = 0.6f

/**
 * The daily average over the last week, above one bar per day. Today's bar is in the primary
 * colour so it can be told apart from the rest; the chart is otherwise static.
 */
@Composable
internal fun WatchTimeCard(
    summary: WatchTimeSummary?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                text = stringResource(R.string.daily_average_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary == null) {
                Box(Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) { FlowLoadingIndicator() }
                return@Column
            }
            Text(
                text = spentTimeLabel(summary.dailyAverageMillis),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_watch_time_week_total, spentTimeLabel(summary.weekMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(ChartTopSpacing))
            if (summary.isEmpty) {
                Box(Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_watch_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                WeekChart(summary)
            }
        }
    }
}

@Composable
private fun WeekChart(summary: WatchTimeSummary) {
    val peak = summary.days.maxOf { it.millis }.coerceAtLeast(1L)
    val today = summary.days.last().date
    val locale = Locale.getDefault()
    var selectedDay by remember { mutableStateOf<DayWatchTime?>(null) }
    val description = summary.days.map { "${it.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${spentTimeLabel(it.millis)}" }

    Column {
        if (selectedDay != null) {
            val dayName = selectedDay!!.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            Text(
                text = "$dayName: ${spentTimeLabel(selectedDay!!.millis)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = description.joinToString() },
            horizontalArrangement = Arrangement.spacedBy(BarSpacing),
        ) {
            summary.days.forEach { day ->
                val isToday = day.date == today
                val isSelected = selectedDay == day
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { selectedDay = if (selectedDay == day) null else day },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(BAR_WIDTH_FRACTION)
                                    .fillMaxHeight((day.millis.toFloat() / peak).coerceIn(MIN_BAR_FRACTION, 1f))
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.tertiary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                    ),
                        )
                    }
                    Spacer(Modifier.height(LabelSpacing))
                    Text(
                        text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday || isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
