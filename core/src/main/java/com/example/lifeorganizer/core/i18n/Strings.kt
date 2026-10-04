package com.example.lifeorganizer.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Current UI language code ("en", "de", "tr", "es"), provided once at the app root. */
val LocalAppLanguage = staticCompositionLocalOf { "en" }

val SupportedLanguages = listOf("en", "de", "tr", "es")

class Txt(val en: String, val de: String, val tr: String, val es: String) {
    fun of(lang: String): String = when (lang) {
        "de" -> de
        "tr" -> tr
        "es" -> es
        else -> en
    }
}

@Composable
@ReadOnlyComposable
fun Txt.text(): String = of(LocalAppLanguage.current)

/** Shared UI strings for the app shell, notes, search and Smart Add. */
object Str {
    // Navigation
    val calendar = Txt("Calendar", "Kalender", "Takvim", "Calendario")
    val notes = Txt("Notes", "Notizen", "Notlar", "Notas")
    val waypoints = Txt("Waypoints", "Waypoints", "Konumlar", "Lugares")
    val documents = Txt("Dokki (Documents)", "Dokki (Dokumente)", "Dokki (Belgeler)", "Dokki (Documentos)")
    val settings = Txt("Settings", "Einstellungen", "Ayarlar", "Ajustes")
    val menu = Txt("Menu", "Menü", "Menü", "Menú")
    val back = Txt("Back", "Zurück", "Geri", "Atrás")
    val tagline = Txt("Your day, organised.", "Dein Alltag, organisiert.", "Günün, düzenli.", "Tu día, organizado.")
    val organise = Txt("Organise", "Organisieren", "Düzenle", "Organizar")
    val tools = Txt("Tools", "Werkzeuge", "Araçlar", "Herramientas")
    val switchToNotes = Txt("Switch to notes", "Zu Notizen wechseln", "Notlara geç", "Cambiar a notas")
    val switchToCalendar = Txt("Switch to calendar", "Zum Kalender wechseln", "Takvime geç", "Cambiar al calendario")
    val newEvent = Txt("New event", "Neuer Termin", "Yeni etkinlik", "Nuevo evento")
    val newNote = Txt("New note", "Neue Notiz", "Yeni not", "Nueva nota")
    val smartAdd = Txt("Smart Add", "Smart Add", "Akıllı Ekle", "Añadir inteligente")

    // Universal search
    val searchEverywhere = Txt("Search everywhere…", "Überall suchen…", "Her yerde ara…", "Buscar en todo…")
    val searchHint = Txt(
        "Search events, notes, waypoints and documents at once",
        "Durchsucht Termine, Notizen, Waypoints und Dokumente gleichzeitig",
        "Etkinlikleri, notları, konumları ve belgeleri birlikte ara",
        "Busca eventos, notas, lugares y documentos a la vez"
    )
    val noResults = Txt("No results", "Keine Treffer", "Sonuç yok", "Sin resultados")
    val events = Txt("Events", "Termine", "Etkinlikler", "Eventos")
    val untitled = Txt("Untitled", "Ohne Titel", "Başlıksız", "Sin título")
    val clear = Txt("Clear", "Löschen", "Temizle", "Borrar")

