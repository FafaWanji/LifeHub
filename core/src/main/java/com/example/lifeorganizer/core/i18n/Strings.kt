package com.example.lifeorganizer.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Current UI language code ("en", "de", "tr", "es", "zh"), provided once at the app root. */
val LocalAppLanguage = staticCompositionLocalOf { "en" }

val SupportedLanguages = listOf("en", "de", "tr", "es", "zh")

/** One UI text in all supported languages (zh = Simplified Chinese). */
class Txt(val en: String, val de: String, val tr: String, val es: String, val zh: String) {
    fun of(lang: String): String = when (lang) {
        "de" -> de
        "tr" -> tr
        "es" -> es
        "zh" -> zh
        else -> en
    }
}

@Composable
@ReadOnlyComposable
fun Txt.text(): String = of(LocalAppLanguage.current)

/** Shared UI strings for the app shell, notes, search and Smart Add. */
object Str {
    // Navigation
    val calendar = Txt("Calendar", "Kalender", "Takvim", "Calendario", "日历")
    val notes = Txt("Notes", "Notizen", "Notlar", "Notas", "笔记")
    val waypoints = Txt("Waypoints", "Waypoints", "Konumlar", "Lugares", "地点")
    val documents = Txt("Documents", "Dokumente", "Belgeler", "Documentos", "文档")
    val finance = Txt("Finance", "Finanzen", "Finans", "Finanzas", "财务")
    val settings = Txt("Settings", "Einstellungen", "Ayarlar", "Ajustes", "设置")
    val menu = Txt("Menu", "Menü", "Menü", "Menú", "菜单")
    val back = Txt("Back", "Zurück", "Geri", "Atrás", "返回")
    val tagline = Txt("Your day, organised.", "Dein Alltag, organisiert.", "Günün, düzenli.", "Tu día, organizado.", "你的日常，井井有条。")
    val organise = Txt("Organise", "Organisieren", "Düzenle", "Organizar", "整理")
    val tools = Txt("Tools", "Werkzeuge", "Araçlar", "Herramientas", "工具")
    val switchToNotes = Txt("Switch to notes", "Zu Notizen wechseln", "Notlara geç", "Cambiar a notas", "切换到笔记")
    val switchToCalendar = Txt("Switch to calendar", "Zum Kalender wechseln", "Takvime geç", "Cambiar al calendario", "切换到日历")
    val newEvent = Txt("New event", "Neuer Termin", "Yeni etkinlik", "Nuevo evento", "新建日程")
    val newNote = Txt("New note", "Neue Notiz", "Yeni not", "Nueva nota", "新建笔记")
    val smartAdd = Txt("Smart Add", "Smart Add", "Akıllı Ekle", "Añadir inteligente", "智能添加")

    // Universal search
    val searchEverywhere = Txt("Search everywhere…", "Überall suchen…", "Her yerde ara…", "Buscar en todo…", "全局搜索…")
    val searchHint = Txt(
        "Search events, notes, waypoints and documents at once",
        "Durchsucht Termine, Notizen, Waypoints und Dokumente gleichzeitig",
        "Etkinlikleri, notları, konumları ve belgeleri birlikte ara",
        "Busca eventos, notas, lugares y documentos a la vez", "同时搜索日程、笔记、地点和文档"
    )
    val noResults = Txt("No results", "Keine Treffer", "Sonuç yok", "Sin resultados", "无结果")
    val events = Txt("Events", "Termine", "Etkinlikler", "Eventos", "日程")
    val untitled = Txt("Untitled", "Ohne Titel", "Başlıksız", "Sin título", "无标题")
    val clear = Txt("Clear", "Löschen", "Temizle", "Borrar", "清除")

