package com.example.lifeorganizer.web

import com.example.lifeorganizer.core.i18n.Txt

object WebStr {
    val title = Txt("PC access", "PC-Zugriff", "PC erişimi", "Acceso desde PC")
    val switchLabel = Txt("Use LifeHub in the browser", "LifeHub im Browser nutzen", "LifeHub'ı tarayıcıda kullan", "Usar LifeHub en el navegador")
    val intro = Txt(
        "Open the address below in a browser on your PC. PC and phone must be in the same Wi-Fi. Your data stays on the phone.",
        "Öffne die Adresse unten im Browser am PC. PC und Handy müssen im selben WLAN sein. Deine Daten bleiben auf dem Handy.",
        "Aşağıdaki adresi bilgisayarındaki tarayıcıda aç. Bilgisayar ve telefon aynı Wi-Fi ağında olmalı. Verilerin telefonda kalır.",
        "Abre la dirección de abajo en el navegador del PC. El PC y el móvil deben estar en la misma Wi-Fi. Tus datos se quedan en el móvil."
    )
    val address = Txt("Address", "Adresse", "Adres", "Dirección")
    val code = Txt("Pairing code", "Kopplungscode", "Eşleştirme kodu", "Código de emparejamiento")
    val codeHint = Txt(
        "Enter this code once in the browser. It changes after each use.",
        "Gib diesen Code einmal im Browser ein. Er ändert sich nach jeder Nutzung.",
        "Bu kodu tarayıcıya bir kez gir. Her kullanımdan sonra değişir.",
        "Introduce este código una vez en el navegador. Cambia tras cada uso."
    )
    val newCode = Txt("New code", "Neuer Code", "Yeni kod", "Nuevo código")
    val noWifi = Txt("No Wi-Fi connection found.", "Keine WLAN-Verbindung gefunden.", "Wi-Fi bağlantısı bulunamadı.", "No se encontró conexión Wi-Fi.")
    val startFailed = Txt("Could not start (port 8080 busy?).", "Start fehlgeschlagen (Port 8080 belegt?).", "Başlatılamadı (8080 portu meşgul mü?).", "No se pudo iniciar (¿puerto 8080 ocupado?).")
    val security = Txt(
        "The connection is not encrypted. Only use it in your own Wi-Fi, not in public networks. It switches off by itself after 30 minutes without use.",
        "Die Verbindung ist nicht verschlüsselt. Nutze sie nur im eigenen WLAN, nicht in öffentlichen Netzen. Sie schaltet sich nach 30 Minuten ohne Nutzung selbst ab.",
        "Bağlantı şifreli değildir. Yalnızca kendi Wi-Fi ağında kullan, halka açık ağlarda kullanma. 30 dakika kullanılmazsa kendiliğinden kapanır.",
        "La conexión no está cifrada. Úsala solo en tu propia Wi-Fi, no en redes públicas. Se apaga sola tras 30 minutos sin uso."
    )
    val browsers = Txt("Paired browsers", "Gekoppelte Browser", "Eşleştirilmiş tarayıcılar", "Navegadores emparejados")
    val noBrowsers = Txt("None yet", "Noch keine", "Henüz yok", "Ninguno todavía")
    val lastUsed = Txt("last used %s", "zuletzt %s", "son kullanım %s", "último uso %s")
    val remove = Txt("Remove", "Entfernen", "Kaldır", "Quitar")
    val notifTitle = Txt("PC access is on", "PC-Zugriff ist an", "PC erişimi açık", "Acceso desde PC activo")
    val notifText = Txt("%1\$s · Code %2\$s", "%1\$s · Code %2\$s", "%1\$s · Kod %2\$s", "%1\$s · Código %2\$s")
    val stop = Txt("Stop", "Beenden", "Durdur", "Detener")
    val channel = Txt("PC access", "PC-Zugriff", "PC erişimi", "Acceso desde PC")
}
