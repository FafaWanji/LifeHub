package com.example.lifeorganizer.web

import com.example.lifeorganizer.core.i18n.Txt

object WebStr {
    val title = Txt("PC access", "PC-Zugriff", "PC erişimi", "Acceso desde PC", "电脑访问")
    val switchLabel = Txt("Use LifeHub in the browser", "LifeHub im Browser nutzen", "LifeHub'ı tarayıcıda kullan", "Usar LifeHub en el navegador", "在浏览器中使用 LifeHub")
    val intro = Txt(
        "Open the address below in a browser on your PC. PC and phone must be in the same Wi-Fi. Your data stays on the phone.",
        "Öffne die Adresse unten im Browser am PC. PC und Handy müssen im selben WLAN sein. Deine Daten bleiben auf dem Handy.",
        "Aşağıdaki adresi bilgisayarındaki tarayıcıda aç. Bilgisayar ve telefon aynı Wi-Fi ağında olmalı. Verilerin telefonda kalır.",
        "Abre la dirección de abajo en el navegador del PC. El PC y el móvil deben estar en la misma Wi-Fi. Tus datos se quedan en el móvil.", "在电脑浏览器中打开下方地址。电脑和手机必须连接同一 Wi-Fi。你的数据保留在手机上。"
    )
    val address = Txt("Address", "Adresse", "Adres", "Dirección", "地址")
    val code = Txt("Pairing code", "Kopplungscode", "Eşleştirme kodu", "Código de emparejamiento", "配对码")
    val codeHint = Txt(
        "Enter this code once in the browser. It changes after each use.",
        "Gib diesen Code einmal im Browser ein. Er ändert sich nach jeder Nutzung.",
        "Bu kodu tarayıcıya bir kez gir. Her kullanımdan sonra değişir.",
        "Introduce este código una vez en el navegador. Cambia tras cada uso.", "在浏览器中输入一次此代码。每次使用后都会更换。"
    )
    val newCode = Txt("New code", "Neuer Code", "Yeni kod", "Nuevo código", "新代码")
    val noWifi = Txt("No Wi-Fi connection found.", "Keine WLAN-Verbindung gefunden.", "Wi-Fi bağlantısı bulunamadı.", "No se encontró conexión Wi-Fi.", "未找到 Wi-Fi 连接。")
    val startFailed = Txt("Could not start (port 8080 busy?).", "Start fehlgeschlagen (Port 8080 belegt?).", "Başlatılamadı (8080 portu meşgul mü?).", "No se pudo iniciar (¿puerto 8080 ocupado?).", "无法启动（端口 8080 被占用？）。")
    val security = Txt(
        "The connection is not encrypted. Only use it in your own Wi-Fi, not in public networks. It switches off by itself after 30 minutes without use.",
        "Die Verbindung ist nicht verschlüsselt. Nutze sie nur im eigenen WLAN, nicht in öffentlichen Netzen. Sie schaltet sich nach 30 Minuten ohne Nutzung selbst ab.",
        "Bağlantı şifreli değildir. Yalnızca kendi Wi-Fi ağında kullan, halka açık ağlarda kullanma. 30 dakika kullanılmazsa kendiliğinden kapanır.",
        "La conexión no está cifrada. Úsala solo en tu propia Wi-Fi, no en redes públicas. Se apaga sola tras 30 minutos sin uso.", "连接未加密。请只在自己的 Wi-Fi 中使用，不要在公共网络中使用。30 分钟未使用会自动关闭。"
    )
    val browsers = Txt("Paired browsers", "Gekoppelte Browser", "Eşleştirilmiş tarayıcılar", "Navegadores emparejados", "已配对的浏览器")
    val noBrowsers = Txt("None yet", "Noch keine", "Henüz yok", "Ninguno todavía", "暂无")
    val lastUsed = Txt("last used %s", "zuletzt %s", "son kullanım %s", "último uso %s", "最近使用 %s")
    val remove = Txt("Remove", "Entfernen", "Kaldır", "Quitar", "移除")
    val notifTitle = Txt("PC access is on", "PC-Zugriff ist an", "PC erişimi açık", "Acceso desde PC activo", "电脑访问已开启")
    val notifText = Txt("%1\$s · Code %2\$s", "%1\$s · Code %2\$s", "%1\$s · Kod %2\$s", "%1\$s · Código %2\$s", "%1\$s · 代码 %2\$s")
    val stop = Txt("Stop", "Beenden", "Durdur", "Detener", "停止")
    val channel = Txt("PC access", "PC-Zugriff", "PC erişimi", "Acceso desde PC", "电脑访问")
    val fallback = Txt(
        "If the name does not work in your network: %s",
        "Falls der Name in deinem Netz nicht klappt: %s",
        "Ad ağında çalışmazsa: %s",
        "Si el nombre no funciona en tu red: %s", "如果名称在你的网络中无效：%s"
    )
    val settings = Txt("Address settings", "Adresse anpassen", "Adres ayarları", "Ajustes de dirección", "地址设置")
    val name = Txt("Name", "Name", "Ad", "Nombre", "名称")
    val port = Txt("Port", "Port", "Port", "Puerto", "端口")
    val portHint = Txt("1024–65535", "1024–65535", "1024–65535", "1024–65535", "1024–65535")
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar", "保存")