    // Notes list
    val pinned = Txt("Pinned", "Angeheftet", "Sabitlenmiş", "Fijadas", "已置顶")
    val all = Txt("All", "Alle", "Tümü", "Todas", "全部")
    val trash = Txt("Trash", "Papierkorb", "Çöp kutusu", "Papelera", "回收站")
    val labels = Txt("Labels", "Labels", "Etiketler", "Etiquetas", "标签")
    val manageLabels = Txt("Manage labels", "Labels verwalten", "Etiketleri yönet", "Gestionar etiquetas", "管理标签")
    val templates = Txt("Templates", "Vorlagen", "Şablonlar", "Plantillas", "模板")
    val manageTemplates = Txt("Manage templates", "Vorlagen verwalten", "Şablonları yönet", "Gestionar plantillas", "管理模板")
    val sort = Txt("Sort", "Sortieren", "Sırala", "Ordenar", "排序")
    val sortUpdated = Txt("Last edited", "Zuletzt bearbeitet", "Son düzenlenen", "Última edición", "最近编辑")
    val sortCreated = Txt("Newest", "Neueste", "En yeni", "Más recientes", "最新")
    val sortTitle = Txt("Title A–Z", "Titel A–Z", "Başlık A–Z", "Título A–Z", "标题 A–Z")
    val sortColor = Txt("Color", "Farbe", "Renk", "Color", "颜色")
    val emptyNotes = Txt(
        "No notes yet.\nTap + to write your first one.",
        "Noch keine Notizen.\nTippe auf +, um loszulegen.",
        "Henüz not yok.\nİlk notun için + simgesine dokun.",
        "Aún no hay notas.\nToca + para escribir la primera.", "还没有笔记。\n点按 + 写下第一条。"
    )
    val trashEmpty = Txt("Trash is empty", "Papierkorb ist leer", "Çöp kutusu boş", "La papelera está vacía", "回收站是空的")
    val emptyTrash = Txt("Empty trash", "Papierkorb leeren", "Çöpü boşalt", "Vaciar papelera", "清空回收站")
    val emptyTrashConfirm = Txt(
        "Permanently delete all notes in the trash?",
        "Alle Notizen im Papierkorb endgültig löschen?",
        "Çöp kutusundaki tüm notlar kalıcı olarak silinsin mi?",
        "¿Eliminar definitivamente todas las notas de la papelera?", "永久删除回收站中的所有笔记？"
    )
    val deleteForeverConfirm = Txt(
        "Permanently delete this note?",
        "Diese Notiz endgültig löschen?",
        "Bu not kalıcı olarak silinsin mi?",
        "¿Eliminar definitivamente esta nota?", "永久删除这条笔记？"
    )
    val notesMovedToTrash = Txt("%d notes moved to trash", "%d Notizen in den Papierkorb verschoben", "%d not çöp kutusuna taşındı", "%d notas movidas a la papelera", "已将 %d 条笔记移至回收站")
    val fileUnavailable = Txt(
        "This file can't be opened. It may have been moved or deleted, or access was lost – add it again.",
        "Diese Datei lässt sich nicht öffnen. Sie wurde evtl. verschoben/gelöscht oder der Zugriff ging verloren – bitte neu hinzufügen.",
        "Bu dosya açılamıyor. Taşınmış/silinmiş olabilir ya da erişim kaybolmuş olabilir – yeniden ekle.",
        "No se puede abrir el archivo. Puede que se haya movido o borrado, o se perdió el acceso: vuelve a añadirlo.", "无法打开此文件。它可能已被移动或删除，或访问权限已丢失 – 请重新添加。"
    )
    val selected = Txt("selected", "ausgewählt", "seçildi", "seleccionadas", "已选择")
    val unpinNote = Txt("Unpin", "Lösen", "Sabitlemeyi kaldır", "Desfijar", "取消置顶")
    val noNotesWithLabel = Txt(
        "No notes with this label.",
        "Keine Notizen mit diesem Label.",
        "Bu etikete sahip not yok.",
        "No hay notas con esta etiqueta.", "没有带此标签的笔记。"
    )
    val restore = Txt("Restore", "Wiederherstellen", "Geri yükle", "Restaurar", "恢复")
    val deleteForever = Txt("Delete forever", "Endgültig löschen", "Kalıcı olarak sil", "Eliminar definitivamente", "永久删除")
    val movedToTrash = Txt("Moved to trash", "In den Papierkorb verschoben", "Çöp kutusuna taşındı", "Movida a la papelera", "已移至回收站")
    val undo = Txt("Undo", "Rückgängig", "Geri al", "Deshacer", "撤销")
    val showDeviceCalendars = Txt("Show phone calendars", "Gerätekalender anzeigen", "Telefon takvimlerini göster", "Mostrar calendarios del teléfono", "显示手机日历")
    val showDeviceCalendarsDesc = Txt(
        "Google, Samsung & co. – read-only",
        "Google, Samsung & Co. – nur lesen",
        "Google, Samsung vb. – salt okunur",
        "Google, Samsung, etc. – solo lectura", "Google、三星等 – 只读"
    )
    val deviceEventReadOnly = Txt(
        "This event is from your phone calendar – change it there.",
        "Dieser Termin stammt aus dem Gerätekalender – bitte dort ändern.",
        "Bu etkinlik telefon takviminden – orada değiştir.",
        "Este evento es del calendario del teléfono: cámbialo allí.", "此日程来自你的手机日历 – 请在那里修改。"
    )
    val editRepeating = Txt("Edit repeating event", "Wiederkehrenden Termin bearbeiten", "Tekrarlanan etkinliği düzenle", "Editar evento periódico", "编辑重复日程")
    val deleteRepeating = Txt("Delete repeating event", "Wiederkehrenden Termin löschen", "Tekrarlanan etkinliği sil", "Eliminar evento periódico", "删除重复日程")
    val thisEventOnly = Txt("This event", "Nur diesen Termin", "Yalnızca bu etkinlik", "Solo este evento", "此日程")
    val thisAndFollowing = Txt("This and following events", "Diesen und alle folgenden", "Bu ve sonrakiler", "Este y los siguientes", "此日程及之后的日程")
    val allEvents = Txt("All events", "Alle Termine der Serie", "Tüm etkinlikler", "Todos los eventos", "所有日程")
    val checkedToBottom = Txt("Move checked to bottom", "Erledigte nach unten", "İşaretlileri alta taşı", "Marcados al final", "将已勾选项移到底部")
    val placeholderHint = Txt(
        "Placeholders: {{date}}, {{weekday}}, {{time}}",
        "Platzhalter: {{datum}}, {{wochentag}}, {{uhrzeit}}",
        "Yer tutucular: {{tarih}}, {{gun}}, {{saat}}",
        "Marcadores: {{fecha}}, {{dia}}, {{hora}}", "占位符：{{date}}、{{weekday}}、{{time}}"
    )
    val createNoteForEvent = Txt("Create note", "Notiz anlegen", "Not oluştur", "Crear nota", "创建笔记")
    val attachDocument = Txt("Attach document", "Dokument anhängen", "Belge ekle", "Adjuntar documento", "附加文档")
    val copyToDevice = Txt("Copy to phone calendar", "In Gerätekalender kopieren", "Telefon takvimine kopyala", "Copiar al calendario del teléfono", "复制到手机日历")
    val copiedToDevice = Txt("Copied to phone calendar", "In Gerätekalender kopiert", "Telefon takvimine kopyalandı", "Copiado al calendario del teléfono", "已复制到手机日历")
    val copyFailed = Txt("Could not copy", "Kopieren fehlgeschlagen", "Kopyalanamadı", "No se pudo copiar", "无法复制")
    val noWritableCalendar = Txt("No writable calendar on this phone", "Kein beschreibbarer Kalender auf dem Gerät", "Yazılabilir takvim yok", "No hay calendario editable", "此手机上没有可写入的日历")
    val markdownHelp = Txt("Formatting help", "Formatierungshilfe", "Biçimlendirme yardımı", "Ayuda de formato", "格式帮助")
    val helpHeading = Txt("Headings", "Überschriften", "Başlıklar", "Títulos", "标题")
    val helpEmphasis = Txt("Bold, italic, strikethrough", "Fett, kursiv, durchgestrichen", "Kalın, italik, üstü çizili", "Negrita, cursiva, tachado", "粗体、斜体、删除线")
    val helpBullet = Txt("Bullet list", "Aufzählung", "Madde listesi", "Lista", "项目符号列表")
    val helpNumbered = Txt("Numbered list", "Nummerierte Liste", "Numaralı liste", "Lista numerada", "编号列表")
    val helpCheckbox = Txt("Checklist", "Checkliste", "Kontrol listesi", "Lista de tareas", "清单")
    val helpQuote = Txt("Quote", "Zitat", "Alıntı", "Cita", "引用")
    val helpCode = Txt("Code", "Code", "Kod", "Código", "代码")
    val helpRule = Txt("Divider line", "Trennlinie", "Ayırıcı çizgi", "Línea divisoria", "分隔线")
    val helpTick = Txt(
        "Tick items: tap the box in the preview or on the note card – or press the checkbox button on the line while editing.",
        "Abhaken: Kästchen in der Vorschau oder direkt auf der Notizkarte antippen – oder beim Bearbeiten den Checkbox-Knopf in der Zeile drücken.",
        "İşaretle: önizlemede veya not kartında kutuya dokun – ya da düzenlerken satırda onay kutusu düğmesine bas.",
        "Marcar: toca la casilla en la vista previa o en la tarjeta, o pulsa el botón de casilla en la línea al editar.", "勾选项目：在预览或笔记卡片上点按方框 – 或在编辑时按该行的复选框按钮。"
    )
    val filter = Txt("Filter", "Filter", "Filtre", "Filtro", "筛选")
    val show = Txt("Show", "Anzeigen", "Göster", "Mostrar", "显示")
    val repeatingEvents = Txt("Repeating events", "Wiederholungen", "Tekrarlanan etkinlikler", "Eventos periódicos", "重复日程")
    val discard = Txt("Discard", "Verwerfen", "Vazgeç", "Descartar", "放弃")
    val retry = Txt("Retry", "Erneut versuchen", "Tekrar dene", "Reintentar", "重试")
    val globalSearch = Txt("Search everywhere", "Überall suchen", "Her yerde ara", "Buscar en todo", "全局搜索")
    val weekView = Txt("Week", "Woche", "Hafta", "Semana", "周")
    val agendaView = Txt("Agenda", "Agenda", "Ajanda", "Agenda", "日程列表")
    val monthView = Txt("Month", "Monat", "Ay", "Mes", "月")
    val addEvent = Txt("Add event", "Termin hinzufügen", "Etkinlik ekle", "Añadir evento", "添加日程")
    val pinnedNote = Txt("Pinned note", "Angeheftete Notiz", "Sabitlenmiş not", "Nota fijada", "置顶笔记")
    val untitledNote = Txt("Untitled note", "Notiz ohne Titel", "Başlıksız not", "Nota sin título", "无标题笔记")
    val minutes = Txt("minutes", "Minuten", "dakika", "minutos", "分钟")
    val hours = Txt("hours", "Stunden", "saat", "horas", "小时")
    val days = Txt("days", "Tage", "gün", "días", "天")
    val exactTime = Txt("At start time", "Zur Startzeit", "Başlangıçta", "A la hora de inicio", "开始时")
    val before = Txt("%s before", "%s vorher", "%s önce", "%s antes", "提前 %s")
    val addReminder = Txt("Add reminder", "Erinnerung hinzufügen", "Hatırlatıcı ekle", "Añadir recordatorio", "添加提醒")
    val addressTooShort = Txt("Address too short", "Adresse zu kurz", "Adres çok kısa", "Dirección demasiado corta", "地址太短")
    val saveAsTemplate2 = Txt("Save as template", "Als Vorlage speichern", "Şablon olarak kaydet", "Guardar como plantilla", "保存为模板")
    val loadTemplate = Txt("Load from template", "Aus Vorlage laden", "Şablondan yükle", "Cargar de plantilla", "从模板加载")
    val deleteTemplate = Txt("Delete template", "Vorlage löschen", "Şablonu sil", "Eliminar plantilla", "删除模板")
    val manageCategories = Txt("Manage categories", "Kategorien verwalten", "Kategorileri yönet", "Gestionar categorías", "管理分类")
    val addCategory = Txt("Add category", "Kategorie hinzufügen", "Kategori ekle", "Añadir categoría", "添加分类")
    val newCategory = Txt("New category", "Neue Kategorie", "Yeni kategori", "Nueva categoría", "新分类")
    val categoryName = Txt("Category name", "Kategoriename", "Kategori adı", "Nombre de categoría", "分类名称")
    val selectColor = Txt("Select colour", "Farbe wählen", "Renk seç", "Elegir color", "选择颜色")
    val customColor = Txt("Custom colour (RGB)", "Eigene Farbe (RGB)", "Özel renk (RGB)", "Color propio (RGB)", "自定义颜色（RGB）")
    val customRecurrence = Txt("Custom repetition", "Eigene Wiederholung", "Özel tekrar", "Repetición personalizada", "自定义重复")
    val frequency = Txt("Frequency", "Häufigkeit", "Sıklık", "Frecuencia", "频率")
    val close = Txt("Close", "Schließen", "Kapat", "Cerrar", "关闭")
    val exportOptions = Txt("Export options", "Export-Optionen", "Dışa aktarma seçenekleri", "Opciones de exportación", "导出选项")
    val exportEverything = Txt("Export everything", "Alles exportieren", "Her şeyi dışa aktar", "Exportar todo", "全部导出")
    val exportSpecific = Txt("Export selection", "Auswahl exportieren", "Seçimi dışa aktar", "Exportar selección", "导出所选")
    val birthdays = Txt("Birthdays", "Geburtstage", "Doğum günleri", "Cumpleaños", "生日")
    val exportBtn2 = Txt("Export", "Exportieren", "Dışa aktar", "Exportar", "导出")
    val previousWeek = Txt("Previous week", "Vorherige Woche", "Önceki hafta", "Semana anterior", "上一周")
    val nextWeek = Txt("Next week", "Nächste Woche", "Sonraki hafta", "Semana siguiente", "下一周")
    val exportCalendar = Txt("Export calendar", "Kalender exportieren", "Takvimi dışa aktar", "Exportar calendario", "导出日历")
    val upcomingAgenda = Txt("Upcoming", "Demnächst", "Yaklaşan", "Próximos", "即将到来")
    val noUpcoming = Txt("No upcoming events", "Keine anstehenden Termine", "Yaklaşan etkinlik yok", "No hay eventos próximos", "没有即将到来的日程")
    val allDay = Txt("All day", "Ganztägig", "Tüm gün", "Todo el día", "全天")
    val widgetCategories = Txt("Widget categories", "Widget-Kategorien", "Widget kategorileri", "Categorías del widget", "小部件分类")
    val selectAll = Txt("Select all", "Alle auswählen", "Tümünü seç", "Seleccionar todo", "全选")
    val noCategory = Txt("No category", "Keine Kategorie", "Kategori yok", "Sin categoría", "无分类")
    val appLock = Txt("App lock", "App-Sperre", "Uygulama kilidi", "Bloqueo de app", "应用锁")
    val appLockSubtitle = Txt("Unlock to open LifeHub", "Entsperren, um LifeHub zu öffnen", "LifeHub'ı açmak için kilidi aç", "Desbloquea para abrir LifeHub", "解锁以打开 LifeHub")
    val unlockApp = Txt("Unlock", "Entsperren", "Kilidi aç", "Desbloquear", "解锁")
    val info = Txt("Info", "Info", "Bilgi", "Info", "信息")
    val remindersChannel = Txt("Calendar reminders", "Kalender-Erinnerungen", "Takvim hatırlatıcıları", "Recordatorios del calendario", "日历提醒")
    val remindersChannelDesc = Txt("Notifications for calendar events", "Benachrichtigungen zu Terminen", "Etkinlik bildirimleri", "Notificaciones de eventos", "日历日程通知")
    val birthdayChannel = Txt("Birthday reminders", "Geburtstags-Erinnerungen", "Doğum günü hatırlatıcıları", "Recordatorios de cumpleaños", "生日提醒")
    val mode = Txt("Mode", "Modus", "Mod", "Modo", "模式")
    val pastelLight = Txt("Pastel light", "Pastell hell", "Pastel açık", "Pastel claro", "柔和浅色")
    val pastelDark = Txt("Pastel dark", "Pastell dunkel", "Pastel koyu", "Pastel oscuro", "柔和深色")
    val neonAccent = Txt("Neon accent", "Neon-Akzent", "Neon vurgu", "Acento neón", "霓虹强调")
    val pin2 = Txt("Pin", "Anheften", "Sabitle", "Fijar", "置顶")
    val strikethrough = Txt("Strikethrough", "Durchgestrichen", "Üstü çizili", "Tachado", "删除线")
    val edit2 = Txt("Edit", "Bearbeiten", "Düzenle", "Editar", "编辑")
    val delete2 = Txt("Delete", "Löschen", "Sil", "Eliminar", "删除")
    val cancel2 = Txt("Cancel", "Abbrechen", "İptal", "Cancelar", "取消")
    val done2 = Txt("Done", "Fertig", "Tamam", "Listo", "完成")
    val crashTitle = Txt("LifeHub stopped", "LifeHub wurde beendet", "LifeHub durdu", "LifeHub se cerró", "LifeHub 已停止")
    val crashText = Txt(
        "Something went wrong. Your data is safe. Sharing the report helps fix the problem – it only contains technical information, no notes or events.",
        "Etwas ist schiefgelaufen. Deine Daten sind sicher. Teile den Bericht, damit der Fehler behoben werden kann – er enthält nur technische Angaben, keine Notizen oder Termine.",
        "Bir şeyler ters gitti. Verilerin güvende. Raporu paylaşmak hatanın düzeltilmesine yardım eder – yalnızca teknik bilgi içerir.",
        "Algo salió mal. Tus datos están a salvo. Compartir el informe ayuda a corregirlo: solo contiene información técnica.", "出了点问题。你的数据是安全的。分享报告有助于修复问题 – 报告仅包含技术信息，不含笔记或日程。"
    )
    val shareReport = Txt("Share report", "Bericht teilen", "Raporu paylaş", "Compartir informe", "分享报告")
    val copyReport = Txt("Copy report", "Bericht kopieren", "Raporu kopyala", "Copiar informe", "复制报告")
    val restartApp = Txt("Restart app", "App neu starten", "Uygulamayı yeniden başlat", "Reiniciar app", "重启应用")
    val technicalDetails = Txt("Technical details", "Technische Details", "Teknik ayrıntılar", "Detalles técnicos", "技术详情")
    val about = Txt("About & privacy", "Über die App & Datenschutz", "Hakkında ve gizlilik", "Acerca de y privacidad", "关于与隐私")
    val privacy = Txt("Privacy", "Datenschutz", "Gizlilik", "Privacidad", "隐私")
    val openSource = Txt("Open-source licences", "Open-Source-Lizenzen", "Açık kaynak lisansları", "Licencias de código abierto", "开源许可")
    val minute = Txt("minute", "Minute", "dakika", "minuto", "分钟")
    val hour = Txt("hour", "Stunde", "saat", "hora", "小时")
    val day = Txt("day", "Tag", "gün", "día", "天")
    val widgetToday = Txt("Today", "Heute", "Bugün", "Hoy", "今天")
    val widgetTomorrow = Txt("Tomorrow", "Morgen", "Yarın", "Mañana", "明天")
    val widgetInDays = Txt("in %d days", "in %d Tagen", "%d gün sonra", "en %d días", "%d 天后")
    val appSetupTitle = Txt("Welcome to LifeHub", "Willkommen bei LifeHub", "LifeHub'a hoş geldin", "Bienvenido a LifeHub", "欢迎使用 LifeHub")
    val checkUpdates = Txt("Check for updates", "Nach Updates suchen", "Güncellemeleri denetle", "Buscar actualizaciones", "检查更新")
    val updateAvailable = Txt("Version %s available", "Version %s verfügbar", "%s sürümü mevcut", "Versión %s disponible", "有新版本 %s")
    val upToDate = Txt("Up to date", "Aktuell", "Güncel", "Actualizado", "已是最新")
    val upToDateDesc = Txt("You have the newest version (or GitHub is not reachable).", "Du hast die neueste Version (oder GitHub ist nicht erreichbar).", "En yeni sürüm sende (veya GitHub'a ulaşılamıyor).", "Tienes la última versión (o GitHub no responde).", "你已是最新版本（或无法访问 GitHub）。")
    val downloadInstall = Txt("Download & install", "Herunterladen & installieren", "İndir ve yükle", "Descargar e instalar", "下载并安装")
    val updateFailed = Txt("Download failed – check the connection.", "Download fehlgeschlagen – Verbindung prüfen.", "İndirme başarısız – bağlantıyı kontrol et.", "Descarga fallida: revisa la conexión.", "下载失败 – 请检查网络连接。")
    val later = Txt("Later", "Später", "Sonra", "Más tarde", "稍后")
    val whatsNew = Txt("What's new in %s", "Neu in Version %s", "%s sürümünde yenilikler", "Novedades en %s", "%s 的新内容")
    val changelog = Txt("Patch notes", "Patchnotes", "Sürüm notları", "Notas de versión", "更新说明")
    val showAfterUpdates = Txt("Show after updates", "Nach Updates anzeigen", "Güncellemeden sonra göster", "Mostrar tras actualizar", "更新后显示")
    val setupSubtitle = Txt("Choose language and look – you can change everything later.", "Wähle Sprache und Aussehen – alles lässt sich später ändern.", "Dil ve görünümü seç – her şey sonra değiştirilebilir.", "Elige idioma y aspecto: todo se puede cambiar luego.", "选择语言和外观 – 之后都可以修改。")
    val language = Txt("Language", "Sprache", "Dil", "Idioma", "语言")
    val design = Txt("Design", "Design", "Tasarım", "Diseño", "外观")
    val brightness = Txt("Brightness", "Helligkeit", "Parlaklık", "Brillo", "亮度")
    val system = Txt("System", "System", "Sistem", "Sistema", "跟随系统")
    val light = Txt("Light", "Hell", "Açık", "Claro", "浅色")
    val dark = Txt("Dark", "Dunkel", "Koyu", "Oscuro", "深色")
    val accent = Txt("Accent colour", "Akzentfarbe", "Vurgu rengi", "Color de acento", "强调色")
    val getStarted = Txt("Get started", "Los geht's", "Başla", "Empezar", "开始使用")
    val setupLater = Txt("Design and language can be changed any time in the settings.", "Design und Sprache kannst du jederzeit in den Einstellungen ändern.", "Tasarım ve dil ayarlardan her zaman değiştirilebilir.", "Diseño e idioma se cambian en ajustes en cualquier momento.", "外观和语言随时可以在设置中修改。")
    val uncheckAll = Txt("Uncheck all", "Alle Haken entfernen", "Tüm işaretleri kaldır", "Desmarcar todo", "全部取消勾选")
    val labelTemplate = Txt("Template for new notes", "Vorlage für neue Notizen", "Yeni notlar için şablon", "Plantilla para notas nuevas", "新笔记模板")
    val labelTemplateNone = Txt("No template – tap to add one", "Keine Vorlage – tippen zum Anlegen", "Şablon yok – eklemek için dokun", "Sin plantilla: toca para añadir", "没有模板 – 点按添加")
    val addChecklistItem = Txt("Checklist item", "Listenpunkt", "Liste öğesi", "Elemento de lista", "清单项")
    val newLabel = Txt("New label", "Neues Label", "Yeni etiket", "Nueva etiqueta", "新标签")
    val add = Txt("Add", "Hinzufügen", "Ekle", "Añadir", "添加")
    val delete = Txt("Delete", "Löschen", "Sil", "Eliminar", "删除")
    val cancel = Txt("Cancel", "Abbrechen", "İptal", "Cancelar", "取消")
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar", "保存")
    val done = Txt("Done", "Fertig", "Tamam", "Listo", "完成")
    val noLabels = Txt("No labels yet", "Noch keine Labels", "Henüz etiket yok", "Aún no hay etiquetas", "还没有标签")
    val noTemplates = Txt(
        "No templates yet. Open a note and choose “Save as template”.",
        "Noch keine Vorlagen. Öffne eine Notiz und wähle „Als Vorlage speichern“.",
        "Henüz şablon yok. Bir not açıp “Şablon olarak kaydet”i seç.",
        "Aún no hay plantillas. Abre una nota y elige «Guardar como plantilla».", "还没有模板。打开一条笔记并选择“保存为模板”。"
    )
    val templateName = Txt("Template name", "Name der Vorlage", "Şablon adı", "Nombre de la plantilla", "模板名称")
    val blankNote = Txt("Blank note", "Leere Notiz", "Boş not", "Nota en blanco", "空白笔记")
    val chooseTemplate = Txt("Start from…", "Starten mit…", "Şununla başla…", "Empezar con…", "从…开始")

