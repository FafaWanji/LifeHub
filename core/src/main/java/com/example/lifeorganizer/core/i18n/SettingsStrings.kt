package com.example.lifeorganizer.core.i18n

/** "15 min", "1 h", "1 h 30 min", "1 day", "2 days", "1 week" for a number of minutes. */
fun durationLabel(minutes: Int, lang: String): String = when {
    minutes >= 7 * 1440 && minutes % (7 * 1440) == 0 ->
        (minutes / (7 * 1440)).let { if (it == 1) SetStr.oneWeek.of(lang) else SetStr.weeks.of(lang).format(it) }
    minutes >= 1440 && minutes % 1440 == 0 ->
        (minutes / 1440).let { if (it == 1) SetStr.oneDay.of(lang) else SetStr.days.of(lang).format(it) }
    minutes >= 60 && minutes % 60 == 0 -> SetStr.hourShort.of(lang).format(minutes / 60)
    minutes > 60 -> SetStr.hourShort.of(lang).format(minutes / 60) + " " + SetStr.minShort.of(lang).format(minutes % 60)
    else -> SetStr.minShort.of(lang).format(minutes)
}

/** Texts of the module sub-sections in the settings. */
object SetStr {
    // Units
    val minShort = Txt("%d min", "%d Min.", "%d dk", "%d min", "%d 分钟")
    val hourShort = Txt("%d h", "%d Std.", "%d sa", "%d h", "%d 小时")
    val oneDay = Txt("1 day", "1 Tag", "1 gün", "1 día", "1 天")
    val days = Txt("%d days", "%d Tage", "%d gün", "%d días", "%d 天")
    val oneWeek = Txt("1 week", "1 Woche", "1 hafta", "1 semana", "1 周")
    val weeks = Txt("%d weeks", "%d Wochen", "%d hafta", "%d semanas", "%d 周")

    // Shared choices
    val off = Txt("Off", "Aus", "Kapalı", "Desactivado", "关闭")
    val none = Txt("None", "Keine", "Yok", "Ninguno", "无")
    val never = Txt("Never", "Nie", "Asla", "Nunca", "从不")
    val all = Txt("All", "Alle", "Tümü", "Todas", "全部")
    val system = Txt("System", "System", "Sistem", "Sistema", "系统")
    val from = Txt("From", "Von", "Başlangıç", "Desde", "从")
    val to = Txt("To", "Bis", "Bitiş", "Hasta", "到")
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar", "保存")
    val cancel = Txt("Cancel", "Abbrechen", "İptal", "Cancelar", "取消")

    // Habits & tasks
    val defaultDeadlineTime = Txt("Deadline time of new tasks", "Uhrzeit der Deadline bei neuen Aufgaben", "Yeni görevlerde son tarih saati", "Hora límite de las tareas nuevas", "新任务的截止时间")
    val defaultDeadlineTimeHint = Txt(
        "Used when you only pick a day, also in quick add.",
        "Gilt, wenn du nur einen Tag wählst – auch beim schnellen Hinzufügen.",
        "Sadece gün seçtiğinde kullanılır, hızlı eklemede de.",
        "Se usa cuando solo eliges un día, también al añadir rápido.",
        "只选日期时使用，快速添加也适用。"
    )
    val defaultReminders = Txt("Reminders of new tasks", "Erinnerungen bei neuen Aufgaben", "Yeni görevlerin hatırlatıcıları", "Recordatorios de las tareas nuevas", "新任务的提醒")
    val pointsPerTask = Txt("Points per task on time", "Punkte pro rechtzeitiger Aufgabe", "Zamanında görev başına puan", "Puntos por tarea a tiempo", "每项按时任务的积分")
    val latePoints = Txt("Half points when late by at most", "Halbe Punkte bei höchstens so viel Verspätung", "En fazla bu kadar gecikmede yarım puan", "Mitad de puntos si llegas tarde como mucho", "逾期不超过以下时间得一半积分")
    val latePointsHint = Txt(
        "Off: a deadline is a deadline – late is 0.",
        "Aus: Deadline ist Deadline – zu spät gibt 0.",
        "Kapalı: son tarih son tarihtir – geç kalan 0.",
        "Desactivado: una fecha límite es una fecha límite, tarde es 0.",
        "关闭：截止就是截止 – 逾期为 0。"
    )
    val showPoints = Txt("Show points", "Punkte anzeigen", "Puanları göster", "Mostrar puntos", "显示积分")
    val doneShown = Txt("Done tasks in the list", "Erledigte Aufgaben in der Liste", "Listedeki tamamlanan görevler", "Tareas hechas en la lista", "列表中的已完成任务")
    val quietHours = Txt("Quiet hours", "Ruhezeit", "Sessiz saatler", "Horas de silencio", "免打扰时段")
    val quietHoursHint = Txt(
        "No task reminders in this time – the one at the deadline itself still comes.",
        "Keine Aufgaben-Erinnerungen in dieser Zeit – die zur Deadline selbst kommt trotzdem.",
        "Bu sürede görev hatırlatıcısı yok – son tarihin kendisindeki yine gelir.",
        "Sin recordatorios de tareas en este tiempo; el de la hora límite llega igualmente.",
        "此时段内不发任务提醒 – 截止时刻的提醒仍会发送。"
    )
    val soonDays = Txt("“Next days” in the tasks tab", "„Nächste Tage“ im Aufgaben-Tab", "Görevler sekmesinde “Sonraki günler”", "«Próximos días» en la pestaña de tareas", "任务页中的“未来几天”")
    val widgetDoneHabits = Txt("Widget shows habits done today", "Widget zeigt heute erledigte Gewohnheiten", "Widget bugün yapılan alışkanlıkları gösterir", "El widget muestra hábitos hechos hoy", "小部件显示今天已完成的习惯")
    val confirmDeleteTask = Txt("Ask before deleting a task", "Vor dem Löschen einer Aufgabe nachfragen", "Görevi silmeden önce sor", "Preguntar antes de borrar una tarea", "删除任务前先确认")

