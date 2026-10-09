package com.example.lifeorganizer.dev

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.calendar.debug.DebugLogger
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.Txt
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.theme.DeleteButton
import java.time.LocalDateTime

internal object DevStr {
    val title = Txt("Developer mode", "Entwicklermodus", "Geliştirici modu", "Modo desarrollador", "开发者模式")
    val unlocked = Txt("Developer mode on – see the menu", "Entwicklermodus an – siehe Menü", "Geliştirici modu açık – menüye bak", "Modo desarrollador activado: mira el menú", "开发者模式已开启 – 请查看菜单")
    val tapsLeft = Txt("%d more taps", "Noch %d Mal tippen", "%d dokunuş kaldı", "%d toques más", "再点 %d 次")
    val intro = Txt(
        "Helps to find crashes and other problems. Share the report to get an issue fixed.",
        "Hilft, Abstürze und andere Probleme zu finden. Teile den Bericht, damit ein Fehler behoben werden kann.",
        "Çökmeleri ve diğer sorunları bulmaya yardımcı olur. Bir hatanın düzeltilmesi için raporu paylaş.",
        "Ayuda a encontrar fallos y otros problemas. Comparte el informe para que se pueda corregir.",
        "帮助查找崩溃和其他问题。分享报告以便修复问题。"
    )
    val shareReport = Txt("Share report", "Bericht teilen", "Raporu paylaş", "Compartir informe", "分享报告")
    val crashTitle = Txt("Crash log", "Absturz-Log", "Çökme kaydı", "Registro de fallos", "崩溃日志")
    val crashText = Txt(
        "When LifeHub crashes, the cause is saved here and a silent notification appears.",
        "Wenn LifeHub abstürzt, wird die Ursache hier gespeichert und eine lautlose Benachrichtigung erscheint.",
        "LifeHub çöktüğünde neden burada kaydedilir ve sessiz bir bildirim görünür.",
        "Si LifeHub se cierra, la causa se guarda aquí y aparece una notificación silenciosa.",
        "LifeHub 崩溃时，原因会保存在这里，并显示一条静音通知。"
    )
    val noCrashes = Txt("No crashes recorded.", "Keine Abstürze aufgezeichnet.", "Kayıtlı çökme yok.", "No hay fallos registrados.", "没有崩溃记录。")
    val appLog = Txt("App log", "App-Log", "Uygulama kaydı", "Registro de la app", "应用日志")
    val appLogText = Txt(
        "Reminders, alarms, travel times and sync steps, newest at the bottom.",
        "Erinnerungen, Wecker, Fahrzeiten und Sync-Schritte, neueste unten.",
        "Hatırlatıcılar, alarmlar, yol süreleri ve senkronizasyon adımları; en yenisi altta.",
        "Recordatorios, alarmas, tiempos de viaje y sincronización; lo más nuevo abajo.",
        "提醒、闹钟、路程时间和同步步骤，最新的在底部。"
    )
    val emptyLog = Txt("Nothing logged yet.", "Noch nichts protokolliert.", "Henüz kayıt yok.", "Aún no hay registros.", "暂无日志。")
    val testCrash = Txt("Test crash", "Test-Absturz", "Test çökmesi", "Fallo de prueba", "测试崩溃")
    val testCrashConfirm = Txt(
        "LifeHub will close immediately to test the crash log.",
        "LifeHub wird sofort beendet, um das Absturz-Log zu testen.",
        "Çökme kaydını test etmek için LifeHub hemen kapanacak.",
        "LifeHub se cerrará ahora para probar el registro de fallos.",
        "LifeHub 将立即关闭，以测试崩溃日志。"
    )
    val clear = Txt("Delete", "Löschen", "Sil", "Borrar", "删除")
    val turnOff = Txt("Turn off developer mode", "Entwicklermodus ausschalten", "Geliştirici modunu kapat", "Desactivar modo desarrollador", "关闭开发者模式")
}

/** Everything needed to fix a problem in one text: device, crashes, app log and system log. */
private fun fullReport(context: android.content.Context): String = buildString {
    appendLine("LifeHub report, ${LocalDateTime.now().withNano(0)}")
    appendLine(CrashLog.deviceInfo())
    CrashLog.entries(context).take(5).forEach { appendLine("\n=== Crash ${it.time} ===").appendLine(it.report) }
    appendLine("\n=== App log ===").append(DebugLogger.saved().lines().takeLast(200).joinToString("\n"))
    appendLine("\n\n=== System log ===").append(CrashLog.systemLog(300))
}

private fun share(context: android.content.Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DevModeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val crashOn by DevMode.crashLog.collectAsStateWithLifecycle()
    var crashes by remember { mutableStateOf(CrashLog.entries(context)) }
    var expanded by remember { mutableStateOf<String?>(crashes.firstOrNull()?.file?.name) }
    var appLog by remember { mutableStateOf(DebugLogger.saved().lines().filter { it.isNotBlank() }.takeLast(100)) }
    var confirmTest by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(DevStr.title.text()) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, Str.back.text()) } }
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(DevStr.intro.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { share(context, fullReport(context)) }) {
                Icon(Icons.Outlined.Share, null)
                Text("  " + DevStr.shareReport.text())
            }

            // Crash log
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(DevStr.crashTitle.text(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = crashOn, onCheckedChange = { DevMode.setCrashLog(context, it) })
                    }
                    Text(DevStr.crashText.text(), style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = crashOn, onClick = { confirmTest = true }) { Text(DevStr.testCrash.text()) }
                        DeleteButton(DevStr.clear.text(), enabled = crashes.isNotEmpty(), onClick = {
                            CrashLog.clear(context)
                            crashes = emptyList()
                        })
                    }
                }
            }
            if (crashes.isEmpty()) Text(DevStr.noCrashes.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            crashes.forEach { entry ->
                val open = expanded == entry.file.name
                Card(Modifier.fillMaxWidth().clickable { expanded = if (open) null else entry.file.name }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, maxLines = if (open) Int.MAX_VALUE else 2)
                            }
                            IconButton(onClick = { share(context, entry.report) }) { Icon(Icons.Outlined.Share, DevStr.shareReport.text()) }
                        }
                        if (open) LogText(entry.report)
                    }
                }
            }

            // App log
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(DevStr.appLog.text(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        DeleteButton(DevStr.clear.text(), enabled = appLog.isNotEmpty(), onClick = {
                            DebugLogger.clearSaved()
                            appLog = emptyList()
                        })
                    }
                    Text(DevStr.appLogText.text(), style = MaterialTheme.typography.bodySmall)
                    if (appLog.isEmpty()) Text(DevStr.emptyLog.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else LogText(appLog.joinToString("\n"))
                }
            }

            TextButton(onClick = { DevMode.setEnabled(context, false); onBack() }) { Text(DevStr.turnOff.text()) }
        }
    }

    if (confirmTest) AlertDialog(
        onDismissRequest = { confirmTest = false },
        text = { Text(DevStr.testCrashConfirm.text()) },
        confirmButton = {
            TextButton(onClick = { throw RuntimeException("Test crash from developer mode") }) {
                Text(DevStr.testCrash.text(), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmTest = false }) { Text(Str.cancel.text()) } }
    )
}

@Composable
private fun LogText(text: String) {
    SelectionContainer {
        Text(
            text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp,
            softWrap = false, modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}