    // Note editor
    val title = Txt("Title", "Titel", "Başlık", "Título", "标题")
    val startWriting = Txt(
        "Start writing… Markdown supported. Use - [ ] for checkboxes.",
        "Schreib los… Markdown wird unterstützt. - [ ] erzeugt Checkboxen.",
        "Yazmaya başla… Markdown desteklenir. Onay kutuları için - [ ] kullan.",
        "Empieza a escribir… Admite Markdown. Usa - [ ] para casillas.", "开始书写… 支持 Markdown。用 - [ ] 创建复选框。"
    )
    val edit = Txt("Edit", "Bearbeiten", "Düzenle", "Editar", "编辑")
    val preview = Txt("Preview", "Vorschau", "Önizleme", "Vista previa", "预览")
    val words = Txt("words", "Wörter", "kelime", "palabras", "字")
    val chars = Txt("chars", "Zeichen", "karakter", "caracteres", "字符")
    val pinToDate = Txt("Pin to calendar day", "An Kalendertag heften", "Takvim gününe sabitle", "Fijar a un día", "置顶到日历某天")
    val pinnedTo = Txt("Pinned to", "Angeheftet an", "Sabitlendi:", "Fijada al", "置顶于")
    val unpin = Txt("Remove", "Entfernen", "Kaldır", "Quitar", "移除")
    val pinNote = Txt("Pin note", "Notiz anheften", "Notu sabitle", "Fijar nota", "置顶笔记")
    val color = Txt("Color", "Farbe", "Renk", "Color", "颜色")
    val noColor = Txt("None", "Keine", "Yok", "Ninguno", "无")
    val reminder = Txt("Reminder", "Erinnerung", "Hatırlatıcı", "Recordatorio", "提醒")
    val setReminder = Txt("Set reminder", "Erinnerung setzen", "Hatırlatıcı kur", "Crear recordatorio", "设置提醒")
    val reminderCreated = Txt(
        "Reminder added to your calendar",
        "Erinnerung im Kalender angelegt",
        "Hatırlatıcı takvime eklendi",
        "Recordatorio añadido al calendario", "提醒已添加到你的日历"
    )
    val saveAsTemplate = Txt("Save as template", "Als Vorlage speichern", "Şablon olarak kaydet", "Guardar como plantilla", "保存为模板")
    val templateSaved = Txt("Template saved", "Vorlage gespeichert", "Şablon kaydedildi", "Plantilla guardada", "模板已保存")
    val more = Txt("More", "Mehr", "Daha fazla", "Más", "更多")
    val bold = Txt("Bold", "Fett", "Kalın", "Negrita", "粗体")
    val italic = Txt("Italic", "Kursiv", "İtalik", "Cursiva", "斜体")
    val heading = Txt("Heading", "Überschrift", "Başlık", "Encabezado", "标题")
    val bulletList = Txt("List", "Liste", "Liste", "Lista", "列表")
    val checkbox = Txt("Checkbox", "Checkbox", "Onay kutusu", "Casilla", "复选框")
    val ok = Txt("OK", "OK", "Tamam", "Aceptar", "确定")
    val next = Txt("Next", "Weiter", "İleri", "Siguiente", "下一步")
    val reminderPrefix = Txt("Note", "Notiz", "Not", "Nota", "笔记")
    val openNote = Txt("Open note", "Notiz öffnen", "Notu aç", "Abrir nota", "打开笔记")