    // Notifications
    val notifications = Txt("Notifications", "Benachrichtigungen", "Bildirimler", "Notificaciones", "通知")
    val soundVibration = Txt("Sound & vibration", "Ton & Vibration", "Ses ve titreşim", "Sonido y vibración", "声音和振动")
    val soundVibrationHint = Txt(
        "Set per kind of reminder in the system settings.",
        "Pro Art von Erinnerung in den Systemeinstellungen festlegen.",
        "Her hatırlatıcı türü için sistem ayarlarında belirle.",
        "Se ajusta por tipo de recordatorio en los ajustes del sistema.",
        "在系统设置中按提醒类型分别设置。"
    )
    val channelCalendar = Txt("Calendar reminders", "Kalender-Erinnerungen", "Takvim hatırlatıcıları", "Recordatorios del calendario", "日历提醒")
    val channelBirthdays = Txt("Birthdays", "Geburtstage", "Doğum günleri", "Cumpleaños", "生日")
    val snoozeLength = Txt("Snooze for", "Schlummern für", "Erteleme süresi", "Posponer durante", "稍后提醒时长")
    val deadlineRepeat = Txt("Remind again after a missed deadline", "Nach verpasster Deadline noch mal erinnern", "Kaçırılan son tarihten sonra tekrar hatırlat", "Volver a recordar tras perder la fecha límite", "错过截止后再次提醒")

    // Calendar
    val weekStart = Txt("Week starts on", "Woche beginnt am", "Hafta başlangıcı", "La semana empieza el", "每周开始于")
    val eventLength = Txt("Length of new events", "Dauer neuer Termine", "Yeni etkinliklerin süresi", "Duración de los eventos nuevos", "新日程的时长")
    val eventReminder = Txt("Reminder of new events", "Erinnerung bei neuen Terminen", "Yeni etkinliklerin hatırlatıcısı", "Recordatorio de los eventos nuevos", "新日程的提醒")
    val weekNumbers = Txt("Show week numbers", "Kalenderwochen anzeigen", "Hafta numaralarını göster", "Mostrar números de semana", "显示周数")
    val widgetBirthdays = Txt("Birthdays in the agenda widget", "Geburtstage im Agenda-Widget", "Ajanda widget'ında doğum günleri", "Cumpleaños en el widget de agenda", "日程小部件中显示生日")

    // Notes
    val noteChecklist = Txt("New notes start as a checklist", "Neue Notizen starten als Checkliste", "Yeni notlar kontrol listesi olarak başlar", "Las notas nuevas empiezan como lista", "新笔记以清单开始")
    val noteDoneBottom = Txt("Move ticked items to the bottom", "Abgehakte Punkte nach unten schieben", "İşaretlenenleri aşağı taşı", "Mover lo marcado al final", "已勾选项目移到底部")
    val noteFont = Txt("Text size in the editor", "Schriftgröße im Editor", "Düzenleyicide yazı boyutu", "Tamaño del texto en el editor", "编辑器字号")
    val fontSmall = Txt("Small", "Klein", "Küçük", "Pequeño", "小")
    val fontNormal = Txt("Normal", "Normal", "Normal", "Normal", "标准")
    val fontLarge = Txt("Large", "Groß", "Büyük", "Grande", "大")
    val trashDays = Txt("Empty the trash after", "Papierkorb leeren nach", "Çöp kutusunu şu süre sonra boşalt", "Vaciar la papelera tras", "清空回收站的时间")

