package com.example.lifeorganizer.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.BuildConfig
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.Txt
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.settings.SettingsManager
import kotlinx.coroutines.launch

// ============================================================================ changelog

data class Release(val version: String, val items: List<Txt>)

/** Newest first. Add an entry for every release. */
val CHANGELOG = listOf(
    Release(
        "1.3", listOf(
            Txt("Tasks: every task gets a deadline with a time and reliable reminders – any number before it (15 min to 1 week or your own) plus one right at the deadline", "Aufgaben: jede Aufgabe bekommt eine Deadline mit Uhrzeit und zuverlässige Erinnerungen – beliebig viele vorher (15 Min. bis 1 Woche oder eigener Wert) plus eine genau zur Deadline", "Görevler: her görevin saatli bir son tarihi ve güvenilir hatırlatıcıları var – öncesinde istediğin kadar (15 dk ile 1 hafta arası veya özel değer) ve son tarihin tam anında bir tane", "Tareas: cada tarea tiene una fecha límite con hora y recordatorios fiables: los que quieras antes (de 15 min a 1 semana o un valor propio) y uno justo a la hora límite", "任务：每个任务都有带时间的截止时间和可靠提醒 – 截止前可设任意多个（15 分钟到 1 周或自定义），截止时刻再提醒一次"),
            Txt("Points: +10 for every task done by its deadline, late counts 0 – also when ticked off in the notification or the widget", "Punkte: +10 für jede bis zur Deadline erledigte Aufgabe, zu spät zählt 0 – auch beim Abhaken in der Benachrichtigung oder im Widget", "Puanlar: son tarihine kadar biten her görev için +10, geç kalan 0 – bildirimden veya widget'tan işaretleyince de", "Puntos: +10 por cada tarea hecha antes de su fecha límite, tarde cuenta 0, también al marcarla en la notificación o el widget", "积分：每按时完成一项任务 +10 分，逾期为 0 – 在通知或小部件中勾选同样计分"),
            Txt("Quick add for tasks with deadline: “Exercise 5 tomorrow 14:00”, “Call dentist at 2pm”", "Aufgaben schnell hinzufügen mit Deadline: „Aufgabe 5 morgen 14 Uhr“, „Zahnarzt anrufen um 14 Uhr“", "Son tarihli hızlı görev ekleme: “Ödev 5 yarın 14:00”", "Añadir tareas rápido con fecha límite: «Ejercicio 5 mañana 14:00»", "快速添加带截止时间的任务：「第 5 题 明天 14:00」"),
            Txt("Hint in the tasks tab when notifications or exact alarms are not allowed, with a button to the settings", "Hinweis im Aufgaben-Tab, wenn Benachrichtigungen oder genaue Wecker nicht erlaubt sind, mit Button zu den Einstellungen", "Bildirimlere veya tam zamanlı alarmlara izin verilmediğinde görevler sekmesinde ayarlara giden düğmeli bir uyarı", "Aviso en la pestaña de tareas si las notificaciones o las alarmas exactas no están permitidas, con botón a los ajustes", "若未允许通知或精确闹钟，任务页会显示提示，并提供前往设置的按钮"),
            Txt("Forgot to tick off in time? “I did it on time” gives the points after an “Are you sure?” (can be switched in the settings)", "Abhaken vergessen? „Rechtzeitig erledigt“ gibt die Punkte nach einem „Bist du sicher?“ (in den Einstellungen umschaltbar)", "İşaretlemeyi mi unuttun? “Zamanında yaptım” bir “Emin misin?” sonrası puanı verir (ayarlardan değiştirilebilir)", "¿Olvidaste marcarla a tiempo? «Lo hice a tiempo» da los puntos tras un «¿Seguro?» (se cambia en los ajustes)", "忘了按时勾选？“我按时完成了”在确认“你确定吗？”后给予积分（可在设置中切换）"),
            Txt("More settings, in folded sections per module: default reminders and deadline time of tasks, points, quiet hours, snooze time, week start, default event length, note text size, currency, payday as month start, start screen, date format, lock for single modules, backups on Wi-Fi only and more", "Mehr Einstellungen, eingeklappt pro Modul: Standard-Erinnerungen und -Uhrzeit von Aufgaben, Punkte, Ruhezeit, Schlummerdauer, Wochenbeginn, Standarddauer von Terminen, Schriftgröße in Notizen, Währung, Zahltag als Monatsbeginn, Startbildschirm, Datumsformat, Sperre für einzelne Module, Backup nur im WLAN und mehr", "Modül başına katlanmış bölümlerde daha fazla ayar: görevlerin varsayılan hatırlatıcıları ve saati, puanlar, sessiz saatler, erteleme süresi, hafta başlangıcı, etkinlik süresi, not yazı boyutu, para birimi, ay başı olarak maaş günü, başlangıç ekranı, tarih biçimi, tek modül kilidi, yalnızca Wi-Fi ile yedekleme ve daha fazlası", "Más ajustes, en secciones plegadas por módulo: recordatorios y hora por defecto de las tareas, puntos, horas de silencio, tiempo de posponer, inicio de semana, duración de eventos, tamaño de texto en notas, moneda, día de cobro como inicio de mes, pantalla de inicio, formato de fecha, bloqueo por módulo, copia solo con Wi-Fi y más", "更多设置，按模块折叠：任务的默认提醒和时间、积分、免打扰时段、稍后提醒时长、每周起始日、日程默认时长、笔记字号、货币、以发薪日为月初、启动页面、日期格式、单独锁定模块、仅 Wi-Fi 备份等"),
            Txt("Habits and tasks have their own notification channel, so sound and vibration can differ; reminders can be snoozed", "Gewohnheiten und Aufgaben haben eigene Benachrichtigungskanäle – Ton und Vibration lassen sich getrennt einstellen; Erinnerungen lassen sich verschieben", "Alışkanlıklar ve görevlerin ayrı bildirim kanalları var – ses ve titreşim ayrı ayarlanabilir; hatırlatıcılar ertelenebilir", "Hábitos y tareas tienen su propio canal de notificaciones, así que sonido y vibración pueden ser distintos; los recordatorios se pueden posponer", "习惯和任务有各自的通知渠道，可分别设置声音和振动；提醒可以稍后再提醒"),
            Txt("Calendar: every event without its own reminder now reminds 2 hours before (adjustable) – of several events at the same place on one day only the first", "Kalender: Jeder Termin ohne eigene Erinnerung erinnert jetzt 2 Stunden vorher (einstellbar) – bei mehreren Terminen am selben Ort an einem Tag nur der erste", "Takvim: kendi hatırlatıcısı olmayan her etkinlik artık 2 saat önce hatırlatır (ayarlanabilir) – aynı gün aynı yerdeki birden çok etkinlikte yalnızca ilki", "Calendario: cada evento sin recordatorio propio avisa ahora 2 horas antes (ajustable); de varios eventos en el mismo lugar un día, solo el primero", "日历：没有自己提醒的日程现在会提前 2 小时提醒（可调整）– 同一天同一地点的多个日程只提醒第一个"),
            Txt("Task deadlines appear in the calendar; tap one to open the task", "Deadlines von Aufgaben erscheinen im Kalender; antippen öffnet die Aufgabe", "Görev son tarihleri takvimde görünür; dokununca görev açılır", "Las fechas límite de las tareas aparecen en el calendario; al tocarlas se abre la tarea", "任务截止时间显示在日历中；点按即可打开任务"),
            Txt("Notes: main notes with sub-notes", "Notizen: Hauptnotizen mit Unternotizen", "Notlar: alt notları olan ana notlar", "Notas: notas principales con subnotas", "笔记：主笔记可包含子笔记"),
            Txt("Auto backups also go to Documents/LifeHub and survive uninstalling; after a reinstall they can be restored right in the setup", "Auto-Backups landen zusätzlich in Dokumente/LifeHub und überstehen das Deinstallieren; nach einer Neuinstallation direkt im Setup wiederherstellen", "Otomatik yedekler ayrıca Belgeler/LifeHub içine kaydedilir ve kaldırmadan sonra da kalır; yeniden kurulumdan sonra doğrudan kurulumda geri yüklenebilir", "Las copias automáticas también se guardan en Documentos/LifeHub y sobreviven a la desinstalación; tras reinstalar se restauran en la configuración", "自动备份也会保存到“文档/LifeHub”，卸载后仍保留；重新安装后可在设置向导中直接恢复"),
            Txt("PC access: when the phone is not connected the browser keeps showing your data, read only; background tabs no longer keep the phone awake", "PC-Zugriff: Ist das Handy nicht verbunden, zeigt der Browser deine Daten weiter an – nur lesen; Tabs im Hintergrund halten das Handy nicht mehr wach", "PC erişimi: telefon bağlı değilken tarayıcı verilerini göstermeye devam eder – yalnızca okuma; arka plandaki sekmeler telefonu artık uyanık tutmaz", "Acceso desde PC: si el móvil no está conectado, el navegador sigue mostrando tus datos en solo lectura; las pestañas en segundo plano ya no mantienen el móvil activo", "电脑访问：手机未连接时，浏览器会继续以只读方式显示数据；后台标签页不再让手机保持唤醒"),
            Txt("Agenda widget: more room for events, every day with a small label", "Agenda-Widget: mehr Platz für Termine, jeder Tag mit kleiner Überschrift", "Ajanda widget'ı: etkinliklere daha fazla yer, her gün küçük bir başlıkla", "Widget de agenda: más espacio para eventos, cada día con una pequeña etiqueta", "日程小部件：为日程留出更多空间，每天带一个小标签")
        )
    ),
    Release(
        "1.2", listOf(
            Txt("New: Habits & tasks – daily habits with streaks, to-dos with deadline, reminders and a home-screen widget", "Neu: Gewohnheiten & Aufgaben – tägliche Gewohnheiten mit Serien, To-dos mit Frist, Erinnerungen und Homescreen-Widget", "Yeni: Alışkanlıklar ve görevler – seri takipli günlük alışkanlıklar, son tarihli yapılacaklar, hatırlatıcılar ve ana ekran widget'ı", "Nuevo: Hábitos y tareas – hábitos diarios con rachas, tareas con fecha límite, recordatorios y widget", "新功能：习惯与任务 – 带连续记录的每日习惯、有截止日期的待办、提醒和桌面小部件"),
            Txt("Switch modules on and off in the settings (Waypoints, Documents, Finance, Habits) – the data stays", "Module in den Einstellungen ein- und ausschalten (Waypoints, Dokumente, Finanzen, Gewohnheiten) – die Daten bleiben", "Modülleri ayarlardan aç/kapat (Waypoints, Belgeler, Finans, Alışkanlıklar) – veriler kalır", "Activa o desactiva módulos en los ajustes (Waypoints, Documentos, Finanzas, Hábitos): los datos se conservan", "可在设置中开启或关闭模块（地点、文档、财务、习惯）– 数据会保留"),
            Txt("New look for the agenda widget: times, places, what's on now and + for a new event", "Neues Agenda-Widget: Uhrzeiten, Orte, was gerade läuft und + für einen neuen Termin", "Yeni ajanda widget'ı: saatler, yerler, şu an olanlar ve yeni etkinlik için +", "Nuevo widget de agenda: horas, lugares, lo que está pasando y + para un nuevo evento", "全新日程小部件：时间、地点、正在进行的日程，以及用于新建日程的 +"),
            Txt("Finance: delete an entry from its edit dialog", "Finanzen: Einträge im Bearbeiten-Dialog löschen", "Finans: kayıtlar düzenleme penceresinden silinebilir", "Finanzas: borra un movimiento desde su diálogo de edición", "财务：可在编辑对话框中删除记录"),
            Txt("Updates are found sooner: checked on every start and after longer breaks", "Updates werden schneller gefunden: Prüfung bei jedem Start und nach längeren Pausen", "Güncellemeler daha hızlı bulunur: her açılışta ve uzun aralardan sonra kontrol edilir", "Las actualizaciones se encuentran antes: se comprueba en cada inicio y tras pausas largas", "更快发现更新：每次启动及较长时间未使用后都会检查")
        )
    ),
    Release(
        "1.1.1", listOf(
            Txt("Info hints (ⓘ) explain settings and features", "Info-Hinweise (ⓘ) erklären Einstellungen und Funktionen", "Bilgi ipuçları (ⓘ) ayarları ve özellikleri açıklar", "Pistas informativas (ⓘ) explican ajustes y funciones", "信息提示（ⓘ）解释设置和功能"),
            Txt("Small phones: scrollable menu, wrapping buttons, Smart Add above the keyboard", "Kleine Handys: scrollbares Menü, umbrechende Buttons, Smart Add über der Tastatur", "Küçük telefonlar: kaydırılabilir menü, sığan düğmeler, klavyenin üstünde Akıllı Ekle", "Móviles pequeños: menú desplazable, botones que se ajustan, Añadir inteligente sobre el teclado", "小屏手机：菜单可滚动、按钮自动换行、智能添加显示在键盘上方"),
            Txt("Delete buttons look the same everywhere", "Löschen-Buttons sehen überall gleich aus", "Silme düğmeleri her yerde aynı görünür", "Los botones de borrar se ven igual en todas partes", "删除按钮在各处外观统一"),
            Txt("Status bar icons visible in the light theme", "Statusleisten-Symbole im hellen Design sichtbar", "Açık temada durum çubuğu simgeleri görünür", "Iconos de la barra de estado visibles en el tema claro", "浅色主题下状态栏图标清晰可见"),
            Txt("PC access: LifeHub logo in the browser tab, input fields aligned", "PC-Zugriff: LifeHub-Logo im Browser-Tab, Eingabefelder gleich hoch", "PC erişimi: tarayıcı sekmesinde LifeHub logosu, giriş alanları hizalı", "Acceso desde PC: logo de LifeHub en la pestaña, campos alineados", "电脑访问：浏览器标签页显示 LifeHub 标志，输入框对齐"),
            Txt("Missing Turkish and Spanish translation added", "Fehlende türkische und spanische Übersetzung ergänzt", "Eksik Türkçe ve İspanyolca çeviri eklendi", "Añadida la traducción al turco y al español que faltaba", "补充了缺失的土耳其语和西班牙语翻译")
        )
    ),
    Release(
        "1.1", listOf(
            Txt("New language: Simplified Chinese; patch notes in all languages", "Neue Sprache: vereinfachtes Chinesisch; Patchnotes in allen Sprachen", "Yeni dil: Basitleştirilmiş Çince; sürüm notları tüm dillerde", "Nuevo idioma: chino simplificado; notas de versión en todos los idiomas", "新增语言：简体中文；更新说明提供所有语言版本"),
            Txt("New: Finance – income, expenses and what's left each month", "Neu: Finanzen – Einnahmen, Ausgaben und Rest pro Monat", "Yeni: Finans – aylık gelir, gider ve kalan", "Nuevo: Finanzas – ingresos, gastos y lo que queda cada mes", "新功能：财务 – 每月收入、支出和结余"),
            Txt("Categories with chart and monthly budgets", "Kategorien mit Diagramm und Budgets", "Grafikli kategoriler ve aylık bütçeler", "Categorías con gráfico y presupuestos mensuales", "带图表和月度预算的分类"),
            Txt("Fixed costs book themselves every month, optionally shown in the calendar", "Fixkosten buchen sich jeden Monat selbst, auf Wunsch im Kalender", "Sabit giderler her ay kendiliğinden kaydedilir, istersen takvimde", "Los gastos fijos se registran solos cada mes, opcionalmente en el calendario", "固定支出每月自动记账，可选择在日历中显示"),
            Txt("Import bank statements as CSV (Sparkasse, ING, DKB, Volksbank, N26, Commerzbank and more)", "Kontoauszüge als CSV importieren (Sparkasse, ING, DKB, Volksbank, N26, Commerzbank u. a.)", "Banka hesap özetlerini CSV olarak içe aktar (Sparkasse, ING, DKB, Volksbank, N26, Commerzbank vb.)", "Importa extractos bancarios en CSV (Sparkasse, ING, DKB, Volksbank, N26, Commerzbank y más)", "以 CSV 导入银行流水（Sparkasse、ING、DKB、Volksbank、N26、Commerzbank 等）"),
            Txt("Smart Add understands expenses: “12,50 Döner gestern”", "Smart Add versteht Ausgaben: „12,50 Döner gestern“", "Akıllı Ekle harcamaları anlar: “12,50 Döner gestern”", "Añadir inteligente entiende gastos: «12,50 Döner gestern»", "智能添加能识别支出：“12,50 Döner gestern”"),
            Txt("New: PC access – use calendar, notes and finance in the browser on your PC (same Wi-Fi, data stays on the phone)", "Neu: PC-Zugriff – Kalender, Notizen und Finanzen im Browser am PC nutzen (gleiches WLAN, Daten bleiben auf dem Handy)", "Yeni: PC erişimi – takvim, notlar ve finansı bilgisayardaki tarayıcıda kullan (aynı Wi-Fi, veriler telefonda kalır)", "Nuevo: acceso desde PC – usa calendario, notas y finanzas en el navegador del PC (misma Wi-Fi, los datos se quedan en el móvil)", "新功能：电脑访问 – 在电脑浏览器中使用日历、笔记和财务（同一 Wi-Fi，数据保留在手机上）"),
            Txt("Notes: lists reliably continue after Enter", "Notizen: Listen werden nach Enter zuverlässig fortgesetzt", "Notlar: listeler Enter'dan sonra güvenilir şekilde devam eder", "Notas: las listas continúan correctamente tras Intro", "笔记：按回车后列表可靠地延续")
        )
    ),
    Release(
        "1.0.1", listOf(
            Txt("Backup screen tidied up: import from the old apps removed (LifeHub backup and .ics import remain)", "Backup-Bildschirm aufgeräumt: Import aus den alten Apps entfernt (LifeHub-Backup und .ics-Import bleiben)", "Yedek ekranı sadeleşti: eski uygulamalardan içe aktarma kaldırıldı", "Pantalla de copia simplificada: se quitó la importación de las apps antiguas", "备份界面已整理：移除了从旧应用导入（保留 LifeHub 备份和 .ics 导入）")
        )
    ),
    Release(
        "1.0", listOf(
            Txt("First stable version", "Erste stabile Version", "İlk kararlı sürüm", "Primera versión estable", "首个稳定版本"),
            Txt("New app icon and logo", "Neues App-Icon und Logo", "Yeni uygulama simgesi ve logo", "Nuevo icono y logo", "新的应用图标和标志"),
            Txt("Agenda widget: more room for events, days translated", "Agenda-Widget: mehr Platz für Termine, Tage übersetzt", "Ajanda widget: etkinliklere daha fazla yer, günler çevrildi", "Widget de agenda: más espacio, días traducidos", "日程小部件：为日程留出更多空间，星期已翻译"),
            Txt("Reminders and their notifications are shown in your language", "Erinnerungen und ihre Benachrichtigungen erscheinen in deiner Sprache", "Hatırlatıcılar ve bildirimleri kendi dilinde", "Recordatorios y avisos en tu idioma", "提醒及其通知以你的语言显示")
        )
    ),
    Release(
        "0.10", listOf(
            Txt("LifeOrganizer is now called LifeHub (new app – move your data once via backup)", "LifeOrganizer heißt jetzt LifeHub (neue App – Daten einmalig per Backup umziehen)", "LifeOrganizer artık LifeHub (yeni uygulama – verileri bir kez yedekle taşı)", "LifeOrganizer ahora es LifeHub (app nueva: mueve tus datos con una copia)", "LifeOrganizer 现更名为 LifeHub（新应用 – 通过备份一次性迁移数据）"),
            Txt("App is much smaller (about 18 MB instead of 62 MB)", "App ist deutlich kleiner (ca. 18 MB statt 62 MB)", "Uygulama çok daha küçük (~18 MB)", "La app es mucho más pequeña (~18 MB)", "应用体积大幅缩小（约 18 MB，原为 62 MB）"),
            Txt("All texts translated – no more English mix", "Alle Texte übersetzt – kein Englisch-Mix mehr", "Tüm metinler çevrildi", "Todos los textos traducidos", "所有文本均已翻译 – 不再混杂英文"),
            Txt("Backup now includes settings and documents attached to notes", "Backup enthält jetzt Einstellungen und an Notizen angehängte Dokumente", "Yedek artık ayarları ve notlara ekli belgeleri içerir", "La copia incluye ajustes y documentos de notas", "备份现包含设置和附加到笔记的文档"),
            Txt("Crash screen with 'share report'", "Absturz-Bildschirm mit „Bericht teilen“", "'Raporu paylaş' ile çökme ekranı", "Pantalla de error con 'compartir informe'", "崩溃界面，可“分享报告”"),
            Txt("About & privacy with open-source licences", "Über die App & Datenschutz mit Open-Source-Lizenzen", "Hakkında ve gizlilik, açık kaynak lisansları", "Acerca de y privacidad con licencias", "关于与隐私，含开源许可"),
            Txt("Better with large font sizes", "Besser bei großer Schrift", "Büyük yazı boyutunda daha iyi", "Mejor con letra grande", "更好地支持大字体"),
            Txt("Landscape and tablets: readable width, compact month, more room in the note editor", "Querformat & Tablets: lesbare Breite, kompakter Monat, mehr Platz im Notiz-Editor", "Yatay ve tablet: okunur genişlik, kompakt ay, not düzenleyicide daha fazla alan", "Horizontal y tablets: ancho legible, mes compacto, más espacio en notas", "横屏和平板：易读的宽度、紧凑的月视图、笔记编辑器空间更大"),
            Txt("Fixed: backup failed after reinstalling", "Behoben: Backup schlug nach Neuinstallation fehl", "Düzeltildi: yeniden kurulumdan sonra yedekleme hatası", "Corregido: copia fallaba tras reinstalar", "修复：重新安装后备份失败")
        )
    ),
    Release(
        "0.9", listOf(
            Txt("First start: setup for language and design", "Erster Start: Einrichtung von Sprache und Design", "İlk açılış: dil ve tasarım kurulumu", "Primer inicio: configuración de idioma y diseño", "首次启动：设置语言和外观"),
            Txt("Patch notes after updates and a full changelog in the menu", "Patchnotes nach Updates und komplettes Changelog im Menü", "Güncellemeden sonra yenilikler ve menüde tüm değişiklikler", "Novedades tras actualizar y registro completo en el menú", "更新后显示更新说明，菜单中有完整的更新日志"),
            Txt("Update check: download the newest version from GitHub in the app", "Update-Prüfung: neueste Version direkt in der App von GitHub laden", "Güncelleme: en yeni sürümü uygulamada GitHub'dan indir", "Actualizar: descarga la última versión de GitHub en la app", "检查更新：在应用内从 GitHub 下载最新版本"),
            Txt("Dokki is now simply called Documents", "Dokki heißt jetzt einfach Dokumente", "Dokki artık Belgeler", "Dokki ahora se llama Documentos", "Dokki 现在简称为“文档”"),
            Txt("Calendar filter: hide repeating events or categories (e.g. work, uni) live", "Kalender-Filter: Wiederholungen oder Kategorien (z. B. Arbeit, Uni) live ausblenden", "Takvim filtresi: tekrarları veya kategorileri canlı gizle", "Filtro: oculta eventos periódicos o categorías al instante", "日历筛选：实时隐藏重复日程或分类（如工作、学校）"),
            Txt("Events save automatically when the dialog is closed", "Termine speichern beim Schließen automatisch", "Etkinlikler kapatınca otomatik kaydedilir", "Los eventos se guardan al cerrar", "关闭对话框时日程自动保存"),
            Txt("Repeating events: 'this and following' is the default when deleting", "Wiederholungen: „Diesen und alle folgenden“ ist beim Löschen Standard", "Tekrarlar: silerken 'bu ve sonrakiler' varsayılan", "Periódicos: 'este y los siguientes' por defecto al borrar", "重复日程：删除时默认“此日程及之后”"),
            Txt("Notes: tick checklist items on the card, checkbox button ticks while editing, formatting help", "Notizen: Listenpunkte auf der Karte abhaken, Checkbox-Knopf hakt beim Bearbeiten ab, Formatierungshilfe", "Notlar: kartta işaretle, düzenlerken onay düğmesi, biçim yardımı", "Notas: marca en la tarjeta, botón de casilla al editar, ayuda de formato", "笔记：在卡片上勾选清单项，编辑时用复选框按钮勾选，格式帮助"),
            Txt("Copy an event into a phone calendar", "Termin in den Gerätekalender kopieren", "Etkinliği telefon takvimine kopyala", "Copiar un evento al calendario del teléfono", "将日程复制到手机日历"),
            Txt("Attach documents to notes", "Dokumente an Notizen anhängen", "Notlara belge ekle", "Adjuntar documentos a notas", "为笔记附加文档")
        )
    ),
    Release(
        "0.8", listOf(
            Txt("Repeating events: edit or delete this, following or all", "Wiederholungen: nur diesen, folgende oder alle bearbeiten/löschen", "Tekrarlar: bunu, sonrakileri veya hepsini düzenle", "Periódicos: editar este, siguientes o todos", "重复日程：编辑或删除此日程、之后的或全部"),
            Txt("Phone calendars shown read-only", "Gerätekalender werden angezeigt (nur lesen)", "Telefon takvimleri gösterilir (salt okunur)", "Calendarios del teléfono visibles (solo lectura)", "手机日历以只读方式显示"),
            Txt("Weekly automatic backup, optional copy to your own folder", "Wöchentliches Auto-Backup, optional Kopie in eigenen Ordner", "Haftalık otomatik yedek, isteğe bağlı kendi klasörüne", "Copia semanal automática, opcional en tu carpeta", "每周自动备份，可选复制到你自己的文件夹"),
            Txt("Smart Add works offline without API key", "Smart Add funktioniert offline ohne API-Key", "Akıllı Ekle API anahtarı olmadan çevrimdışı çalışır", "Smart Add funciona sin conexión ni clave", "智能添加无需 API 密钥即可离线使用"),
            Txt("Label templates, template placeholders, note widget", "Label-Vorlagen, Platzhalter, Notiz-Widget", "Etiket şablonları, yer tutucular, not widget'ı", "Plantillas por etiqueta, marcadores, widget de notas", "标签模板、模板占位符、笔记小部件"),
            Txt("Fixed: repeating events multiplied after .ics import", "Behoben: Serien vervielfachten sich nach .ics-Import", "Düzeltildi: .ics içe aktarmada tekrarlar çoğalıyordu", "Corregido: eventos periódicos duplicados al importar .ics", "修复：导入 .ics 后重复日程成倍增加")
        )
    )
)