    // Calendar
    val leaveAt = Txt("Leave at", "Losfahren um", "Çıkış saati", "Salir a las", "出发时间")
    val minShort = Txt("min", "Min.", "dk", "min", "分钟")
    val navigate = Txt("Navigate", "Navigation starten", "Yol tarifi", "Navegar", "导航")
    val pinnedNotes = Txt("Pinned notes", "Angeheftete Notizen", "Sabitlenmiş notlar", "Notas fijadas", "置顶笔记")
    val reminders = Txt("reminders", "Erinnerungen", "hatırlatıcı", "recordatorios", "提醒")

    // Settings
    val designStyle = Txt("Design style", "Designstil", "Tasarım stili", "Estilo de diseño", "设计风格")
    val locationNavigation = Txt("Location & navigation", "Standort & Navigation", "Konum ve navigasyon", "Ubicación y navegación", "位置与导航")
    val appPreferences = Txt("App preferences", "App-Einstellungen", "Uygulama tercihleri", "Preferencias de la app", "应用偏好")
    val notificationSound = Txt("Notification sound", "Benachrichtigungston", "Bildirim sesi", "Sonido de notificación", "通知声音")
    val soundDefault = Txt("Default", "Standard", "Varsayılan", "Predeterminado", "默认")
    val soundCustom = Txt("Custom", "Benutzerdefiniert", "Özel", "Personalizado", "自定义")
    val categories = Txt("Categories", "Kategorien", "Kategoriler", "Categorías", "分类")
    val categoriesDesc = Txt("Manage event categories and colors", "Termin-Kategorien und Farben verwalten", "Etkinlik kategorilerini ve renklerini yönet", "Gestiona categorías y colores", "管理日程分类和颜色")
    val advancedExport = Txt("Advanced & export", "Erweitert & Export", "Gelişmiş ve dışa aktarma", "Avanzado y exportación", "高级与导出")
    val groqKey = Txt("Groq API key (for Smart Add)", "Groq-API-Key (für Smart Add)", "Groq API anahtarı (Akıllı Ekle için)", "Clave API de Groq (Smart Add)", "Groq API 密钥（用于智能添加）")
    val enableSmartAlarms = Txt("Smart alarms (leave-now alerts)", "Smart-Alarme (Losfahr-Wecker)", "Akıllı alarmlar (çıkış uyarısı)", "Alarmas inteligentes (hora de salir)", "智能闹钟（该出发提醒）")
    val importResult = Txt("Imported %1\$d events, skipped %2\$d duplicates.", "%1\$d Termine importiert, %2\$d Duplikate übersprungen.", "%1\$d etkinlik içe aktarıldı, %2\$d kopya atlandı.", "%1\$d eventos importados, %2\$d duplicados omitidos.", "已导入 %1\$d 个日程，跳过 %2\$d 个重复项。")

