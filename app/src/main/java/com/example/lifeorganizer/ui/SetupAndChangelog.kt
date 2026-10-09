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
