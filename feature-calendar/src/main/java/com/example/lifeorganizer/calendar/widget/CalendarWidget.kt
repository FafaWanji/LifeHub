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
import androidx.glance.appwidget.cornerRadius
import com.example.lifeorganizer.calendar.R
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
        val S = com.example.lifeorganizer.core.i18n.Str
        val dateFormatter = com.example.lifeorganizer.core.i18n.localDateFormatter(lang, "EEEMMMdd")
        val surface = ColorProvider(R.color.widget_surface)
        val onSurface = ColorProvider(R.color.widget_on_surface)
        val muted = ColorProvider(R.color.widget_muted)
        val accent = ColorProvider(R.color.widget_accent)
        val card = ColorProvider(R.color.widget_chip)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val grouped = events.groupBy { Instant.ofEpochMilli(it.startTimeMillis).atZone(zone).toLocalDate() }
        val open = actionRunCallback<OpenAppAction>()

        fun dayLabel(date: LocalDate): String = when (val d = ChronoUnit.DAYS.between(today, date)) {
            0L -> S.widgetToday.of(lang)
            1L -> S.widgetTomorrow.of(lang)
            else -> S.widgetInDays.of(lang).format(d).replaceFirstChar { it.uppercase() }
        }

        Column(
            modifier = GlanceModifier.fillMaxSize().background(surface).cornerRadius(20.dp).padding(14.dp)
        ) {
            // Header: today's date, "+" for a new event
            Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight().clickable(open)) {
                    Text(S.widgetToday.of(lang), style = TextStyle(color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Text(today.format(dateFormatter), style = TextStyle(color = muted, fontSize = 12.sp))
                }
                Text(
                    "+",
                    style = TextStyle(color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(horizontal = 6.dp).clickable(actionRunCallback<NewEventAction>())
                )
            }

            if (events.isEmpty()) {
                Text(S.noUpcoming.of(lang), style = TextStyle(color = muted, fontSize = 14.sp), modifier = GlanceModifier.clickable(open))
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    grouped.forEach { (date, dayEvents) ->
                        // Today is already in the header; later days get a small label
                        if (date != today) item {
                            Text(
                                dayLabel(date) + " · " + date.format(dateFormatter),
                                style = TextStyle(color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }
                        items(dayEvents) { event ->
                            EventRow(event, lang, now, onSurface, muted, accent, card)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EventRow(
        event: WidgetEvent, lang: String, now: Long,
        onSurface: ColorProvider, muted: ColorProvider, accent: ColorProvider, card: ColorProvider
    ) {
        val S = com.example.lifeorganizer.core.i18n.Str
        val zone = ZoneId.of(event.timezone)
        val hhmm = DateTimeFormatter.ofPattern("HH:mm")
        val running = !event.isAllDay && event.startTimeMillis <= now && now < event.endTimeMillis
        // Second line: running now, otherwise the place (only its first part, addresses get long)
        val subtitle = when {
            running -> nowLabel.of(lang)
            !event.location.isNullOrBlank() -> event.location.substringBefore(',').trim()
            else -> null
        }
        Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().background(card).cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp).clickable(actionRunCallback<OpenAppAction>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = GlanceModifier.width(4.dp).height(34.dp).cornerRadius(2.dp).background(ColorProvider(Color(event.color)))) {}
                Spacer(GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.width(46.dp)) {
                    if (event.isAllDay) {
                        Text(S.allDay.of(lang), maxLines = 2, style = TextStyle(color = muted, fontSize = 11.sp))
                    } else {
                        Text(
                            Instant.ofEpochMilli(event.startTimeMillis).atZone(zone).format(hhmm),
                            style = TextStyle(color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(Instant.ofEpochMilli(event.endTimeMillis).atZone(zone).format(hhmm), style = TextStyle(color = muted, fontSize = 11.sp))
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(event.title, maxLines = 1, style = TextStyle(color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    if (subtitle != null) Text(
                        subtitle, maxLines = 1,
                        style = TextStyle(color = if (running) accent else muted, fontSize = 12.sp, fontWeight = if (running) FontWeight.Bold else FontWeight.Normal)
                    )
                }
            }
        }
    }

    private val nowLabel = com.example.lifeorganizer.core.i18n.Txt("Now", "Jetzt", "Şimdi", "Ahora", "进行中")

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
        
        // Birthdays can be left out in the calendar settings
        val showBirthdays = com.example.lifeorganizer.core.settings.MicroSettings.get(context, com.example.lifeorganizer.core.settings.MicroSettings.WIDGET_BIRTHDAYS)
        return expandedEvents
            .filter { showBirthdays || !it.event.isBirthday }
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
                    endTimeMillis = e.event.endTimeMillis ?: (e.event.startTimeMillis + 3_600_000),
                    location = e.event.targetAddress,
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
        val endTimeMillis: Long,
        val location: String?,
        val isAllDay: Boolean,
        val isBirthday: Boolean,
        val birthYear: Int?,
        val timezone: String,
        val color: Int
    )
}

/** "+" in the widget: opens LifeHub with the new-event dialog. */
class NewEventAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, Class.forName("com.example.lifeorganizer.MainActivity")).apply {
            action = "com.example.lifeorganizer.ACTION_NEW_EVENT"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
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

