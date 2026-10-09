package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.lifeorganizer.core.i18n.Txt
import com.example.lifeorganizer.core.i18n.text

/**
 * Small ⓘ next to a setting or field. Tapping shows a short explanation in a bubble;
 * tapping anywhere closes it again.
 */
@Composable
fun InfoTip(tip: Txt, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = !open }, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.Info, contentDescription = tip.text(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
            )
        }
        if (open) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, 90),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = 8.dp)
                ) {
                    Text(tip.text(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

/** Explanations shown by [InfoTip]. */
object Tips {
    val smartAlarm = Txt(
        "Calculates the travel time to the event's address with Google Maps and reminds you when it's time to leave.",
        "Berechnet mit Google Maps die Fahrzeit zur Adresse des Termins und erinnert dich, wenn du losmusst.",
        "Google Haritalar ile etkinlik adresine yol süresini hesaplar ve çıkma zamanı geldiğinde hatırlatır.",
        "Calcula con Google Maps el tiempo de viaje a la dirección del evento y te avisa cuando debes salir.",
        "使用 Google 地图计算到日程地址的路程时间，并在该出发时提醒你。"
    )
    val systemAlarm = Txt(
        "Reminders in the next 24 hours are also set as an alarm in the phone's clock app – they ring even when the phone is on silent.",
        "Erinnerungen in den nächsten 24 Stunden werden zusätzlich als Wecker in der Uhr-App gestellt – sie klingeln auch im Lautlos-Modus.",
        "Önümüzdeki 24 saatteki hatırlatıcılar ayrıca saat uygulamasında alarm olarak kurulur – telefon sessizdeyken de çalar.",
        "Los recordatorios de las próximas 24 horas también se ponen como alarma en la app de reloj: suenan aunque el móvil esté en silencio.",
        "未来 24 小时内的提醒也会在手机时钟应用中设为闹钟——即使手机静音也会响。"
    )
    val groqKey = Txt(
        "Optional. With a free Groq key, Smart Add understands free text, screenshots and lists much better. Without a key it works offline with simple rules.",
        "Optional. Mit einem kostenlosen Groq-Key versteht Smart Add freien Text, Screenshots und Listen viel besser. Ohne Key arbeitet es offline mit einfachen Regeln.",
        "İsteğe bağlı. Ücretsiz bir Groq anahtarıyla Akıllı Ekle serbest metni, ekran görüntülerini ve listeleri çok daha iyi anlar. Anahtar olmadan basit kurallarla çevrimdışı çalışır.",
        "Opcional. Con una clave gratuita de Groq, Añadir inteligente entiende mucho mejor el texto libre, capturas y listas. Sin clave funciona sin conexión con reglas simples.",
        "可选。使用免费的 Groq 密钥后，智能添加能更好地理解自由文本、截图和列表。没有密钥时会用简单规则离线工作。"
    )
    val homeAddress = Txt(
        "Starting point for travel times when your current location is not used.",
        "Startpunkt für Fahrzeiten, wenn nicht der aktuelle Standort verwendet wird.",
        "Mevcut konum kullanılmadığında yol süreleri için başlangıç noktası.",
        "Punto de partida para los tiempos de viaje cuando no se usa tu ubicación actual.",
        "不使用当前位置时，计算路程时间的起点。"
    )
    val appLock = Txt(
        "Asks for fingerprint, face or device PIN whenever LifeHub is opened.",
        "Fragt beim Öffnen von LifeHub nach Fingerabdruck, Gesicht oder Geräte-PIN.",
        "LifeHub her açıldığında parmak izi, yüz veya cihaz PIN'i ister.",
        "Pide huella, cara o PIN del dispositivo cada vez que se abre LifeHub.",
        "每次打开 LifeHub 时要求验证指纹、面容或设备 PIN 码。"
    )
    val travelBuffer = Txt(
        "Arrival buffer: how many minutes early you want to be there. Lead time: how long before leaving the smart alarm rings.",
        "Ankunftspuffer: wie viele Minuten du früher da sein willst. Vorlaufzeit: wie lange vor dem Losfahren der Smart-Alarm klingelt.",
        "Varış tamponu: kaç dakika erken orada olmak istediğin. Hazırlık süresi: akıllı alarmın çıkıştan ne kadar önce çalacağı.",
        "Margen de llegada: cuántos minutos antes quieres estar allí. Antelación: cuánto antes de salir suena la alarma inteligente.",
        "到达缓冲：你想提前几分钟到达。提前量：智能闹钟在出发前多久响起。"
    )
    val labelTemplate = Txt(
        "Text that every new note with this label starts with, e.g. a checklist you always need.",
        "Text, mit dem jede neue Notiz mit diesem Label beginnt, z. B. eine Checkliste, die du immer brauchst.",
        "Bu etiketle oluşturulan her yeni notun başladığı metin, örn. her zaman ihtiyaç duyduğun bir kontrol listesi.",
        "Texto con el que empieza cada nota nueva con esta etiqueta, p. ej. una lista que siempre necesitas.",
        "使用此标签的新笔记都会以这段文字开头，例如你常用的清单。"
    )
    val budget = Txt(
        "Monthly limit for this category. In the overview the bar turns yellow at 80 % and red at 100 %.",
        "Monatliches Limit für diese Kategorie. In der Übersicht wird der Balken ab 80 % gelb und ab 100 % rot.",
        "Bu kategori için aylık sınır. Genel bakışta çubuk %80'de sarı, %100'de kırmızı olur.",
        "Límite mensual para esta categoría. En el resumen la barra se pone amarilla al 80 % y roja al 100 %.",
        "此分类的每月限额。概览中进度条达到 80% 时变黄，达到 100% 时变红。"
    )
    val fixedCosts = Txt(
        "Rent, subscriptions, contracts: booked automatically on the chosen day. When you import your bank CSV, the bank entry replaces the automatic one – nothing is counted twice.",
        "Miete, Abos, Verträge: werden am gewählten Tag automatisch gebucht. Beim Import der Bank-CSV ersetzt die Bankbuchung die automatische – nichts wird doppelt gezählt.",
        "Kira, abonelikler, sözleşmeler: seçilen günde otomatik kaydedilir. Banka CSV'sini içe aktardığında banka kaydı otomatik olanın yerini alır – hiçbir şey iki kez sayılmaz.",
        "Alquiler, suscripciones, contratos: se registran solos el día elegido. Al importar el CSV del banco, el movimiento del banco sustituye al automático: nada se cuenta dos veces.",
        "房租、订阅、合同：在所选日期自动记账。导入银行 CSV 时，银行记录会替换自动记录——不会重复计算。"
    )
    val fixedInCalendar = Txt(
        "Adds a repeating all-day entry with the amount to the calendar category \"Fixed costs\".",
        "Legt im Kalender einen wiederkehrenden ganztägigen Eintrag mit dem Betrag in der Kategorie „Fixkosten“ an.",
        "Takvimde \"Sabit giderler\" kategorisinde tutarı içeren tekrarlayan tüm gün kaydı oluşturur.",
        "Crea en el calendario una entrada recurrente de todo el día con el importe en la categoría «Gastos fijos».",
        "在日历的“固定支出”分类中添加一个带金额的全天重复日程。"
    )
    val learnedPayees = Txt(
        "When you give a payee a category, LifeHub remembers it and sorts future transactions from them automatically. Remove an entry to forget it.",
        "Wenn du einem Händler eine Kategorie gibst, merkt sich LifeHub das und sortiert künftige Buchungen automatisch ein. Entferne einen Eintrag, um ihn zu vergessen.",
        "Bir alıcıya kategori verdiğinde LifeHub bunu hatırlar ve gelecekteki işlemleri otomatik sınıflandırır. Unutmak için bir kaydı kaldır.",
        "Cuando asignas una categoría a un comercio, LifeHub lo recuerda y clasifica solos los próximos movimientos. Quita una entrada para olvidarla.",
        "当你为某个收款方设置分类后，LifeHub 会记住，并自动归类以后的交易。删除条目即可让它忘记。"
    )
    val webAddress = Txt(
        "Name: the address becomes <name>.local – works in most home networks, otherwise use the IP shown above. Port: only change it if 8080 is used by something else.",
        "Name: die Adresse wird zu <name>.local – klappt in den meisten Heimnetzen, sonst die oben gezeigte IP nutzen. Port: nur ändern, wenn 8080 schon belegt ist.",
        "Ad: adres <ad>.local olur – çoğu ev ağında çalışır, aksi halde yukarıdaki IP'yi kullan. Port: yalnızca 8080 başka bir şey tarafından kullanılıyorsa değiştir.",
        "Nombre: la dirección pasa a ser <nombre>.local; funciona en la mayoría de redes domésticas, si no usa la IP de arriba. Puerto: cámbialo solo si el 8080 ya está ocupado.",
        "名称：地址会变成 <名称>.local——适用于大多数家庭网络，否则请使用上方显示的 IP。端口：只有 8080 被占用时才需要修改。"
    )
    val waypoints = Txt(
        "Saved places like home, work or friends. Events can use them as destination, and Smart Add recognises their names (\"at Max\").",
        "Gespeicherte Orte wie Zuhause, Arbeit oder Freunde. Termine können sie als Ziel nutzen, und Smart Add erkennt ihre Namen („bei Max“).",
        "Ev, iş veya arkadaşlar gibi kayıtlı yerler. Etkinlikler bunları hedef olarak kullanabilir ve Akıllı Ekle adlarını tanır (\"Max'te\").",
        "Lugares guardados como casa, trabajo o amigos. Los eventos pueden usarlos como destino y Añadir inteligente reconoce sus nombres («en casa de Max»).",
        "保存的地点，如家、公司或朋友家。日程可以把它们作为目的地，智能添加也能识别它们的名称（“在 Max 家”）。"
    )
}