private object WhatsNewPrefs {
    private fun prefs(c: Context) = c.getSharedPreferences("whats_new", Context.MODE_PRIVATE)
    fun lastSeen(c: Context) = prefs(c).getString("last_seen", null)
    fun setLastSeen(c: Context, v: String) = prefs(c).edit().putString("last_seen", v).apply()
    fun enabled(c: Context) = prefs(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, on: Boolean) = prefs(c).edit().putBoolean("enabled", on).apply()
}

/** Shows "What's new" once after an update (can be switched off). Fresh installs see the setup instead. */
@Composable
fun WhatsNewAfterUpdate(context: Context, setupDone: Boolean) {
    val current = BuildConfig.VERSION_NAME
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(setupDone) {
        if (!setupDone) return@LaunchedEffect
        val last = WhatsNewPrefs.lastSeen(context)
        if (last != current) {
            show = last != null && WhatsNewPrefs.enabled(context) && CHANGELOG.any { it.version == current }
            WhatsNewPrefs.setLastSeen(context, current)
        }
    }
    if (show) {
        ChangelogDialog(onlyVersion = current, onDismiss = { show = false })
    }
}

@Composable
fun ChangelogDialog(onlyVersion: String? = null, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var enabled by remember { mutableStateOf(WhatsNewPrefs.enabled(context)) }
    val releases = CHANGELOG.filter { onlyVersion == null || it.version == onlyVersion }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (onlyVersion != null) Str.whatsNew.text().format(onlyVersion) else Str.changelog.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                releases.forEach { r ->
                    if (onlyVersion == null) {
                        Text("Version ${r.version}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    }
                    r.items.forEach { item -> Text("• ${item.text()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    Text(Str.showAfterUpdates.text(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it; WhatsNewPrefs.setEnabled(context, it) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Str.done.text()) } }
    )
}

// ============================================================================ setup

/** First start: language and design, applied live so the user sees the result immediately. */
@Composable
fun SetupScreen(settings: SettingsManager, onRestore: ((android.net.Uri) -> Unit)? = null, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    // After a reinstall: pick a backup (the picker opens in Documents/LifeHub, where auto backups outlive the app)
    val restorePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        object : androidx.activity.result.contract.ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: Context, input: Array<String>) = super.createIntent(context, input).putExtra(
                android.provider.DocumentsContract.EXTRA_INITIAL_URI,
                android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FLifeHub")
            )
        }
    ) { uri ->
        if (uri != null) scope.launch {
            settings.saveOnboardingCompleted(true)
            onRestore?.invoke(uri)
            onFinish()
        }
    }
    val language by settings.languageCode.collectAsState(initial = "de")
    val style by settings.designStyle.collectAsState(initial = "pastel")
    val dark by settings.isDarkTheme.collectAsState(initial = null)
    val accent by settings.accentColor.collectAsState(initial = null)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(Str.appSetupTitle.text(), style = MaterialTheme.typography.headlineMedium)
            Text(Str.setupSubtitle.text(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onRestore != null) {
                OutlinedButton(onClick = { restorePicker.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(com.example.lifeorganizer.core.i18n.BkStr.restoreBackup.text())
                }
                Text(
                    com.example.lifeorganizer.core.i18n.BkStr.restoreBackupHint.text(),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Section(Str.language.text()) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("de" to "Deutsch", "en" to "English", "tr" to "Türkçe", "es" to "Español", "zh" to "简体中文").forEach { (code, name) ->
                        FilterChip(selected = language == code, onClick = { scope.launch { settings.saveLanguageCode(code) } }, label = { Text(name) })
                    }
                }
            }

            Section(Str.design.text()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "pastel" to listOf(Color(0xFF90CAF9), Color(0xFFF48FB1), Color(0xFFFFF8F5)),
                        "midnight" to listOf(Color(0xFF0B1020), Color(0xFF3D5AFE), Color(0xFF8C9EFF)),
                        "neon" to listOf(Color(0xFF0A0A0A), Color(0xFF00FFB2), Color(0xFFFF007F)),
                        "white_light" to listOf(Color(0xFFFFFFFF), Color(0xFF111111), Color(0xFFE0E0E0))
                    ).forEach { (key, colors) ->
                        val selected = style == key
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(16.dp))
                                .clickable { scope.launch { settings.saveDesignStyle(key); settings.saveAccentColor(null) } }
                                .padding(14.dp)
                        ) {
                            colors.forEach { c -> Box(Modifier.padding(end = 6.dp).size(22.dp).background(c, CircleShape).border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)) }
                            Spacer(Modifier.width(8.dp))
                            Text(styleName(key), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            RadioButton(selected = selected, onClick = null)
                        }
                    }
                }
            }

            if (style == "pastel") {
                Section(Str.brightness.text()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to Str.system, false to Str.light, true to Str.dark).forEach { (value, label) ->
                            FilterChip(selected = dark == value, onClick = { scope.launch { settings.saveIsDarkTheme(value) } }, label = { Text(label.text()) })
                        }
                    }
                }
            }
            if (style == "pastel" || style == "neon") {
                Section(Str.accent.text()) {
                    val options = if (style == "pastel") listOf(null, 0xFFF48FB1.toInt(), 0xFFCE93D8.toInt(), 0xFF90CAF9.toInt(), 0xFF80CBC4.toInt(), 0xFFFFCC80.toInt())
                    else listOf(null, 0xFF00FFB2.toInt(), 0xFF00E5FF.toInt(), 0xFFD4FF00.toInt(), 0xFFD500F9.toInt(), 0xFFFF007F.toInt())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        options.forEach { c ->
                            val shown = Color(c ?: options[1]!!)
                            Box(
                                Modifier.size(36.dp).background(shown, CircleShape)
                                    .border(if (accent == c) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .clickable { scope.launch { settings.saveAccentColor(c) } }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { scope.launch { settings.saveOnboardingCompleted(true); onFinish() } },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(Str.getStarted.text()) }
            Text(Str.setupLater.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

private fun styleName(key: String) = when (key) {
    "midnight" -> "Midnight"
    "neon" -> "Neon"
    "white_light" -> "White Light"
    else -> "Pastel"
}
