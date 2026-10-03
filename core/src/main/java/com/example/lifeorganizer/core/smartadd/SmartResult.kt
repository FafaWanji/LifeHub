package com.example.lifeorganizer.core.smartadd

sealed class SmartResult {
    data class Event(
        val title: String,
        val description: String,
        val location: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val reminderMinutesBefore: List<Int> = emptyList(),
        val categoryId: Long? = null,
        val isBirthday: Boolean = false,
        val birthYear: Int? = null
    ) : SmartResult()

    data class Note(
        val title: String,
        val content: String,
        val isChecklist: Boolean = false,
        val colorLabel: Int? = null
    ) : SmartResult()
}