    // Notes list
    val pinned = Txt("Pinned", "Angeheftet", "Sabitlenmiş", "Fijadas")
    val all = Txt("All", "Alle", "Tümü", "Todas")
    val trash = Txt("Trash", "Papierkorb", "Çöp kutusu", "Papelera")
    val labels = Txt("Labels", "Labels", "Etiketler", "Etiquetas")
    val manageLabels = Txt("Manage labels", "Labels verwalten", "Etiketleri yönet", "Gestionar etiquetas")
    val templates = Txt("Templates", "Vorlagen", "Şablonlar", "Plantillas")
    val manageTemplates = Txt("Manage templates", "Vorlagen verwalten", "Şablonları yönet", "Gestionar plantillas")
    val sort = Txt("Sort", "Sortieren", "Sırala", "Ordenar")
    val sortUpdated = Txt("Last edited", "Zuletzt bearbeitet", "Son düzenlenen", "Última edición")
    val sortCreated = Txt("Newest", "Neueste", "En yeni", "Más recientes")
    val sortTitle = Txt("Title A–Z", "Titel A–Z", "Başlık A–Z", "Título A–Z")
    val sortColor = Txt("Color", "Farbe", "Renk", "Color")
    val emptyNotes = Txt(
        "No notes yet.\nTap + to write your first one.",
        "Noch keine Notizen.\nTippe auf +, um loszulegen.",
        "Henüz not yok.\nİlk notun için + simgesine dokun.",
        "Aún no hay notas.\nToca + para escribir la primera."
    )
    val trashEmpty = Txt("Trash is empty", "Papierkorb ist leer", "Çöp kutusu boş", "La papelera está vacía")
    val emptyTrash = Txt("Empty trash", "Papierkorb leeren", "Çöpü boşalt", "Vaciar papelera")
    val emptyTrashConfirm = Txt(
        "Permanently delete all notes in the trash?",
        "Alle Notizen im Papierkorb endgültig löschen?",
        "Çöp kutusundaki tüm notlar kalıcı olarak silinsin mi?",
        "¿Eliminar definitivamente todas las notas de la papelera?"
    )
    val deleteForeverConfirm = Txt(
        "Permanently delete this note?",
        "Diese Notiz endgültig löschen?",
        "Bu not kalıcı olarak silinsin mi?",
        "¿Eliminar definitivamente esta nota?"
    )
    val notesMovedToTrash = Txt("%d notes moved to trash", "%d Notizen in den Papierkorb verschoben", "%d not çöp kutusuna taşındı", "%d notas movidas a la papelera")
    val fileUnavailable = Txt(
        "This file can't be opened. It may have been moved or deleted, or access was lost – add it again.",
        "Diese Datei lässt sich nicht öffnen. Sie wurde evtl. verschoben/gelöscht oder der Zugriff ging verloren – bitte neu hinzufügen.",
        "Bu dosya açılamıyor. Taşınmış/silinmiş olabilir ya da erişim kaybolmuş olabilir – yeniden ekle.",
        "No se puede abrir el archivo. Puede que se haya movido o borrado, o se perdió el acceso: vuelve a añadirlo."
    )
    val selected = Txt("selected", "ausgewählt", "seçildi", "seleccionadas")
    val unpinNote = Txt("Unpin", "Lösen", "Sabitlemeyi kaldır", "Desfijar")
    val noNotesWithLabel = Txt(
        "No notes with this label.",
        "Keine Notizen mit diesem Label.",
        "Bu etikete sahip not yok.",
        "No hay notas con esta etiqueta."
    )
    val restore = Txt("Restore", "Wiederherstellen", "Geri yükle", "Restaurar")
    val deleteForever = Txt("Delete forever", "Endgültig löschen", "Kalıcı olarak sil", "Eliminar definitivamente")
    val movedToTrash = Txt("Moved to trash", "In den Papierkorb verschoben", "Çöp kutusuna taşındı", "Movida a la papelera")
    val undo = Txt("Undo", "Rückgängig", "Geri al", "Deshacer")
    val showDeviceCalendars = Txt("Show phone calendars", "Gerätekalender anzeigen", "Telefon takvimlerini göster", "Mostrar calendarios del teléfono")
    val showDeviceCalendarsDesc = Txt(
        "Google, Samsung & co. – read-only",
        "Google, Samsung & Co. – nur lesen",
        "Google, Samsung vb. – salt okunur",
        "Google, Samsung, etc. – solo lectura"
    )
    val deviceEventReadOnly = Txt(
        "This event is from your phone calendar – change it there.",
        "Dieser Termin stammt aus dem Gerätekalender – bitte dort ändern.",
        "Bu etkinlik telefon takviminden – orada değiştir.",
        "Este evento es del calendario del teléfono: cámbialo allí."
    )
    val editRepeating = Txt("Edit repeating event", "Wiederkehrenden Termin bearbeiten", "Tekrarlanan etkinliği düzenle", "Editar evento periódico")
    val deleteRepeating = Txt("Delete repeating event", "Wiederkehrenden Termin löschen", "Tekrarlanan etkinliği sil", "Eliminar evento periódico")
    val thisEventOnly = Txt("This event", "Nur diesen Termin", "Yalnızca bu etkinlik", "Solo este evento")
    val thisAndFollowing = Txt("This and following events", "Diesen und alle folgenden", "Bu ve sonrakiler", "Este y los siguientes")
    val allEvents = Txt("All events", "Alle Termine der Serie", "Tüm etkinlikler", "Todos los eventos")
    val checkedToBottom = Txt("Move checked to bottom", "Erledigte nach unten", "İşaretlileri alta taşı", "Marcados al final")
    val placeholderHint = Txt(
        "Placeholders: {{date}}, {{weekday}}, {{time}}",
        "Platzhalter: {{datum}}, {{wochentag}}, {{uhrzeit}}",
        "Yer tutucular: {{tarih}}, {{gun}}, {{saat}}",
        "Marcadores: {{fecha}}, {{dia}}, {{hora}}"
    )
    val uncheckAll = Txt("Uncheck all", "Alle Haken entfernen", "Tüm işaretleri kaldır", "Desmarcar todo")
    val labelTemplate = Txt("Template for new notes", "Vorlage für neue Notizen", "Yeni notlar için şablon", "Plantilla para notas nuevas")
    val labelTemplateNone = Txt("No template – tap to add one", "Keine Vorlage – tippen zum Anlegen", "Şablon yok – eklemek için dokun", "Sin plantilla: toca para añadir")
    val addChecklistItem = Txt("Checklist item", "Listenpunkt", "Liste öğesi", "Elemento de lista")
    val newLabel = Txt("New label", "Neues Label", "Yeni etiket", "Nueva etiqueta")
    val add = Txt("Add", "Hinzufügen", "Ekle", "Añadir")
    val delete = Txt("Delete", "Löschen", "Sil", "Eliminar")
    val cancel = Txt("Cancel", "Abbrechen", "İptal", "Cancelar")
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar")
    val done = Txt("Done", "Fertig", "Tamam", "Listo")
    val noLabels = Txt("No labels yet", "Noch keine Labels", "Henüz etiket yok", "Aún no hay etiquetas")
    val noTemplates = Txt(
        "No templates yet. Open a note and choose “Save as template”.",
        "Noch keine Vorlagen. Öffne eine Notiz und wähle „Als Vorlage speichern“.",
        "Henüz şablon yok. Bir not açıp “Şablon olarak kaydet”i seç.",
        "Aún no hay plantillas. Abre una nota y elige «Guardar como plantilla»."
    )
    val templateName = Txt("Template name", "Name der Vorlage", "Şablon adı", "Nombre de la plantilla")
    val blankNote = Txt("Blank note", "Leere Notiz", "Boş not", "Nota en blanco")
    val chooseTemplate = Txt("Start from…", "Starten mit…", "Şununla başla…", "Empezar con…")