    // Automatic start
    val autoTitle = Txt("Switch on automatically", "Automatisch einschalten", "Otomatik aç", "Activar automáticamente", "自动开启")
    val modeOff = Txt("Off", "Aus", "Kapalı", "Desactivado", "关闭")
    val modeOffHint = Txt(
        "Only with the switch above. No battery use.",
        "Nur mit dem Schalter oben. Kein Akkuverbrauch.",
        "Yalnızca yukarıdaki anahtarla. Pil kullanmaz.",
        "Solo con el interruptor de arriba. Sin consumo de batería.",
        "仅通过上方开关。不耗电。"
    )
    val modeAppOpen = Txt("When LifeHub opens", "Beim Öffnen von LifeHub", "LifeHub açıldığında", "Al abrir LifeHub", "打开 LifeHub 时")
    val modeAppOpenHint = Txt(
        "In a Wi-Fi; off after 30 min without use. Barely any battery.",
        "Im WLAN; aus nach 30 Min. ohne Nutzung. Kaum Akku.",
        "Wi-Fi'da; 30 dk kullanılmazsa kapanır. Neredeyse hiç pil harcamaz.",
        "En una Wi-Fi; se apaga tras 30 min sin uso. Casi sin batería.",
        "仅在 Wi-Fi 下；30 分钟未使用后关闭。几乎不耗电。"
    )
    val modeWifi = Txt("In Wi-Fi", "Im WLAN", "Wi-Fi'da", "En Wi-Fi", "在 Wi-Fi 下")
    val modeWifiHint = Txt(
        "Runs while in a Wi-Fi. Estimated 1–3 % battery a day, more while a PC tab is open.",
        "Läuft, solange das Handy im WLAN ist. Geschätzt 1–3 % Akku pro Tag, mehr bei offenem PC-Tab.",
        "Telefon Wi-Fi'dayken çalışır. Tahmini günde %1–3 pil, açık bir PC sekmesinde daha fazla.",
        "Funciona mientras estés en una Wi-Fi. Aprox. 1–3 % de batería al día, más con una pestaña abierta en el PC.",
        "手机连接 Wi-Fi 时运行。估计每天耗电 1–3%，电脑标签页打开时更多。"
    )
    val modeAlways = Txt("Always on", "Immer an", "Her zaman açık", "Siempre activo", "始终开启")
    val modeAlwaysHint = Txt(
        "Also in public networks. Estimated 2–4 % battery a day, more while a PC tab is open. Permanent notification.",
        "Auch in öffentlichen Netzen. Geschätzt 2–4 % Akku pro Tag, mehr bei offenem PC-Tab. Dauerhafte Benachrichtigung.",
        "Halka açık ağlarda da. Tahmini günde %2–4 pil, açık bir PC sekmesinde daha fazla. Kalıcı bildirim.",
        "También en redes públicas. Aprox. 2–4 % de batería al día, más con una pestaña abierta. Notificación permanente.",
        "公共网络中也会运行。估计每天耗电 2–4%，电脑标签页打开时更多。常驻通知。"
    )
    val allWifi = Txt("Every Wi-Fi", "Jedes WLAN", "Her Wi-Fi", "Cualquier Wi-Fi", "任何 Wi-Fi")
    val knownWifi = Txt("Only remembered ones", "Nur gemerkte", "Yalnızca kayıtlı olanlar", "Solo las guardadas", "仅已记住的")
    val rememberWifi = Txt("Remember this Wi-Fi", "Dieses WLAN merken", "Bu Wi-Fi'ı hatırla", "Recordar esta Wi-Fi", "记住此 Wi-Fi")
    val noKnownWifi = Txt("No Wi-Fi remembered yet.", "Noch kein WLAN gemerkt.", "Henüz kayıtlı Wi-Fi yok.", "Aún no hay ninguna Wi-Fi guardada.", "尚未记住任何 Wi-Fi。")
    val notInWifi = Txt("The phone is not in a Wi-Fi.", "Das Handy ist in keinem WLAN.", "Telefon bir Wi-Fi'a bağlı değil.", "El móvil no está en una Wi-Fi.", "手机未连接 Wi-Fi。")
    val lowBattery = Txt("Switch off when the battery is below", "Ausschalten bei Akku unter", "Pil şu seviyenin altındaysa kapat", "Desactivar con la batería por debajo de", "电量低于此值时关闭")
    val lowBatteryHint = Txt("Not while charging.", "Nicht beim Laden.", "Şarj olurken değil.", "No mientras se carga.", "充电时除外。")
    val connectedNow = Txt("connected", "verbunden", "bağlı", "conectado", "已连接")
    val notifWaiting = Txt("PC access is waiting", "PC-Zugriff wartet", "PC erişimi bekliyor", "Acceso desde PC en espera", "电脑访问等待中")
    val waitWifi = Txt("Starts again in a Wi-Fi", "Startet wieder im WLAN", "Wi-Fi'da yeniden başlar", "Se reanuda en una Wi-Fi", "连接 Wi-Fi 后重新开启")
    val waitKnownWifi = Txt("Starts again in a remembered Wi-Fi", "Startet wieder in einem gemerkten WLAN", "Kayıtlı bir Wi-Fi'da yeniden başlar", "Se reanuda en una Wi-Fi guardada", "连接已记住的 Wi-Fi 后重新开启")
    val waitBattery = Txt("Paused: battery low", "Pausiert: Akku niedrig", "Duraklatıldı: pil düşük", "En pausa: batería baja", "已暂停：电量低")
}
