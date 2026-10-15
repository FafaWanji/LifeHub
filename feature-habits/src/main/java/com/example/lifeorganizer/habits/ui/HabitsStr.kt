package com.example.lifeorganizer.habits.ui

import com.example.lifeorganizer.core.i18n.Txt

object HabitsStr {
    val today = Txt("Today", "Heute", "Bugün", "Hoy", "今天")
    val habits = Txt("Habits", "Gewohnheiten", "Alışkanlıklar", "Hábitos", "习惯")
    val tasks = Txt("Tasks", "Aufgaben", "Görevler", "Tareas", "任务")
    val newHabit = Txt("New habit", "Neue Gewohnheit", "Yeni alışkanlık", "Nuevo hábito", "新建习惯")
    val editHabit = Txt("Edit habit", "Gewohnheit bearbeiten", "Alışkanlığı düzenle", "Editar hábito", "编辑习惯")
    val newTask = Txt("New task", "Neue Aufgabe", "Yeni görev", "Nueva tarea", "新建任务")
    val editTask = Txt("Edit task", "Aufgabe bearbeiten", "Görevi düzenle", "Editar tarea", "编辑任务")
    val title = Txt("Title", "Titel", "Başlık", "Título", "标题")
    val note = Txt("Note", "Notiz", "Not", "Nota", "备注")
    val weekdays = Txt("On these days", "An diesen Tagen", "Bu günlerde", "Estos días", "在这些日子")
    val everyDay = Txt("Every day", "Jeden Tag", "Her gün", "Cada día", "每天")
    val reminder = Txt("Reminder", "Erinnerung", "Hatırlatıcı", "Recordatorio", "提醒")
    val noReminder = Txt("No reminder", "Keine Erinnerung", "Hatırlatıcı yok", "Sin recordatorio", "无提醒")
    val deadline = Txt("Deadline", "Deadline", "Son tarih", "Fecha límite", "截止日期")
    val noDeadline = Txt("No deadline", "Ohne Deadline", "Son tarih yok", "Sin fecha límite", "无截止日期")
    val removeDeadline = Txt("Remove deadline", "Deadline entfernen", "Son tarihi kaldır", "Quitar fecha límite", "移除截止日期")
    val overdue = Txt("Overdue", "Überfällig", "Gecikmiş", "Vencidas", "已逾期")
    val dueToday = Txt("Due today", "Heute fällig", "Bugün son gün", "Vencen hoy", "今天到期")
    val soon = Txt("Next 7 days", "Nächste 7 Tage", "Önümüzdeki 7 gün", "Próximos 7 días", "未来 7 天")
    val later = Txt("Later", "Später", "Daha sonra", "Más adelante", "以后")
    val done = Txt("Done", "Erledigt", "Tamamlandı", "Hecho", "已完成")
    val markDone = Txt("Done", "Erledigt", "Tamamlandı", "Hecho", "完成")
    val streakDays = Txt("%d days in a row", "%d Tage in Folge", "Art arda %d gün", "%d días seguidos", "连续 %d 天")
    val nothingToday = Txt(
        "Nothing due today. Add a habit or a task with +.",
        "Heute steht nichts an. Lege mit + eine Gewohnheit oder Aufgabe an.",
        "Bugün yapılacak bir şey yok. + ile alışkanlık veya görev ekle.",
        "Nada pendiente hoy. Añade un hábito o una tarea con +.",
        "今天没有待办。用 + 添加习惯或任务。"
    )
    val noHabits = Txt(
        "No habits yet – e.g. \"Take medication\" every day with a reminder.",
        "Noch keine Gewohnheiten – z. B. „Medikamente nehmen“ jeden Tag mit Erinnerung.",
        "Henüz alışkanlık yok – ör. her gün hatırlatıcıyla \"İlaç al\".",
        "Aún no hay hábitos – p. ej. \"Tomar la medicación\" cada día con recordatorio.",
        "还没有习惯——例如每天提醒「吃药」。"
    )
    val noTasks = Txt(
        "No tasks yet – e.g. \"Exercise 5\" with a deadline.",
        "Noch keine Aufgaben – z. B. „Aufgabe 5 machen“ mit Deadline.",
        "Henüz görev yok – ör. son tarihli \"5. alıştırma\".",
        "Aún no hay tareas – p. ej. \"Ejercicio 5\" con fecha límite.",
        "还没有任务——例如带截止日期的「第 5 题」。"
    )
    val notToday = Txt("Not due today", "Heute nicht dran", "Bugün değil", "Hoy no toca", "今天不需要")
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar", "保存")
    val cancel = Txt("Cancel", "Abbrechen", "İptal", "Cancelar", "取消")
    val delete = Txt("Delete", "Löschen", "Sil", "Eliminar", "删除")
    val undo = Txt("Undo", "Rückgängig", "Geri al", "Deshacer", "撤销")
    val taskDeleted = Txt("Task deleted", "Aufgabe gelöscht", "Görev silindi", "Tarea eliminada", "任务已删除")
    val habitDeleted = Txt("Habit deleted", "Gewohnheit gelöscht", "Alışkanlık silindi", "Hábito eliminado", "习惯已删除")
    val channel = Txt("Habits & tasks", "Gewohnheiten & Aufgaben", "Alışkanlıklar ve görevler", "Hábitos y tareas", "习惯与任务")
    val habitReminderText = Txt("Time for your habit", "Zeit für deine Gewohnheit", "Alışkanlığının zamanı", "Hora de tu hábito", "该做这个习惯了")
    val taskReminderText = Txt("Due today", "Heute fällig", "Bugün son gün", "Vence hoy", "今天到期")
    /** One letter per weekday, Monday first. */
    val weekdayLetters = Txt("MTWTFSS", "MDMDFSS", "PSÇPCCP", "LMXJVSD", "一二三四五六日")
}
