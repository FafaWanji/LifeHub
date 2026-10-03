package com.example.lifeorganizer.calendar.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.lifeorganizer.calendar.data.EventWithReminders
import java.time.LocalDate

@Composable
fun rememberDragDropState(): DragDropState {
    return remember { DragDropState() }
}

class DragDropState {
    var isDragging by mutableStateOf(false)
    var draggingEvent by mutableStateOf<EventWithReminders?>(null)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    
    // Maps a LocalDate to its bounding box on screen
    val dayCellBounds = mutableMapOf<LocalDate, Rect>()

    fun onDragStart(event: EventWithReminders, position: Offset) {
        isDragging = true
        draggingEvent = event
        dragPosition = position
        dragOffset = Offset.Zero
    }

    fun onDrag(offset: Offset) {
        dragOffset += offset
    }

    fun onDragEnd(onReschedule: (EventWithReminders, LocalDate) -> Unit) {
        if (isDragging) {
            val dropTarget = findDropTarget(dragPosition + dragOffset)
            if (dropTarget != null && draggingEvent != null) {
                onReschedule(draggingEvent!!, dropTarget)
            }
        }
        isDragging = false
        draggingEvent = null
        dragOffset = Offset.Zero
    }

    fun onDragCancel() {
        isDragging = false
        draggingEvent = null
        dragOffset = Offset.Zero
    }

    private fun findDropTarget(position: Offset): LocalDate? {
        for ((date, bounds) in dayCellBounds) {
            if (bounds.contains(position)) {
                return date
            }
        }
        return null
    }
}
