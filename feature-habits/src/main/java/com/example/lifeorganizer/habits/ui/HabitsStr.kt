package com.example.lifeorganizer.habits.ui

import com.example.lifeorganizer.core.i18n.Txt

object HabitsStr {
    val today = Txt("Today", "Heute", "Bugün", "Hoy", "今天")
    val habits = Txt("Habits", "Gewohnheiten", "Alışkanlıklar", "Hábitos", "习惯")
    val todos = Txt("To-Dos")
    val newHabit = Txt("New habit", "Neue Gewohnheit", "Yeni alışkanlık", "Nuevo hábito", "新建习惯")
    val editHabit = Txt("Edit habit", "Gewohnheit bearbeiten", "Alışkanlığı düzenle", "Editar hábito", "编辑习惯")
    val title = Txt("Title", "Titel", "Başlık", "Título", "标题")
    val weekdays = Txt("On these days", "An diesen Tagen", "Bu günlerde", "Estos días", "在这些日子")
    val everyDay = Txt("Every day", "Jeden Tag", "Her gün", "Cada día", "每天")
    val reminder = Txt("Reminder", "Erinnerung", "Hatırlatıcı", "Recordatorio", "提醒")
    val markDone = Txt("Done", "Erledigt", "Tamamlandı", "Hecho", "完成")
    val streakOneDay = Txt("1 day in a row", "1 Tag in Folge", "Art arda 1 gün", "1 día seguido", "连续 1 天")
    val streakDays = Txt("%d days in a row", "%d Tage in Folge", "Art arda %d gün", "%d días seguidos", "连续 %d 天")
    val noHabits = Txt(
        "No habits yet – e.g. \"Take medication\" every day with a reminder.",
        "Noch keine Gewohnheiten – z. B. „Medikamente nehmen“ jeden Tag mit Erinnerung.",
        "Henüz alışkanlık yok – ör. her gün hatırlatıcıyla \"İlaç al\".",
        "Aún no hay hábitos – p. ej. \"Tomar la medicación\" cada día con recordatorio.",
        "还没有习惯——例如每天提醒「吃药」。"
    )
    val notToday = Txt("Not due today", "Heute nicht dran", "Bugün değil", "Hoy no toca", "今天不需要")
    // Home-screen widget
    val all = Txt("All", "Alle", "Tümü", "Todo", "全部")
    val tomorrow = Txt("Tomorrow", "Morgen", "Yarın", "Mañana", "明天")
    val allDone = Txt("All done for now 🎉", "Alles erledigt 🎉", "Hepsi tamam 🎉", "Todo hecho 🎉", "全部完成 🎉")
    val moduleOff = Txt(
        "Habits are switched off in the settings.",
        "Gewohnheiten sind in den Einstellungen ausgeschaltet.",
        "Alışkanlıklar ayarlarda kapalı.",
        "Hábitos está desactivado en los ajustes.",
        "习惯已在设置中关闭。"
    )
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar", "保存")
    val cancel = Txt("Cancel", "Abbrechen", "İptal", "Cancelar", "取消")
    val delete = Txt("Delete", "Löschen", "Sil", "Eliminar", "删除")
    val habitDeleted = Txt("Habit deleted", "Gewohnheit gelöscht", "Alışkanlık silindi", "Hábito eliminado", "习惯已删除")
    val habitReminderText = Txt("Time for your habit", "Zeit für deine Gewohnheit", "Alışkanlığının zamanı", "Hora de tu hábito", "该做这个习惯了")
    // Reminders
    val snoozeFor = Txt("Snooze %s", "Später (%s)", "Ertele (%s)", "Posponer %s", "稍后提醒（%s）")
    val quickAddHabit = Txt(
        "Quick add, e.g. “Gym Mon Wed Fri 18:00”",
        "Schnell hinzufügen, z. B. „Sport Mo Mi Fr 18 Uhr“",
        "Hızlı ekle, ör. “Spor her gün 18:00”",
        "Añadir rápido, p. ej. «Gimnasio cada día 18:00»",
        "快速添加，例如「健身 每天 18:00」"
    )
    val add = Txt("Add", "Hinzufügen", "Ekle", "Añadir", "添加")
    val notificationsOff = Txt(
        "Notifications are off – reminders can't reach you.",
        "Benachrichtigungen sind aus – Erinnerungen kommen nicht an.",
        "Bildirimler kapalı – hatırlatıcılar sana ulaşamaz.",
        "Las notificaciones están desactivadas: los recordatorios no te llegarán.",
        "通知已关闭 – 提醒无法送达。"
    )
    val exactAlarmsOff = Txt(
        "Exact alarms are not allowed – reminders may come late.",
        "Genaue Wecker sind nicht erlaubt – Erinnerungen kommen evtl. zu spät.",
        "Tam zamanlı alarmlara izin yok – hatırlatıcılar gecikebilir.",
        "Las alarmas exactas no están permitidas: los recordatorios pueden llegar tarde.",
        "未允许精确闹钟 – 提醒可能会延迟。"
    )
    val openSettings = Txt("Settings", "Einstellungen", "Ayarlar", "Ajustes", "设置")
    /** One letter per weekday, Monday first. */
    val weekdayLetters = Txt("MTWTFSS", "MDMDFSS", "PSÇPCCP", "LMXJVSD", "一二三四五六日")
}