    // Note editor
    val title = Txt("Title", "Titel", "Başlık", "Título")
    val startWriting = Txt(
        "Start writing… Markdown supported. Use - [ ] for checkboxes.",
        "Schreib los… Markdown wird unterstützt. - [ ] erzeugt Checkboxen.",
        "Yazmaya başla… Markdown desteklenir. Onay kutuları için - [ ] kullan.",
        "Empieza a escribir… Admite Markdown. Usa - [ ] para casillas."
    )
    val edit = Txt("Edit", "Bearbeiten", "Düzenle", "Editar")
    val preview = Txt("Preview", "Vorschau", "Önizleme", "Vista previa")
    val words = Txt("words", "Wörter", "kelime", "palabras")
    val chars = Txt("chars", "Zeichen", "karakter", "caracteres")
    val pinToDate = Txt("Pin to calendar day", "An Kalendertag heften", "Takvim gününe sabitle", "Fijar a un día")
    val pinnedTo = Txt("Pinned to", "Angeheftet an", "Sabitlendi:", "Fijada al")
    val unpin = Txt("Remove", "Entfernen", "Kaldır", "Quitar")
    val pinNote = Txt("Pin note", "Notiz anheften", "Notu sabitle", "Fijar nota")
    val color = Txt("Color", "Farbe", "Renk", "Color")
    val noColor = Txt("None", "Keine", "Yok", "Ninguno")
    val reminder = Txt("Reminder", "Erinnerung", "Hatırlatıcı", "Recordatorio")
    val setReminder = Txt("Set reminder", "Erinnerung setzen", "Hatırlatıcı kur", "Crear recordatorio")
    val reminderCreated = Txt(
        "Reminder added to your calendar",
        "Erinnerung im Kalender angelegt",
        "Hatırlatıcı takvime eklendi",
        "Recordatorio añadido al calendario"
    )
    val saveAsTemplate = Txt("Save as template", "Als Vorlage speichern", "Şablon olarak kaydet", "Guardar como plantilla")
    val templateSaved = Txt("Template saved", "Vorlage gespeichert", "Şablon kaydedildi", "Plantilla guardada")
    val more = Txt("More", "Mehr", "Daha fazla", "Más")
    val bold = Txt("Bold", "Fett", "Kalın", "Negrita")
    val italic = Txt("Italic", "Kursiv", "İtalik", "Cursiva")
    val heading = Txt("Heading", "Überschrift", "Başlık", "Encabezado")
    val bulletList = Txt("List", "Liste", "Liste", "Lista")
    val checkbox = Txt("Checkbox", "Checkbox", "Onay kutusu", "Casilla")
    val ok = Txt("OK", "OK", "Tamam", "Aceptar")
    val next = Txt("Next", "Weiter", "İleri", "Siguiente")
    val reminderPrefix = Txt("Note", "Notiz", "Not", "Nota")
    val openNote = Txt("Open note", "Notiz öffnen", "Notu aç", "Abrir nota")

