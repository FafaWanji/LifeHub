package com.example.lifeorganizer.calendar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.lifeorganizer.calendar.alarm.AlarmScheduler
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.data.Category
import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.EventColor
import com.example.lifeorganizer.calendar.data.EventWithReminders
import com.example.lifeorganizer.calendar.data.RecurrenceType
import com.example.lifeorganizer.calendar.data.Reminder
import com.example.lifeorganizer.calendar.debug.DebugLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.lifeorganizer.calendar.device.DeviceCalendar
import com.example.lifeorganizer.calendar.util.SeriesRules
import com.example.lifeorganizer.calendar.util.SeriesScope
import androidx.work.*
import com.example.lifeorganizer.calendar.worker.TravelTimeWorker
import java.util.concurrent.TimeUnit
import com.example.lifeorganizer.calendar.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import com.example.lifeorganizer.calendar.util.RRuleExpander
import androidx.glance.appwidget.updateAll

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val eventDao = db.eventDao()
    private val alarmScheduler = AlarmScheduler(application)
    private val workManager = WorkManager.getInstance(application)
    private val settingsManager = SettingsManager(application)

    init {
        // Arm reminders of repeating events for their next occurrence.
        viewModelScope.launch(Dispatchers.IO) {
            removeDuplicateSeries()
            com.example.lifeorganizer.calendar.alarm.RecurringAlarmSync.sync(application)
        }
        viewModelScope.launch {
            eventDao.getEventsWithReminders().collect {
                try {
                    com.example.lifeorganizer.calendar.widget.CalendarWidget().updateAll(application)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedDateMillis = MutableStateFlow<Long?>(null)

    val categories: StateFlow<List<Category>> = eventDao.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventTemplates: StateFlow<List<com.example.lifeorganizer.calendar.data.EventTemplate>> = eventDao.getEventTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Events of the phone's calendars (read-only), reloaded when they change. */
    private val deviceEvents = MutableStateFlow<List<EventWithReminders>>(emptyList())
    val deviceCalendarEnabled = MutableStateFlow(DeviceCalendar.isEnabled(application))

    fun reloadDeviceCalendar() {
        viewModelScope.launch(Dispatchers.IO) {
            deviceEvents.value = DeviceCalendar.load(getApplication(), LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))
        }
    }

    fun setDeviceCalendarEnabled(enabled: Boolean) {
        DeviceCalendar.setEnabled(getApplication(), enabled)
        deviceCalendarEnabled.value = enabled
        reloadDeviceCalendar()
    }

    private val deviceObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = reloadDeviceCalendar()
    }

    init {
        application.contentResolver.registerContentObserver(android.provider.CalendarContract.Events.CONTENT_URI, true, deviceObserver)
        reloadDeviceCalendar()
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(deviceObserver)
        super.onCleared()
    }

    val eventsWithReminders: StateFlow<List<EventWithReminders>> = eventDao.getEventsWithReminders()
        .map { events ->
            val rangeStart = LocalDate.now().minusYears(2)
            val rangeEnd = LocalDate.now().plusYears(5)
            events.flatMap { event ->
                if (!event.event.recurrenceRule.isNullOrBlank()) {
                    RRuleExpander.expand(event, rangeStart, rangeEnd)
                } else {
                    listOf(event)
                }
            }
        }
        .combine(deviceEvents) { own, device -> own + device }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Stored events, without expanded recurrences. Edits, deletes and moves must target these:
     * an expanded occurrence carries a shifted start and virtual reminders (id 0).
     */
    val masterEvents: StateFlow<List<EventWithReminders>> = eventDao.getEventsWithReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredEvents: StateFlow<List<EventWithReminders>> = combine(
        eventsWithReminders,
        _searchQuery
    ) { events, query ->
        if (query.isBlank()) events
        else events.filter { it.event.title.contains(query, ignoreCase = true) ||
            it.event.description.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeAddress: StateFlow<String?> = settingsManager.homeAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val travelMode: StateFlow<String> = settingsManager.travelMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "driving")

    val useHomeAsOrigin: StateFlow<Boolean> = settingsManager.useHomeAsOrigin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val useBrowserForMaps: StateFlow<Boolean> = settingsManager.useBrowserForMaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val languageCode: StateFlow<String> = settingsManager.languageCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val isDarkTheme: StateFlow<Boolean?> = settingsManager.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val onboardingCompleted: StateFlow<Boolean> = settingsManager.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationSoundUri: StateFlow<String?> = settingsManager.notificationSoundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val geminiApiKey: StateFlow<String?> = settingsManager.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val enableSmartAlarms: StateFlow<Boolean> = settingsManager.enableSmartAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val enableAppLock: StateFlow<Boolean> = settingsManager.enableAppLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val useSystemAlarmForReminders: StateFlow<Boolean> = settingsManager.useSystemAlarmForReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val accentColor: StateFlow<Int?> = settingsManager.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val designStyle: StateFlow<String> = settingsManager.designStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "pastel")

    fun checkConflict(startTimeMillis: Long, endTimeMillis: Long?, excludeEventId: Long? = null): EventWithReminders? {
        val allEvents = eventsWithReminders.value
        val end = endTimeMillis ?: (startTimeMillis + 3600000) // Default 1 hour duration if no end time
        
        return allEvents.firstOrNull { eventWithReminders ->
            val e = eventWithReminders.event
            if (e.id == excludeEventId) return@firstOrNull false
            if (e.isAllDay) return@firstOrNull false
            
            val eStart = e.startTimeMillis
            val eEnd = e.endTimeMillis ?: (eStart + 3600000)
            
            // Conflict logic: starts before other ends AND ends after other starts
            startTimeMillis < eEnd && end > eStart
        }
    }

    fun saveHomeAddress(address: String) {
        viewModelScope.launch {
            settingsManager.saveHomeAddress(address)
            DebugLogger.log("Home address saved: $address")
        }
    }

    fun saveTravelMode(mode: String) {
        viewModelScope.launch {
            settingsManager.saveTravelMode(mode)
            DebugLogger.log("Travel mode saved: $mode")
        }
    }

    fun saveGeminiApiKey(key: String?) {
        viewModelScope.launch {
            settingsManager.saveGeminiApiKey(key)
        }
    }

    fun saveEnableSmartAlarms(enable: Boolean) {
        viewModelScope.launch {
            settingsManager.saveEnableSmartAlarms(enable)
        }
    }

    fun saveUseSystemAlarmForReminders(enable: Boolean) {
        viewModelScope.launch {
            settingsManager.saveUseSystemAlarmForReminders(enable)
        }
    }

    fun saveEnableAppLock(enable: Boolean) {
        viewModelScope.launch {
            settingsManager.saveEnableAppLock(enable)
        }
    }

    fun saveAccentColor(color: Int?) {
        viewModelScope.launch {
            settingsManager.saveAccentColor(color)
        }
    }

    fun saveDesignStyle(style: String) {
        viewModelScope.launch {
            settingsManager.saveDesignStyle(style)
        }
    }

    /**
     * Repairs calendars filled by the old .ics export: every occurrence of a series had been
     * imported as its own series, so each week showed one copy more. Keeps the earliest series.
     */
    private suspend fun removeDuplicateSeries() {
        val all = eventDao.getEventsWithRemindersSync()
        val dropIds = com.example.lifeorganizer.calendar.util.SeriesDedup.duplicates(all.map { it.event }).map { it.id }.toSet()
        if (dropIds.isEmpty()) return
        all.filter { it.event.id in dropIds }.forEach { e ->
            e.reminders.forEach { alarmScheduler.cancelAll(it) }
            eventDao.deleteEvent(e.event)
        }
        DebugLogger.log("Removed ${dropIds.size} duplicate series copies")
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            val reminders = eventDao.getRemindersForEvent(event.id)
            reminders.forEach { alarmScheduler.cancelAll(it) }
            eventDao.deleteEvent(event)
            DebugLogger.log("Deleted event: ${event.title}")
        }
    }

    fun addCategory(name: String, color: Int) {
        viewModelScope.launch {
            eventDao.insertCategory(Category(name = name, color = color))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            eventDao.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            eventDao.deleteCategory(category)
        }
    }

    fun saveEventTemplate(
        name: String,
        title: String,
        description: String,
        isAllDay: Boolean,
        targetAddress: String?,
        arrivalBufferMinutes: Int,
        alarmLeadMinutes: Int,
        categoryId: Long?,
        recurrenceRule: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            eventDao.insertEventTemplate(
                com.example.lifeorganizer.calendar.data.EventTemplate(
                    name = name,
                    title = title,
                    description = description,
                    isAllDay = isAllDay,
                    targetAddress = targetAddress,
                    arrivalBufferMinutes = arrivalBufferMinutes,
                    alarmLeadMinutes = alarmLeadMinutes,
                    categoryId = categoryId,
                    recurrenceRule = recurrenceRule
                )
            )
        }
    }

    fun deleteEventTemplate(template: com.example.lifeorganizer.calendar.data.EventTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            eventDao.deleteEventTemplate(template)
        }
    }

    fun saveUseHomeAsOrigin(useHome: Boolean) {
        viewModelScope.launch {
            settingsManager.saveUseHomeAsOrigin(useHome)
            DebugLogger.log("Use Home as origin: $useHome")
        }
    }

    fun saveUseBrowserForMaps(useBrowser: Boolean) {
        viewModelScope.launch {
            settingsManager.saveUseBrowserForMaps(useBrowser)
            DebugLogger.log("Use Browser for Maps: $useBrowser")
        }
    }

    fun saveLanguageCode(code: String) {
        viewModelScope.launch {
            settingsManager.saveLanguageCode(code)
            DebugLogger.log("Language changed to: $code")
        }
    }

    fun saveDarkTheme(isDark: Boolean?) {
        viewModelScope.launch {
            settingsManager.saveIsDarkTheme(isDark)
            DebugLogger.log("Theme changed to: ${isDark ?: "system"}")
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsManager.saveOnboardingCompleted(true)
            DebugLogger.log("Onboarding completed")
        }
    }

    fun saveNotificationSoundUri(uri: String?) {
        viewModelScope.launch {
            settingsManager.saveNotificationSoundUri(uri)
            DebugLogger.log("Notification sound: ${uri ?: "default"}")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEvent(
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long? = null,
        isAllDay: Boolean,
        reminderOffsets: List<Long>,
        targetAddress: String? = null,
        arrivalBuffer: Int = 0,
        alarmLead: Int = 0,
        color: Int? = null,
        recurrenceRule: String? = null,
        timezone: String = java.time.ZoneId.systemDefault().id,
        isBirthday: Boolean = false,
        birthYear: Int? = null,
        categoryId: Long? = null,
        linkedNoteId: Long? = null
    ) {
        viewModelScope.launch {
            var finalCategoryId = categoryId
            if (isBirthday && finalCategoryId == null) {
                finalCategoryId = getOrCreateBirthdayCategory()
            }

            val event = Event(
                linkedNoteId = linkedNoteId,
                title = title,
                description = description,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                isAllDay = isAllDay,
                targetAddress = com.example.lifeorganizer.core.util.Places.clean(targetAddress),
                arrivalBufferMinutes = arrivalBuffer,
                alarmLeadMinutes = alarmLead,
                color = color,
                recurrenceRule = recurrenceRule,
                timezone = timezone,
                isBirthday = isBirthday,
                birthYear = birthYear,
                categoryId = finalCategoryId
            )
            val eventId = eventDao.insertEvent(event)

            val reminders = if (isBirthday) {
                // Birthday: add silent 23:59 reminder (1 min before) plus any user reminders
                val birthdayReminder = Reminder(
                    eventId = eventId,
                    reminderTimeMillis = startTimeMillis - 60_000L, // 23:59 day before
                    type = "Birthday at 23:59",
                    timezone = timezone
                )
                listOf(birthdayReminder) + reminderOffsets.map { offset ->
                    Reminder(
                        eventId = eventId,
                        reminderTimeMillis = startTimeMillis - offset,
                        type = formatOffset(offset),
                        timezone = timezone
                    )
                }
            } else {
                reminderOffsets.map { offset ->
                    Reminder(
                        eventId = eventId,
                        reminderTimeMillis = startTimeMillis - offset,
                        type = formatOffset(offset),
                        timezone = timezone
                    )
                }
            }
            eventDao.insertReminders(reminders)

            val savedReminders = eventDao.getRemindersForEvent(eventId)
            savedReminders.forEach { reminder ->
                alarmScheduler.schedule(event.copy(id = eventId), reminder)

                // Skip system alarm for silent 23:59 birthday reminders
                if (reminder.type != "Birthday at 23:59") {
                    if (useSystemAlarmForReminders.value) {
                        val currentTime = System.currentTimeMillis()
                        val isWithin24Hours = (reminder.reminderTimeMillis - currentTime) < (24 * 60 * 60 * 1000L)

                        if (isWithin24Hours) {
                            val reminderDateTime = java.time.Instant.ofEpochMilli(reminder.reminderTimeMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                            com.example.lifeorganizer.calendar.alarm.SystemAlarmScheduler.createSystemAlarm(
                                context = getApplication(),
                                hour = reminderDateTime.hour,
                                minute = reminderDateTime.minute,
                                label = "$title (${reminder.type})"
                            )
                        } else {
                            alarmScheduler.schedule(event.copy(id = eventId), reminder, isSystemAlarmTrigger = true)
                            DebugLogger.log("System alarm for $title deferred (>24h away)")
                        }
                    }
                }
            }

            if (!targetAddress.isNullOrBlank()) {
                scheduleTravelTimeWorker(eventId, startTimeMillis)
            }

            if (!recurrenceRule.isNullOrBlank()) com.example.lifeorganizer.calendar.alarm.RecurringAlarmSync.sync(getApplication())
            DebugLogger.log("Event added: $title")
        }
    }

    private fun scheduleTravelTimeWorker(eventId: Long, startTimeMillis: Long) {
        // Immediate estimate so the card can show "leave at …" right away.
        workManager.enqueueUniqueWork(
            "travel_estimate_$eventId",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<TravelTimeWorker>()
                .setInputData(workDataOf(TravelTimeWorker.KEY_EVENT_ID to eventId, TravelTimeWorker.KEY_SCHEDULE_ALARM to false))
                .setConstraints(androidx.work.Constraints(requiredNetworkType = androidx.work.NetworkType.CONNECTED))
                .build()
        )

        if (!enableSmartAlarms.value) return

        // Refresh with live traffic 2 hours before the event and set the "leave now" alarm.
        val triggerTime = startTimeMillis - (2 * 60 * 60 * 1000L)
        val initialDelay = (triggerTime - System.currentTimeMillis()).coerceAtLeast(0L)

        val workRequest = OneTimeWorkRequestBuilder<TravelTimeWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TravelTimeWorker.KEY_EVENT_ID to eventId, TravelTimeWorker.KEY_SCHEDULE_ALARM to true))
            .build()

        workManager.enqueueUniqueWork(
            "travel_time_$eventId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        DebugLogger.log("Scheduled travel time check for event $eventId")
    }

    fun updateEvent(
        eventWithReminders: EventWithReminders,
        newTitle: String,
        newDescription: String,
        newStartTime: Long,
        newEndTime: Long? = null,
        isAllDay: Boolean,
        newOffsets: List<Long>,
        targetAddress: String? = null,
        arrivalBuffer: Int = 0,
        alarmLead: Int = 0,
        color: Int? = null,
        recurrenceRule: String? = null,
        timezone: String = java.time.ZoneId.systemDefault().id,
        isBirthday: Boolean = false,
        birthYear: Int? = null,
        categoryId: Long? = null
    ) {
        viewModelScope.launch {
            // Cancel old alarms (both notification and system trigger)
            eventWithReminders.reminders.forEach { alarmScheduler.cancelAll(it) }
            // Cancel old travel time worker
            workManager.cancelUniqueWork("travel_time_${eventWithReminders.event.id}")
            workManager.cancelUniqueWork("travel_estimate_${eventWithReminders.event.id}")
            com.example.lifeorganizer.calendar.worker.TravelInfoStore.remove(getApplication(), eventWithReminders.event.id)

            var finalCategoryId = categoryId
            if (isBirthday && finalCategoryId == null) {
                finalCategoryId = getOrCreateBirthdayCategory()
            }

            val updatedEvent = eventWithReminders.event.copy(
                title = newTitle,
                description = newDescription,
                startTimeMillis = newStartTime,
                endTimeMillis = newEndTime,
                isAllDay = isAllDay,
                targetAddress = com.example.lifeorganizer.core.util.Places.clean(targetAddress),
                arrivalBufferMinutes = arrivalBuffer,
                alarmLeadMinutes = alarmLead,
                color = color,
                recurrenceRule = recurrenceRule,
                timezone = timezone,
                isBirthday = isBirthday,
                birthYear = birthYear,
                categoryId = finalCategoryId
            )

            val newReminders = if (isBirthday) {
                val birthdayReminder = Reminder(
                    eventId = updatedEvent.id,
                    reminderTimeMillis = newStartTime - 60_000L, // 23:59 day before
                    type = "Birthday at 23:59",
                    timezone = timezone
                )
                listOf(birthdayReminder) + newOffsets.map { offset ->
                    Reminder(
                        eventId = updatedEvent.id,
                        reminderTimeMillis = newStartTime - offset,
                        type = formatOffset(offset),
                        timezone = timezone
                    )
                }
            } else {
                newOffsets.map { offset ->
                    Reminder(
                        eventId = updatedEvent.id,
                        reminderTimeMillis = newStartTime - offset,
                        type = formatOffset(offset),
                        timezone = timezone
                    )
                }
            }

            eventDao.updateEventWithReminders(updatedEvent, newReminders)

            val savedReminders = eventDao.getRemindersForEvent(updatedEvent.id)
            savedReminders.forEach { reminder ->
                alarmScheduler.schedule(updatedEvent, reminder)

                // Skip system alarm for silent 23:59 birthday reminders
                if (reminder.type != "Birthday at 23:59") {
                    if (useSystemAlarmForReminders.value) {
                        val currentTime = System.currentTimeMillis()
                        val isWithin24Hours = (reminder.reminderTimeMillis - currentTime) < (24 * 60 * 60 * 1000L)

                        if (isWithin24Hours) {
                            val reminderDateTime = java.time.Instant.ofEpochMilli(reminder.reminderTimeMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                            com.example.lifeorganizer.calendar.alarm.SystemAlarmScheduler.createSystemAlarm(
                                context = getApplication(),
                                hour = reminderDateTime.hour,
                                minute = reminderDateTime.minute,
                                label = "$newTitle (${reminder.type})"
                            )
                        } else {
                            alarmScheduler.schedule(updatedEvent, reminder, isSystemAlarmTrigger = true)
                            DebugLogger.log("System alarm for $newTitle deferred (>24h away)")
                        }
                    }
                }
            }

            if (!targetAddress.isNullOrBlank()) {
                scheduleTravelTimeWorker(updatedEvent.id, newStartTime)
            }

            if (!recurrenceRule.isNullOrBlank()) com.example.lifeorganizer.calendar.alarm.RecurringAlarmSync.sync(getApplication())
            DebugLogger.log("Event updated: $newTitle")
        }
    }

    fun deleteEvent(eventWithReminders: EventWithReminders) {
        viewModelScope.launch {
            eventWithReminders.reminders.forEach { alarmScheduler.cancelAll(it) }
            workManager.cancelUniqueWork("travel_time_${eventWithReminders.event.id}")
            workManager.cancelUniqueWork("travel_estimate_${eventWithReminders.event.id}")
            com.example.lifeorganizer.calendar.worker.TravelInfoStore.remove(getApplication(), eventWithReminders.event.id)
            eventDao.deleteEvent(eventWithReminders.event)
            DebugLogger.log("Event deleted: ${eventWithReminders.event.title}")
        }
    }

    /**
     * Cuts one occurrence ([SeriesScope.THIS]) or the rest of a series ([SeriesScope.FOLLOWING]) out of
     * [master]. Deleting stops here; editing then adds the changed part as a new event.
     * Returns false when nothing of the series would be left before the cut (caller treats it as ALL).
     */
    fun cutSeries(master: EventWithReminders, occurrenceStart: Long, scope: SeriesScope): Boolean {
        val event = master.event
        val date = SeriesRules.localDate(event, occurrenceStart)
        if (scope == SeriesScope.FOLLOWING && !date.isAfter(SeriesRules.localDate(event))) return false
        val changed = when (scope) {
            SeriesScope.THIS -> SeriesRules.withoutDate(event, date)
            SeriesScope.FOLLOWING -> SeriesRules.endingBefore(event, date)
            SeriesScope.ALL -> return false
        }
        replaceEvent(changed)
        return true
    }

    /** Attaches a Dokki document to an event (null removes it). */
    fun attachDocument(eventId: Long, documentId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            eventDao.getEventsWithRemindersSync().firstOrNull { it.event.id == eventId }?.let {
                eventDao.updateEvent(it.event.copy(documentId = documentId))
            }
        }
    }

    /** Links a note to an event ("note for this event"). */
    fun linkNote(eventId: Long, noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            eventDao.getEventsWithRemindersSync().firstOrNull { it.event.id == eventId }?.let {
                eventDao.updateEvent(it.event.copy(linkedNoteId = noteId))
            }
        }
    }

    /** Stores [event] as it is (series cut or its undo) and re-arms the series' reminders. */
    fun replaceEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            eventDao.updateEvent(event)
            eventDao.getRemindersForEvent(event.id).forEach { alarmScheduler.cancelAll(it) }
            com.example.lifeorganizer.calendar.alarm.RecurringAlarmSync.sync(getApplication())
        }
    }

    /** Undo for a deletion: puts the event back with its original id and re-arms future reminders. */
    fun restoreEvent(eventWithReminders: EventWithReminders) {
        viewModelScope.launch {
            val event = eventWithReminders.event
            eventDao.insertEvent(event)
            eventDao.insertReminders(eventWithReminders.reminders)
            val now = System.currentTimeMillis()
            eventDao.getRemindersForEvent(event.id).filter { it.reminderTimeMillis > now }
                .forEach { alarmScheduler.schedule(event, it) }
            if (!event.targetAddress.isNullOrBlank()) scheduleTravelTimeWorker(event.id, event.startTimeMillis)
            DebugLogger.log("Event restored: ${event.title}")
        }
    }

    fun duplicateEvent(eventWithReminders: EventWithReminders, newStartTimeMillis: Long) {
        viewModelScope.launch {
            val original = eventWithReminders.event
            val offset = newStartTimeMillis - original.startTimeMillis
            val newEvent = original.copy(
                id = 0, // new ID
                startTimeMillis = newStartTimeMillis,
                endTimeMillis = original.endTimeMillis?.let { it + offset }
            )
            val eventId = eventDao.insertEvent(newEvent)

            val newReminders = eventWithReminders.reminders.map { reminder ->
                Reminder(
                    eventId = eventId,
                    reminderTimeMillis = newStartTimeMillis - (original.startTimeMillis - reminder.reminderTimeMillis),
                    type = reminder.type,
                    timezone = reminder.timezone
                )
            }
            eventDao.insertReminders(newReminders)

            val savedReminders = eventDao.getRemindersForEvent(eventId)
            savedReminders.forEach { alarmScheduler.schedule(newEvent.copy(id = eventId), it) }

            if (!newEvent.targetAddress.isNullOrBlank()) {
                scheduleTravelTimeWorker(eventId, newStartTimeMillis)
            }

            DebugLogger.log("Event duplicated: ${newEvent.title}")
        }
    }

    fun getUpcomingEvents(limit: Int = 5): StateFlow<List<EventWithReminders>> {
        return eventsWithReminders.map { list ->
            val now = System.currentTimeMillis()
            list.filter { e ->
                val end = e.event.endTimeMillis ?: (e.event.startTimeMillis + 3600000)
                if (e.event.isAllDay) {
                    val endOfDay = java.time.Instant.ofEpochMilli(e.event.startTimeMillis)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate().plusDays(1)
                        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    endOfDay > now
                } else {
                    end > now
                }
            }
                .sortedBy { it.event.startTimeMillis }
                .distinctBy { it.event.id } // a series appears once, with its next date
                .take(limit)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private fun formatOffset(offset: Long): String {
        return when (offset) {
            0L -> "Exact start time"
            30 * 60 * 1000L -> "30 mins before"
            60 * 60 * 1000L -> "1 hour before"
            24 * 60 * 60 * 1000L -> "1 day before"
            7 * 24 * 60 * 60 * 1000L -> "1 week before"
            else -> {
                val mins = offset / 60000
                when {
                    mins < 60 -> "$mins mins before"
                    mins < 1440 -> "${mins / 60} hours before"
                    else -> "${mins / 1440} days before"
                }
            }
        }
    }

    fun importEventsFromUri(context: Context, uri: Uri, onResult: (Int, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (content.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onResult(0, 0) }
                    return@launch
                }
                val importedEvents = com.example.lifeorganizer.calendar.util.IcsImporter.parseIcs(content)
                val currentEvents = eventsWithReminders.value.map { it.event }
                var importedCount = 0
                var skippedCount = 0
                
                for (eventWithReminders in importedEvents) {
                    val event = eventWithReminders.event.let { it.copy(targetAddress = com.example.lifeorganizer.core.util.Places.clean(it.targetAddress)) }
                    // Duplicate check: title and start time match
                    val isDuplicate = currentEvents.any { it.title == event.title && it.startTimeMillis == event.startTimeMillis }
                    if (isDuplicate) {
                        skippedCount++
                        continue
                    }
                    val eventId = eventDao.insertEvent(event)
                    val reminders = eventWithReminders.reminders.map { it.copy(eventId = eventId) }
                    eventDao.insertReminders(reminders)
                    // Schedule with the stored ids – unsaved reminders all have id 0 and would overwrite each other.
                    val now = System.currentTimeMillis()
                    eventDao.getRemindersForEvent(eventId).filter { it.reminderTimeMillis > now }
                        .forEach { alarmScheduler.schedule(event.copy(id = eventId), it) }
                    importedCount++
                }
                withContext(Dispatchers.Main) {
                    onResult(importedCount, skippedCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(0, 0)
                }
            }
        }
    }

    private suspend fun getOrCreateBirthdayCategory(): Long {
        val bdayCat = eventDao.getCategoryByNameSync("Birthdays")
            ?: eventDao.getCategoryByNameSync("Geburtstage")
        if (bdayCat != null) return bdayCat.id

        val newCat = com.example.lifeorganizer.calendar.data.Category(name = "Geburtstage", color = android.graphics.Color.parseColor("#FF4081")) // Pink
        return eventDao.insertCategory(newCat)
    }
}