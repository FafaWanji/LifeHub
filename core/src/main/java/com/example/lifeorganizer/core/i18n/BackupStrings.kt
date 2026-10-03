package com.example.lifeorganizer.core.i18n

/** Texts of the Backup & Import screen. */
object BkStr {
    val title = Txt("Backup & import", "Backup & Import", "Yedek ve içe aktarma", "Copia e importación")
    val fullBackup = Txt("Full backup", "Komplett-Backup", "Tam yedek", "Copia completa")
    val fullBackupDesc = Txt(
        "Events, notes, labels, templates, waypoints and document links in one file – e.g. for a new phone.",
        "Termine, Notizen, Labels, Vorlagen, Waypoints und Dokument-Verweise in einer Datei – z. B. fürs neue Handy.",
        "Etkinlikler, notlar, etiketler, şablonlar, konumlar ve belge bağlantıları tek dosyada – örn. yeni telefon için.",
        "Eventos, notas, etiquetas, plantillas, lugares y enlaces de documentos en un archivo, p. ej. para un móvil nuevo."
    )
    val exportBtn = Txt("Export", "Exportieren", "Dışa aktar", "Exportar")
    val importBtn = Txt("Import file", "Datei importieren", "Dosya içe aktar", "Importar archivo")
    val pasteBtn = Txt("Paste from clipboard", "Aus Zwischenablage", "Panodan yapıştır", "Pegar del portapapeles")
    val fromOldApps = Txt("From your old apps", "Aus den alten Apps", "Eski uygulamalardan", "Desde tus apps antiguas")
    val fromOldAppsDesc = Txt(
        "Every import merges: nothing is overwritten and duplicates are skipped. The file type is detected automatically.",
        "Jeder Import führt zusammen: nichts wird überschrieben, Duplikate werden übersprungen. Das Format wird automatisch erkannt.",
        "Her içe aktarma birleştirir: hiçbir şey üzerine yazılmaz, kopyalar atlanır. Biçim otomatik algılanır.",
        "Cada importación fusiona: no se sobrescribe nada y se omiten duplicados. El formato se detecta solo."
    )
    val calendarHow = Txt(
        "In the old app: Settings → Export → share/save the .ics file. Then import it here.",
        "In der alten App: Einstellungen → Export → .ics-Datei speichern/teilen. Dann hier importieren.",
        "Eski uygulamada: Ayarlar → Dışa aktar → .ics dosyasını kaydet. Sonra burada içe aktar.",
        "En la app antigua: Ajustes → Exportar → guarda el .ics. Luego impórtalo aquí."
    )
    val waypointsHow = Txt(
        "In Waypoints IRL: Settings → Data & Backup → Export. Then import the .json here.",
        "In Waypoints IRL: Settings → Data & Backup → Export. Danach die .json hier importieren.",
        "Waypoints IRL'de: Settings → Data & Backup → Export. Sonra .json'u burada içe aktar.",
        "En Waypoints IRL: Settings → Data & Backup → Export. Luego importa el .json aquí."
    )
    val lifeBaseHow = Txt(
        "Open LifeBase in the browser, press F12 → Console and run:\ncopy(localStorage.getItem('lb_offline_data'))\nThen send the copied text to your phone and tap “Paste from clipboard”.",
        "LifeBase im Browser öffnen, F12 → Konsole und eingeben:\ncopy(localStorage.getItem('lb_offline_data'))\nDen kopierten Text aufs Handy schicken und „Aus Zwischenablage“ tippen.",
        "LifeBase'i tarayıcıda aç, F12 → Konsol ve şunu çalıştır:\ncopy(localStorage.getItem('lb_offline_data'))\nKopyalanan metni telefona gönder ve “Panodan yapıştır”a dokun.",
        "Abre LifeBase en el navegador, F12 → Consola y ejecuta:\ncopy(localStorage.getItem('lb_offline_data'))\nEnvía el texto al móvil y pulsa «Pegar del portapapeles»."
    )
    val checklistHow = Txt(
        "Copy checklist_data.json from the Checklist folder on your PC to the phone (or its content to the clipboard). Each page becomes a checklist note.",
        "checklist_data.json aus dem Checklist-Ordner am PC aufs Handy kopieren (oder den Inhalt in die Zwischenablage). Jede Seite wird zu einer Checklisten-Notiz.",
        "PC'deki Checklist klasöründen checklist_data.json'u telefona kopyala (veya içeriğini panoya). Her sayfa bir kontrol listesi notu olur.",
        "Copia checklist_data.json de la carpeta Checklist del PC al móvil (o su contenido al portapapeles). Cada página será una nota con casillas."
    )
    val dokkiHow = Txt(
        "DocPocket only stored links to your files, the files themselves are still on the phone. Re-add them in Dokki – several at once is now possible.",
        "DocPocket hat nur Verweise auf deine Dateien gespeichert, die Dateien selbst liegen weiter auf dem Handy. Füge sie in Dokki neu hinzu – jetzt auch mehrere auf einmal.",
        "DocPocket yalnızca dosyalarına bağlantı saklıyordu; dosyalar hâlâ telefonda. Dokki'de yeniden ekle – artık birden fazlası aynı anda.",
        "DocPocket solo guardaba enlaces; los archivos siguen en el móvil. Vuelve a añadirlos en Dokki, ahora varios a la vez."
    )
    val openDokki = Txt("Open Dokki", "Dokki öffnen", "Dokki'yi aç", "Abrir Dokki")
    val working = Txt("Working…", "Wird verarbeitet…", "İşleniyor…", "Procesando…")
    val exported = Txt("Backup saved", "Backup gespeichert", "Yedek kaydedildi", "Copia guardada")
    val imported = Txt("Import finished", "Import abgeschlossen", "İçe aktarma tamamlandı", "Importación completada")
    val failed = Txt("Something went wrong", "Etwas ist schiefgelaufen", "Bir şeyler ters gitti", "Algo salió mal")
    val unknownFormat = Txt(
        "This file wasn't recognised. Supported: LifeOrganizer backup, .ics, Waypoints IRL, LifeBase, Checklist.",
        "Diese Datei wurde nicht erkannt. Unterstützt: LifeOrganizer-Backup, .ics, Waypoints IRL, LifeBase, Checklist.",
        "Bu dosya tanınmadı. Desteklenenler: LifeOrganizer yedeği, .ics, Waypoints IRL, LifeBase, Checklist.",
        "No se reconoció el archivo. Compatibles: copia de LifeOrganizer, .ics, Waypoints IRL, LifeBase, Checklist."
    )
    val clipboardEmpty = Txt("The clipboard is empty.", "Die Zwischenablage ist leer.", "Pano boş.", "El portapapeles está vacío.")
    val detected = Txt("Detected", "Erkannt", "Algılandı", "Detectado")
    val added = Txt("added", "hinzugefügt", "eklendi", "añadidos")
    val skippedDup = Txt("already existed (skipped)", "schon vorhanden (übersprungen)", "zaten vardı (atlandı)", "ya existían (omitidos)")
    val ok = Str.ok
    val events = Str.events
    val notes = Str.notes
    val waypoints = Str.waypoints
    val documents = Txt("Documents", "Dokumente", "Belgeler", "Documentos")
}