    // Smart Add setup (API key)
    val setupTitle = Txt("Smart Add setup", "Smart Add einrichten", "Akıllı Ekle kurulumu", "Configurar Smart Add", "智能添加设置")
    val privacyNotice = Txt("Privacy notice", "Datenschutzhinweis", "Gizlilik bildirimi", "Aviso de privacidad", "隐私说明")
    val privacyText = Txt(
        "Smart Add sends the text you enter (and text read from screenshots) to Groq's servers, where an AI model extracts title, time and place. Avoid entering highly sensitive information.",
        "Smart Add sendet den eingegebenen Text (und aus Screenshots gelesenen Text) an die Server von Groq, wo ein KI-Modell Titel, Zeit und Ort erkennt. Gib keine hochsensiblen Daten ein.",
        "Akıllı Ekle, girdiğin metni (ve ekran görüntülerinden okunan metni) Groq sunucularına gönderir; bir yapay zekâ modeli başlık, zaman ve yeri çıkarır. Çok hassas bilgiler girme.",
        "Smart Add envía el texto que introduces (y el leído de capturas) a los servidores de Groq, donde un modelo de IA extrae título, hora y lugar. Evita introducir datos muy sensibles.", "智能添加会将你输入的文本（以及从截图中识别的文本）发送到 Groq 的服务器，由 AI 模型提取标题、时间和地点。请避免输入高度敏感的信息。"
    )
    val howToKey = Txt("How to get an API key", "So bekommst du einen API-Key", "API anahtarı nasıl alınır", "Cómo obtener una clave API", "如何获取 API 密钥")
    val keyStep1 = Txt("1. Open the ", "1. Öffne die ", "1. Şunu aç: ", "1. Abre la ", "1. 打开 ")
    val keyStepsRest = Txt(
        "\n2. Sign in with Google or e-mail.\n3. Tap “Create API Key” and copy it.\n4. Paste the key below.",
        "\n2. Mit Google oder E-Mail anmelden.\n3. „Create API Key“ antippen und kopieren.\n4. Key unten einfügen.",
        "\n2. Google veya e-posta ile giriş yap.\n3. “Create API Key”e dokun ve kopyala.\n4. Anahtarı aşağıya yapıştır.",
        "\n2. Inicia sesión con Google o correo.\n3. Pulsa «Create API Key» y cópiala.\n4. Pega la clave abajo.", "\n2. 使用 Google 或电子邮件登录。\n3. 点按“Create API Key”并复制。\n4. 将密钥粘贴到下方。"
    )
    val saveContinue = Txt("Save & continue", "Speichern & weiter", "Kaydet ve devam et", "Guardar y continuar", "保存并继续")
    val category = Txt("Category", "Kategorie", "Kategori", "Categoría", "分类")
    val none = Txt("None", "Keine", "Yok", "Ninguna", "无")
    val snooze = Txt("Snooze 10 min", "10 Min. später", "10 dk ertele", "Posponer 10 min", "10 分钟后再提醒")
    val dismiss = Txt("Dismiss", "Schließen", "Kapat", "Descartar", "关闭")