    // Finance
    val currency = Txt("Currency", "Währung", "Para birimi", "Moneda", "货币")
    val currencyHint = Txt(
        "Only changes how amounts are shown – nothing is converted.",
        "Ändert nur die Anzeige der Beträge – es wird nichts umgerechnet.",
        "Yalnızca tutarların gösterimini değiştirir – dönüştürme yapılmaz.",
        "Solo cambia cómo se muestran los importes; no se convierte nada.",
        "只改变金额的显示方式 – 不会换算。"
    )
    val monthStart = Txt("Month starts on day", "Monat beginnt am Tag", "Ay şu günde başlar", "El mes empieza el día", "每月起始日")
    val monthStartHint = Txt(
        "E.g. your payday: with 25, “October” runs from 25 Oct to 24 Nov.",
        "Z. B. dein Zahltag: Mit 25 läuft „Oktober“ vom 25.10. bis 24.11.",
        "Ör. maaş günün: 25 ile “Ekim” 25 Ekim – 24 Kasım arasıdır.",
        "P. ej. tu día de cobro: con 25, «octubre» va del 25 oct. al 24 nov.",
        "例如发薪日：设为 25 时，“10 月”为 10 月 25 日至 11 月 24 日。"
    )
    val budgetWarn = Txt("Budget turns yellow at", "Budget wird gelb ab", "Bütçe şu oranda sarıya döner", "El presupuesto se pone amarillo al", "预算变黄的比例")

    // Appearance
    val appearance = Txt("Appearance", "Darstellung", "Görünüm", "Apariencia", "外观")
    val compact = Txt("Compact layout", "Kompakte Darstellung", "Sıkışık görünüm", "Diseño compacto", "紧凑布局")
    val compactHint = Txt(
        "Everything a bit smaller, so more fits on the screen.",
        "Alles etwas kleiner, damit mehr auf den Bildschirm passt.",
        "Her şey biraz daha küçük, ekrana daha çok sığar.",
        "Todo un poco más pequeño para que quepa más.",
        "所有内容稍小一些，屏幕能显示更多。"
    )
    val startScreen = Txt("Start screen", "Startbildschirm", "Başlangıç ekranı", "Pantalla de inicio", "启动页面")
    val startScreenHint = Txt("Used from the next app start.", "Gilt ab dem nächsten App-Start.", "Bir sonraki açılıştan itibaren geçerli.", "Se aplica desde el próximo inicio.", "下次启动应用时生效。")
    val dateOrder = Txt("Date format", "Datumsformat", "Tarih biçimi", "Formato de fecha", "日期格式")
    val dateLanguage = Txt("Like the language", "Wie die Sprache", "Dile göre", "Según el idioma", "跟随语言")

    // Privacy & backup
    val privacyBackup = Txt("Privacy & backup", "Datenschutz & Backup", "Gizlilik ve yedekleme", "Privacidad y copia", "隐私与备份")
    val lockScope = Txt("App lock covers", "App-Sperre gilt für", "Uygulama kilidi kapsamı", "El bloqueo cubre", "应用锁范围")
    val lockWholeApp = Txt("Whole app", "Ganze App", "Tüm uygulama", "Toda la app", "整个应用")
    val lockModules = Txt("Only these", "Nur diese", "Sadece bunlar", "Solo estos", "仅以下")
    val lockHint = Txt(
        "Takes effect while the app lock is on (Settings › App preferences).",
        "Wirkt, solange die App-Sperre an ist (Einstellungen › App-Einstellungen).",
        "Uygulama kilidi açıkken geçerlidir (Ayarlar › Uygulama tercihleri).",
        "Funciona mientras el bloqueo está activado (Ajustes › Preferencias).",
        "在开启应用锁时生效（设置 › 应用偏好）。"
    )
    val backupKeep = Txt("Auto backups kept", "Aufbewahrte Auto-Backups", "Saklanan otomatik yedekler", "Copias automáticas guardadas", "保留的自动备份")
    val backupWifi = Txt("Back up on Wi-Fi only", "Backup nur im WLAN", "Yalnızca Wi-Fi'da yedekle", "Copia solo con Wi-Fi", "仅在 Wi-Fi 下备份")
    val backupWifiHint = Txt(
        "Matters for a copy to an online folder such as Google Drive.",
        "Wichtig für die Kopie in einen Online-Ordner wie Google Drive.",
        "Google Drive gibi çevrimiçi bir klasöre kopya için önemlidir.",
        "Importa para la copia en una carpeta en línea como Google Drive.",
        "对复制到 Google 云端硬盘等在线文件夹有影响。"
    )
}