    // Calendar
    val leaveAt = Txt("Leave at", "Losfahren um", "Çıkış saati", "Salir a las")
    val minShort = Txt("min", "Min.", "dk", "min")
    val navigate = Txt("Navigate", "Navigation starten", "Yol tarifi", "Navegar")
    val pinnedNotes = Txt("Pinned notes", "Angeheftete Notizen", "Sabitlenmiş notlar", "Notas fijadas")
    val reminders = Txt("reminders", "Erinnerungen", "hatırlatıcı", "recordatorios")

    // Settings
    val designStyle = Txt("Design style", "Designstil", "Tasarım stili", "Estilo de diseño")
    val locationNavigation = Txt("Location & navigation", "Standort & Navigation", "Konum ve navigasyon", "Ubicación y navegación")
    val appPreferences = Txt("App preferences", "App-Einstellungen", "Uygulama tercihleri", "Preferencias de la app")
    val notificationSound = Txt("Notification sound", "Benachrichtigungston", "Bildirim sesi", "Sonido de notificación")
    val soundDefault = Txt("Default", "Standard", "Varsayılan", "Predeterminado")
    val soundCustom = Txt("Custom", "Benutzerdefiniert", "Özel", "Personalizado")
    val categories = Txt("Categories", "Kategorien", "Kategoriler", "Categorías")
    val categoriesDesc = Txt("Manage event categories and colors", "Termin-Kategorien und Farben verwalten", "Etkinlik kategorilerini ve renklerini yönet", "Gestiona categorías y colores")
    val advancedExport = Txt("Advanced & export", "Erweitert & Export", "Gelişmiş ve dışa aktarma", "Avanzado y exportación")
    val groqKey = Txt("Groq API key (for Smart Add)", "Groq-API-Key (für Smart Add)", "Groq API anahtarı (Akıllı Ekle için)", "Clave API de Groq (Smart Add)")
    val enableSmartAlarms = Txt("Smart alarms (leave-now alerts)", "Smart-Alarme (Losfahr-Wecker)", "Akıllı alarmlar (çıkış uyarısı)", "Alarmas inteligentes (hora de salir)")
    val importResult = Txt("Imported %1\$d events, skipped %2\$d duplicates.", "%1\$d Termine importiert, %2\$d Duplikate übersprungen.", "%1\$d etkinlik içe aktarıldı, %2\$d kopya atlandı.", "%1\$d eventos importados, %2\$d duplicados omitidos.")

    // Smart Add setup (API key)
    val setupTitle = Txt("Smart Add setup", "Smart Add einrichten", "Akıllı Ekle kurulumu", "Configurar Smart Add")
    val privacyNotice = Txt("Privacy notice", "Datenschutzhinweis", "Gizlilik bildirimi", "Aviso de privacidad")
    val privacyText = Txt(
        "Smart Add sends the text you enter (and text read from screenshots) to Groq's servers, where an AI model extracts title, time and place. Avoid entering highly sensitive information.",
        "Smart Add sendet den eingegebenen Text (und aus Screenshots gelesenen Text) an die Server von Groq, wo ein KI-Modell Titel, Zeit und Ort erkennt. Gib keine hochsensiblen Daten ein.",
        "Akıllı Ekle, girdiğin metni (ve ekran görüntülerinden okunan metni) Groq sunucularına gönderir; bir yapay zekâ modeli başlık, zaman ve yeri çıkarır. Çok hassas bilgiler girme.",
        "Smart Add envía el texto que introduces (y el leído de capturas) a los servidores de Groq, donde un modelo de IA extrae título, hora y lugar. Evita introducir datos muy sensibles."
    )
    val howToKey = Txt("How to get an API key", "So bekommst du einen API-Key", "API anahtarı nasıl alınır", "Cómo obtener una clave API")
    val keyStep1 = Txt("1. Open the ", "1. Öffne die ", "1. Şunu aç: ", "1. Abre la ")
    val keyStepsRest = Txt(
        "\n2. Sign in with Google or e-mail.\n3. Tap “Create API Key” and copy it.\n4. Paste the key below.",
        "\n2. Mit Google oder E-Mail anmelden.\n3. „Create API Key“ antippen und kopieren.\n4. Key unten einfügen.",
        "\n2. Google veya e-posta ile giriş yap.\n3. “Create API Key”e dokun ve kopyala.\n4. Anahtarı aşağıya yapıştır.",
        "\n2. Inicia sesión con Google o correo.\n3. Pulsa «Create API Key» y cópiala.\n4. Pega la clave abajo."
    )
    val saveContinue = Txt("Save & continue", "Speichern & weiter", "Kaydet ve devam et", "Guardar y continuar")
    val category = Txt("Category", "Kategorie", "Kategori", "Categoría")
    val none = Txt("None", "Keine", "Yok", "Ninguna")
    val snooze = Txt("Snooze 10 min", "10 Min. später", "10 dk ertele", "Posponer 10 min")
    val dismiss = Txt("Dismiss", "Schließen", "Kapat", "Descartar")

