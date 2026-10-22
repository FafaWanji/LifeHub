package com.example.lifeorganizer.habits.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.habits.TaskActions
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.rememberTaskSettings

/**
 * The task dialog on its own, e.g. over the calendar when a deadline is tapped – no switch to the
 * habits & tasks tab. Deleting always asks first, since there is no undo here.
 */
@Composable
fun TaskEditorOverlay(taskId: Long, onClose: () -> Unit, vm: HabitsViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val ts = rememberTaskSettings()
    val confirmOnTimeClaim by vm.confirmOnTimeClaim.collectAsStateWithLifecycle()
    val onTimeClaimUsed by vm.onTimeClaimUsed.collectAsStateWithLifecycle()
    var claimTask by remember { mutableStateOf<TaskItem?>(null) }
    var confirmDelete by remember { mutableStateOf<TaskItem?>(null) }
    // Still loading (or deleted meanwhile): nothing to show yet
    val task = tasks.firstOrNull { it.id == taskId }

    if (task != null && claimTask == null && confirmDelete == null) {
        TaskDialog(
            initial = task,
            onDismiss = onClose,
            onSave = { vm.saveTask(it); onClose() },
            onClaimOnTime = if (TaskActions.canClaimOnTime(task, ts)) ({ claimTask = task }) else null,
            onDelete = { confirmDelete = task }
        )
    }
    confirmDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(HabitsStr.deleteTaskTitle.text()) },
            text = { Text(t.title + if (ts.showPoints) "\n\n" + HabitsStr.deleteTaskBody.text() else "") },
            confirmButton = { TextButton(onClick = { vm.deleteTask(t); confirmDelete = null; onClose() }) { Text(HabitsStr.delete.text()) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(HabitsStr.cancel.text()) } }
        )
    }
    claimTask?.let { t ->
        ClaimOnTimeDialogs(
            task = t, points = ts.points, ask = confirmOnTimeClaim, firstTime = !onTimeClaimUsed,
            onConfirm = { dontAskAgain -> vm.claimOnTime(t, dontAskAgain); claimTask = null; onClose() },
            onDismiss = { claimTask = null }
        )
    }
}