    // Smart Add
    val smartAddHint = Txt(
        "Type, paste, speak or scan a screenshot — LifeHub decides whether it's an event or a note.",
        "Tippen, einfügen, sprechen oder Screenshot scannen – LifeHub erkennt, ob es ein Termin oder eine Notiz ist.",
        "Yaz, yapıştır, konuş ya da ekran görüntüsü tara — LifeHub etkinlik mi not mu karar verir.",
        "Escribe, pega, habla o escanea una captura: LifeHub decide si es un evento o una nota.", "输入、粘贴、说话或扫描截图 — LifeHub 会判断它是日程还是笔记。"
    )
    val smartAddPlaceholder = Txt(
        "e.g. Sunday 3pm meeting at Max",
        "z. B. Sonntag 15 Uhr Treffen bei Max",
        "örn. Pazar 15:00 Max'te buluşma",
        "p. ej. Domingo 15:00 reunión en casa de Max", "例如：周日下午3点在 Max 家开会"
    )
    val takePhoto = Txt("Take photo", "Foto aufnehmen", "Fotoğraf çek", "Hacer foto", "拍照")
    val fromGallery = Txt("Choose from gallery", "Aus Galerie wählen", "Galeriden seç", "Elegir de la galería", "从相册选择")
    val listening = Txt("Listening…", "Ich höre zu…", "Dinliyorum…", "Escuchando…", "正在聆听…")
    val micPermission = Txt(
        "Microphone access is needed for voice input.",
        "Für die Spracheingabe wird Zugriff aufs Mikrofon benötigt.",
        "Sesli giriş için mikrofon izni gerekli.",
        "Se necesita acceso al micrófono para dictar.", "语音输入需要麦克风权限。"
    )
    val speak = Txt("Speak", "Sprechen", "Konuş", "Hablar", "说话")
    val scanImage = Txt("Scan image", "Bild scannen", "Görsel tara", "Escanear imagen", "扫描图片")
    val process = Txt("Create", "Erstellen", "Oluştur", "Crear", "创建")
    val readingImage = Txt("Reading text from image…", "Text wird aus dem Bild gelesen…", "Görseldeki metin okunuyor…", "Leyendo texto de la imagen…", "正在识别图片中的文字…")
    val noTextFound = Txt("No text found in the image.", "Kein Text im Bild gefunden.", "Görselde metin bulunamadı.", "No se encontró texto en la imagen.", "图片中没有找到文字。")
    val parseFailed = Txt("Couldn't understand that. Try rephrasing.", "Das konnte nicht erkannt werden. Bitte anders formulieren.", "Anlaşılamadı. Farklı ifade etmeyi dene.", "No se pudo entender. Prueba a reformularlo.", "无法理解。请换个说法。")
    val voiceUnavailable = Txt("Speech recognition isn't available on this device.", "Spracherkennung ist auf diesem Gerät nicht verfügbar.", "Bu cihazda konuşma tanıma yok.", "El reconocimiento de voz no está disponible.", "此设备不支持语音识别。")
    val savedEvent = Txt("Event saved", "Termin gespeichert", "Etkinlik kaydedildi", "Evento guardado", "日程已保存")
    val savedNote = Txt("Note saved", "Notiz gespeichert", "Not kaydedildi", "Nota guardada", "笔记已保存")
    val savedTransaction = Txt("Transaction saved", "Buchung gespeichert", "İşlem kaydedildi", "Movimiento guardado", "交易已保存")
    val savedItems = Txt("items saved", "Einträge gespeichert", "öğe kaydedildi", "elementos guardados", "项已保存")

