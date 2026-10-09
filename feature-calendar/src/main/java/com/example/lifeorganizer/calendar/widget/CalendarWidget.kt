package com.example.lifeorganizer.calendar.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.background
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.util.RRuleExpander
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CalendarWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        val configuredCategoryIds = CalendarWidgetConfigActivity.loadWidgetConfig(context, appWidgetId)
        
        val events = loadAgendaEvents(context, configuredCategoryIds)
        // The widget follows the in-app language, like the rest of LifeHub
        val lang = com.example.lifeorganizer.core.settings.SettingsManager(context).languageCode.first()

        provideContent {
            WidgetContent(events, lang)
        }
    }

    @Composable
    private fun WidgetContent(events: List<WidgetEvent>, lang: String) {
        val dateFormatter = com.example.lifeorganizer.core.i18n.localDateFormatter(lang, "EEEMMMdd")
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1C1B1F))) // Dark theme background
                .padding(12.dp)
                .clickable(actionRunCallback<OpenAppAction>()),
            verticalAlignment = Alignment.Vertical.Top
        ) {

            if (events.isEmpty()) {
                Text(
                    text = com.example.lifeorganizer.core.i18n.Str.noUpcoming.of(lang),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(Color(0xFFCAC4D0))
                    )
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    // Group by date
                    val grouped = events.groupBy { 
                        Instant.ofEpochMilli(it.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    
                    grouped.forEach { (date, dayEvents) ->
                        val today = LocalDate.now()
                        val daysLeft = ChronoUnit.DAYS.between(today, date)
                        val S = com.example.lifeorganizer.core.i18n.Str
                        val wl = lang
                        val relativeText = when {
                            daysLeft == 0L -> " (${S.widgetToday.of(wl)})"
                            daysLeft == 1L -> " (${S.widgetTomorrow.of(wl)})"
                            daysLeft > 1L -> " (${S.widgetInDays.of(wl).format(daysLeft)})"
                            else -> ""
                        }

                        item {
                            Text(
                                text = date.format(dateFormatter) + relativeText,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(Color(0xFFE6E0E9))
                                ),
                                modifier = GlanceModifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        
                        items(dayEvents) { event ->
                            val timeText = if (event.isAllDay) {
                                com.example.lifeorganizer.core.i18n.Str.allDay.of(lang)
                            } else {
                                Instant.ofEpochMilli(event.startTimeMillis)
                                    .atZone(ZoneId.of(event.timezone))
                                    .toLocalTime()
                                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                            }
                            
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable(actionRunCallback<OpenAppAction>()),
                                horizontalAlignment = Alignment.Horizontal.Start
                            ) {
                                // A small colored circle for category
                                Box(
                                    modifier = GlanceModifier
                                        .size(8.dp)
                                        .background(ColorProvider(Color(event.color)))
                                ) { }
                                
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                
                                Text(
                                    text = "${event.title} — $timeText",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = ColorProvider(Color(0xFFCAC4D0))
                                    )
                                )
                            }

                        }
                    }
                }
            }
        }
    }

    private suspend fun loadAgendaEvents(context: Context, categoryIds: Set<Long>): List<WidgetEvent> {
        val db = AppDatabase.getDatabase(context)
        val allEventsWithReminders = db.eventDao().getEventsWithRemindersSync()
        val allCategories = db.eventDao().getCategoriesSync()
        val categoryMap = allCategories.associateBy { it.id }
        
        val now = LocalDate.now()
        val startMillis = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = now.plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val expandedEvents = allEventsWithReminders.flatMap { event -> 
            RRuleExpander.expand(event, now, now.plusDays(30))
        }
        
        return expandedEvents
            .filter { categoryIds.isEmpty() || categoryIds.contains(it.event.categoryId ?: -1L) }
            .filter { e ->
                val end = e.event.endTimeMillis ?: (e.event.startTimeMillis + 3600000)
                if (e.event.isAllDay) {
                    val endOfDay = Instant.ofEpochMilli(e.event.startTimeMillis)
                        .atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    endOfDay > System.currentTimeMillis()
                } else {
                    end > System.currentTimeMillis()
                }
            }
            .map { e ->
                val categoryColor = categoryMap[e.event.categoryId]?.color ?: 0xFFD0BCFF.toInt()
                val color = if (e.event.isBirthday) 0xFFFF4081.toInt() else categoryColor
                
                WidgetEvent(
                    id = e.event.id,
                    title = e.event.title,
                    startTimeMillis = e.event.startTimeMillis,
                    isAllDay = e.event.isAllDay,
                    isBirthday = e.event.isBirthday,
                    birthYear = e.event.birthYear,
                    timezone = e.event.timezone,
                    color = color
                )
            }
            .sortedBy { it.startTimeMillis }
            .take(20) // Limit to next 20 events
    }

    data class WidgetEvent(
        val id: Long,
        val title: String,
        val startTimeMillis: Long,
        val isAllDay: Boolean,
        val isBirthday: Boolean,
        val birthYear: Int?,
        val timezone: String,
        val color: Int
    )
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, Class.forName("com.example.lifeorganizer.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_agenda", true)
        }
        context.startActivity(intent)
    }
}

