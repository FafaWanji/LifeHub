package com.example.lifeorganizer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.BuildConfig
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.Txt
import com.example.lifeorganizer.core.i18n.text

/** Libraries shipped in the app and their licences. */
private val LIBRARIES = listOf(
    "AndroidX (Compose, Material 3, Room, WorkManager, DataStore, Glance, Navigation, Biometric, Lifecycle)" to "Apache 2.0",
    "Kotlin & kotlinx.coroutines" to "Apache 2.0",
    "Google Play services (Location, Maps), Places SDK, Maps Compose" to "Google APIs Terms of Service / Apache 2.0",
    "ML Kit Text Recognition" to "ML Kit Terms of Service",
    "Kizitonwose Calendar" to "MIT",
    "Android Image Cropper (CanHub / Vanniktech)" to "Apache 2.0",
    "Retrofit, OkHttp" to "Apache 2.0",
    "Gson" to "Apache 2.0",
    "Coil" to "Apache 2.0"
)

private val PRIVACY = listOf(
    Txt(
        "Your events, notes, places and documents are stored only on this phone. There is no account, no tracking and no advertising.",
        "Termine, Notizen, Orte und Dokumente werden nur auf diesem Handy gespeichert. Es gibt kein Konto, kein Tracking und keine Werbung.",
        "Etkinlikler, notlar, yerler ve belgeler yalnızca bu telefonda saklanır. Hesap, izleme veya reklam yok.",
        "Eventos, notas, lugares y documentos se guardan solo en este teléfono. Sin cuenta, sin rastreo y sin publicidad.", "你的日程、笔记、地点和文档只保存在这部手机上。没有账户，没有追踪，也没有广告。"
    ),
    Txt(
        "Smart Add with an API key sends the entered text (and text read from images) to Groq to recognise date, time and place. Without a key everything is processed offline.",
        "Smart Add mit API-Key sendet den eingegebenen Text (und aus Bildern gelesenen Text) an Groq, um Datum, Zeit und Ort zu erkennen. Ohne Key läuft alles offline.",
        "API anahtarlı Akıllı Ekle, girilen metni (ve görsellerden okunan metni) Groq'a gönderir. Anahtar olmadan her şey çevrimdışı çalışır.",
        "Smart Add con clave API envía el texto (y el leído de imágenes) a Groq. Sin clave todo funciona sin conexión.", "使用 API 密钥时，智能添加会将输入的文本（以及从图片中识别的文本）发送到 Groq，以识别日期、时间和地点。没有密钥时，一切都在本地离线处理。"
    ),
    Txt(
        "For travel times and places, addresses are sent to Google Maps. Location is only used when you choose \"current location\".",
        "Für Reisezeiten und Orte werden Adressen an Google Maps gesendet. Der Standort wird nur genutzt, wenn du „aktueller Standort“ wählst.",
        "Yol süreleri ve yerler için adresler Google Maps'e gönderilir. Konum yalnızca \"mevcut konum\" seçildiğinde kullanılır.",
        "Para tiempos de viaje y lugares, las direcciones se envían a Google Maps. La ubicación solo se usa si eliges \"ubicación actual\".", "为计算路程时间和查找地点，地址会发送到 Google 地图。只有当你选择“当前位置”时才会使用定位。"
    ),
    Txt(
        "Phone calendars, microphone and camera are only accessed after you allow it. Text recognition in images runs on the phone. Updates are checked on GitHub.",
        "Gerätekalender, Mikrofon und Kamera werden nur nach deiner Erlaubnis genutzt. Texterkennung in Bildern läuft auf dem Handy. Updates werden bei GitHub geprüft.",
        "Telefon takvimi, mikrofon ve kameraya yalnızca izin verdikten sonra erişilir. Görsellerde metin tanıma telefonda çalışır. Güncellemeler GitHub'da denetlenir.",
        "Calendarios, micrófono y cámara solo se usan con tu permiso. El reconocimiento de texto funciona en el teléfono. Las actualizaciones se consultan en GitHub.", "只有在你允许后，才会访问手机日历、麦克风和相机。图片文字识别在手机上运行。更新通过 GitHub 检查。"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Str.about.text()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, Str.back.text()) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("LifeHub ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            Text("github.com/FafaWanji/LifeHub · GPL-3.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(Str.privacy.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            PRIVACY.forEach { Text(it.text(), style = MaterialTheme.typography.bodyMedium) }

            Text(Str.openSource.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            LIBRARIES.forEach { (name, license) ->
                Column {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text(license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
