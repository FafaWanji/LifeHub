package com.example.lifeorganizer.calendar.ui

import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.i18n.Str
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.calendar.data.EventWithReminders
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WeekView(
    events: List<EventWithReminders>,
    lang: String,
    currentDate: LocalDate,
    firstDayOfWeek: java.time.DayOfWeek = daysOfWeek().first(),
    onEventClick: (EventWithReminders) -> Unit
) {
    // Follows the day selected in the month view
    var startOfWeek by remember(currentDate) { mutableStateOf(currentDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))) }
    val days = remember(startOfWeek) { (0..6).map { startOfWeek.plusDays(it.toLong()) } }
    
    val weekEvents = remember(events, startOfWeek) {
        val endOfWeek = startOfWeek.plusDays(6)
        events.filter { eventWithReminders ->
            val eventDate = Instant.ofEpochMilli(eventWithReminders.event.startTimeMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            !eventDate.isBefore(startOfWeek) && !eventDate.isAfter(endOfWeek)
        }
    }

    val allDayEvents = weekEvents.filter { it.event.isAllDay }
    val timeEvents = weekEvents.filter { !it.event.isAllDay }

    val verticalScrollState = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Scroll to an hour before now if today is in the week (60dp per hour, converted with the real density)
    LaunchedEffect(startOfWeek) {
        if (LocalDate.now() in days) {
            val hour = (LocalTime.now().hour - 1).coerceAtLeast(0)
            verticalScrollState.scrollTo(with(density) { (hour * 60).dp.roundToPx() })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { startOfWeek = startOfWeek.minusWeeks(1) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = Str.previousWeek.text())
            }
            Text(
                text = startOfWeek.format(com.example.lifeorganizer.core.i18n.localDateFormatter(lang, "yyyyMMM")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { startOfWeek = startOfWeek.plusWeeks(1) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = Str.nextWeek.text())
            }
        }

        // Days Header
        Row(modifier = Modifier.fillMaxWidth().padding(start = 50.dp)) {
            days.forEach { day ->
                val isToday = day == LocalDate.now()
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.Builder().setLanguage(lang).build()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(24.dp)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // All Day Events Area
        if (allDayEvents.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 50.dp, bottom = 4.dp)) {
                days.forEach { day ->
                    val dayEvents = allDayEvents.filter {
                        Instant.ofEpochMilli(it.event.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate() == day
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 2.dp)) {
                        dayEvents.forEach { event ->
                            val color = event.category?.color?.let { Color(it) } ?: event.event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .background(color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .clickable { onEventClick(event) }
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = event.event.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
        }

        // Time Grid
        Box(modifier = Modifier.weight(1f).verticalScroll(verticalScrollState)) {
            val hourHeight = 60.dp
            
            // Grid Lines & Time Labels
            Column {
                for (hour in 0..23) {
                    Row(modifier = Modifier.fillMaxWidth().height(hourHeight)) {
                        Text(
                            text = "$hour:00",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(50.dp).padding(end = 4.dp, top = 2.dp),
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.fillMaxSize()) {
                            HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            if (hour == 23) {
                                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // Vertical Dividers for Days
            Row(modifier = Modifier.fillMaxSize().padding(start = 50.dp)) {
                days.forEach { _ ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        VerticalDivider(modifier = Modifier.align(Alignment.CenterStart), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            // Events Layout
            Row(modifier = Modifier.fillMaxSize().padding(start = 50.dp)) {
                days.forEach { day ->
                    val dayEvents = timeEvents.filter {
                        Instant.ofEpochMilli(it.event.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate() == day
                    }
                    Box(modifier = Modifier.weight(1f).height(hourHeight * 24)) {
                        dayEvents.forEach { event ->
                            val startTime = Instant.ofEpochMilli(event.event.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                            val endTime = event.event.endTimeMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() } ?: startTime.plusHours(1)
                            
                            val startMinutes = startTime.hour * 60 + startTime.minute
                            val endMinutes = if (endTime.isBefore(startTime) && endTime.hour == 0) 24 * 60 else endTime.hour * 60 + endTime.minute
                            val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(15)

                            val startOffset = (startMinutes / 60f) * hourHeight.value
                            val eventHeight = (durationMinutes / 60f) * hourHeight.value

                            val color = event.category?.color?.let { Color(it) } ?: event.event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 1.dp)
                                    .absoluteOffset(y = startOffset.dp)
                                    .height(eventHeight.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.8f))
                                    .clickable { onEventClick(event) }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = event.event.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Current time indicator
                        if (day == LocalDate.now()) {
                            val now = LocalTime.now()
                            val nowOffset = ((now.hour * 60 + now.minute) / 60f) * hourHeight.value
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(y = nowOffset.dp)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
    }
}
