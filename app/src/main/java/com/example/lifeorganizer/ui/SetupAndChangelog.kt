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
        "0.10", listOf(
            Txt("LifeOrganizer is now called LifeHub (new app – move your data once via backup)", "LifeOrganizer heißt jetzt LifeHub (neue App – Daten einmalig per Backup umziehen)", "LifeOrganizer artık LifeHub (yeni uygulama – verileri bir kez yedekle taşı)", "LifeOrganizer ahora es LifeHub (app nueva: mueve tus datos con una copia)"),
            Txt("App is much smaller (about 18 MB instead of 62 MB)", "App ist deutlich kleiner (ca. 18 MB statt 62 MB)", "Uygulama çok daha küçük (~18 MB)", "La app es mucho más pequeña (~18 MB)"),
            Txt("All texts translated – no more English mix", "Alle Texte übersetzt – kein Englisch-Mix mehr", "Tüm metinler çevrildi", "Todos los textos traducidos"),
            Txt("Backup now includes settings and documents attached to notes", "Backup enthält jetzt Einstellungen und an Notizen angehängte Dokumente", "Yedek artık ayarları ve notlara ekli belgeleri içerir", "La copia incluye ajustes y documentos de notas"),
            Txt("Crash screen with 'share report'", "Absturz-Bildschirm mit „Bericht teilen“", "'Raporu paylaş' ile çökme ekranı", "Pantalla de error con 'compartir informe'"),
            Txt("About & privacy with open-source licences", "Über die App & Datenschutz mit Open-Source-Lizenzen", "Hakkında ve gizlilik, açık kaynak lisansları", "Acerca de y privacidad con licencias"),
            Txt("Better with large font sizes", "Besser bei großer Schrift", "Büyük yazı boyutunda daha iyi", "Mejor con letra grande"),
            Txt("Landscape and tablets: readable width, compact month, more room in the note editor", "Querformat & Tablets: lesbare Breite, kompakter Monat, mehr Platz im Notiz-Editor", "Yatay ve tablet: okunur genişlik, kompakt ay, not düzenleyicide daha fazla alan", "Horizontal y tablets: ancho legible, mes compacto, más espacio en notas"),
            Txt("Fixed: backup failed after reinstalling", "Behoben: Backup schlug nach Neuinstallation fehl", "Düzeltildi: yeniden kurulumdan sonra yedekleme hatası", "Corregido: copia fallaba tras reinstalar")
        )
    ),
    Release(
        "0.9", listOf(
            Txt("First start: setup for language and design", "Erster Start: Einrichtung von Sprache und Design", "İlk açılış: dil ve tasarım kurulumu", "Primer inicio: configuración de idioma y diseño"),
            Txt("Patch notes after updates and a full changelog in the menu", "Patchnotes nach Updates und komplettes Changelog im Menü", "Güncellemeden sonra yenilikler ve menüde tüm değişiklikler", "Novedades tras actualizar y registro completo en el menú"),
            Txt("Update check: download the newest version from GitHub in the app", "Update-Prüfung: neueste Version direkt in der App von GitHub laden", "Güncelleme: en yeni sürümü uygulamada GitHub'dan indir", "Actualizar: descarga la última versión de GitHub en la app"),
            Txt("Dokki is now simply called Documents", "Dokki heißt jetzt einfach Dokumente", "Dokki artık Belgeler", "Dokki ahora se llama Documentos"),
            Txt("Calendar filter: hide repeating events or categories (e.g. work, uni) live", "Kalender-Filter: Wiederholungen oder Kategorien (z. B. Arbeit, Uni) live ausblenden", "Takvim filtresi: tekrarları veya kategorileri canlı gizle", "Filtro: oculta eventos periódicos o categorías al instante"),
            Txt("Events save automatically when the dialog is closed", "Termine speichern beim Schließen automatisch", "Etkinlikler kapatınca otomatik kaydedilir", "Los eventos se guardan al cerrar"),
            Txt("Repeating events: 'this and following' is the default when deleting", "Wiederholungen: „Diesen und alle folgenden“ ist beim Löschen Standard", "Tekrarlar: silerken 'bu ve sonrakiler' varsayılan", "Periódicos: 'este y los siguientes' por defecto al borrar"),
            Txt("Notes: tick checklist items on the card, checkbox button ticks while editing, formatting help", "Notizen: Listenpunkte auf der Karte abhaken, Checkbox-Knopf hakt beim Bearbeiten ab, Formatierungshilfe", "Notlar: kartta işaretle, düzenlerken onay düğmesi, biçim yardımı", "Notas: marca en la tarjeta, botón de casilla al editar, ayuda de formato"),
            Txt("Copy an event into a phone calendar", "Termin in den Gerätekalender kopieren", "Etkinliği telefon takvimine kopyala", "Copiar un evento al calendario del teléfono"),
            Txt("Attach documents to notes", "Dokumente an Notizen anhängen", "Notlara belge ekle", "Adjuntar documentos a notas")
        )
    ),
    Release(
        "0.8", listOf(
            Txt("Repeating events: edit or delete this, following or all", "Wiederholungen: nur diesen, folgende oder alle bearbeiten/löschen", "Tekrarlar: bunu, sonrakileri veya hepsini düzenle", "Periódicos: editar este, siguientes o todos"),
            Txt("Phone calendars shown read-only", "Gerätekalender werden angezeigt (nur lesen)", "Telefon takvimleri gösterilir (salt okunur)", "Calendarios del teléfono visibles (solo lectura)"),
            Txt("Weekly automatic backup, optional copy to your own folder", "Wöchentliches Auto-Backup, optional Kopie in eigenen Ordner", "Haftalık otomatik yedek, isteğe bağlı kendi klasörüne", "Copia semanal automática, opcional en tu carpeta"),
            Txt("Smart Add works offline without API key", "Smart Add funktioniert offline ohne API-Key", "Akıllı Ekle API anahtarı olmadan çevrimdışı çalışır", "Smart Add funciona sin conexión ni clave"),
            Txt("Label templates, template placeholders, note widget", "Label-Vorlagen, Platzhalter, Notiz-Widget", "Etiket şablonları, yer tutucular, not widget'ı", "Plantillas por etiqueta, marcadores, widget de notas"),
            Txt("Fixed: repeating events multiplied after .ics import", "Behoben: Serien vervielfachten sich nach .ics-Import", "Düzeltildi: .ics içe aktarmada tekrarlar çoğalıyordu", "Corregido: eventos periódicos duplicados al importar .ics")
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
fun SetupScreen(settings: SettingsManager, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
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

            Section(Str.language.text()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("de" to "Deutsch", "en" to "English", "tr" to "Türkçe", "es" to "Español").forEach { (code, name) ->
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