    // App lock
    val locked = Txt("LifeHub is locked", "LifeHub ist gesperrt", "LifeHub kilitli", "LifeHub está bloqueado", "LifeHub 已锁定")
    val unlock = Txt("Unlock", "Entsperren", "Kilidi aç", "Desbloquear", "解锁")
    val unlockSubtitle = Txt(
        "Use your fingerprint, face or device PIN",
        "Mit Fingerabdruck, Gesicht oder Geräte-PIN entsperren",
        "Parmak izi, yüz ya da cihaz PIN'ini kullan",
        "Usa tu huella, rostro o PIN del dispositivo", "使用指纹、面容或设备 PIN 码"
    )
}

/** Label of a reminder that fires [offsetMillis] before the start, e.g. "15 Minuten vorher". */
fun reminderLabel(offsetMillis: Long, lang: String): String {
    if (offsetMillis <= 0L) return Str.exactTime.of(lang)
    val mins = offsetMillis / 60_000
    fun unit(n: Long, one: Txt, many: Txt) = "$n ${(if (n == 1L) one else many).of(lang)}"
    val amount = when {
        mins % 1440 == 0L -> unit(mins / 1440, Str.day, Str.days)
        mins % 60 == 0L -> unit(mins / 60, Str.hour, Str.hours)
        else -> unit(mins, Str.minute, Str.minutes)
    }
    return Str.before.of(lang).format(amount)
}