    // Smart Add
    val smartAddHint = Txt(
        "Type, paste, speak or scan a screenshot — LifeOrganizer decides whether it's an event or a note.",
        "Tippen, einfügen, sprechen oder Screenshot scannen – LifeOrganizer erkennt, ob es ein Termin oder eine Notiz ist.",
        "Yaz, yapıştır, konuş ya da ekran görüntüsü tara — LifeOrganizer etkinlik mi not mu karar verir.",
        "Escribe, pega, habla o escanea una captura: LifeOrganizer decide si es un evento o una nota."
    )
    val smartAddPlaceholder = Txt(
        "e.g. Sunday 3pm meeting at Max",
        "z. B. Sonntag 15 Uhr Treffen bei Max",
        "örn. Pazar 15:00 Max'te buluşma",
        "p. ej. Domingo 15:00 reunión en casa de Max"
    )
    val takePhoto = Txt("Take photo", "Foto aufnehmen", "Fotoğraf çek", "Hacer foto")
    val fromGallery = Txt("Choose from gallery", "Aus Galerie wählen", "Galeriden seç", "Elegir de la galería")
    val listening = Txt("Listening…", "Ich höre zu…", "Dinliyorum…", "Escuchando…")
    val micPermission = Txt(
        "Microphone access is needed for voice input.",
        "Für die Spracheingabe wird Zugriff aufs Mikrofon benötigt.",
        "Sesli giriş için mikrofon izni gerekli.",
        "Se necesita acceso al micrófono para dictar."
    )
    val speak = Txt("Speak", "Sprechen", "Konuş", "Hablar")
    val scanImage = Txt("Scan image", "Bild scannen", "Görsel tara", "Escanear imagen")
    val process = Txt("Create", "Erstellen", "Oluştur", "Crear")
    val readingImage = Txt("Reading text from image…", "Text wird aus dem Bild gelesen…", "Görseldeki metin okunuyor…", "Leyendo texto de la imagen…")
    val noTextFound = Txt("No text found in the image.", "Kein Text im Bild gefunden.", "Görselde metin bulunamadı.", "No se encontró texto en la imagen.")
    val parseFailed = Txt("Couldn't understand that. Try rephrasing.", "Das konnte nicht erkannt werden. Bitte anders formulieren.", "Anlaşılamadı. Farklı ifade etmeyi dene.", "No se pudo entender. Prueba a reformularlo.")
    val voiceUnavailable = Txt("Speech recognition isn't available on this device.", "Spracherkennung ist auf diesem Gerät nicht verfügbar.", "Bu cihazda konuşma tanıma yok.", "El reconocimiento de voz no está disponible.")
    val savedEvent = Txt("Event saved", "Termin gespeichert", "Etkinlik kaydedildi", "Evento guardado")
    val savedNote = Txt("Note saved", "Notiz gespeichert", "Not kaydedildi", "Nota guardada")
    val savedItems = Txt("items saved", "Einträge gespeichert", "öğe kaydedildi", "elementos guardados")

    // App lock
    val locked = Txt("LifeOrganizer is locked", "LifeOrganizer ist gesperrt", "LifeOrganizer kilitli", "LifeOrganizer está bloqueado")
    val unlock = Txt("Unlock", "Entsperren", "Kilidi aç", "Desbloquear")
    val unlockSubtitle = Txt(
        "Use your fingerprint, face or device PIN",
        "Mit Fingerabdruck, Gesicht oder Geräte-PIN entsperren",
        "Parmak izi, yüz ya da cihaz PIN'ini kullan",
        "Usa tu huella, rostro o PIN del dispositivo"
    )
}
