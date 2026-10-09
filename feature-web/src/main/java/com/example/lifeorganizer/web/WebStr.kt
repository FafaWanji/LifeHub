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
}
