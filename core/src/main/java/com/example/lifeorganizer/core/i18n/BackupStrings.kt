package com.example.lifeorganizer.core.i18n

/** Texts of the Backup & Import screen. */
object BkStr {
    val autoBackup = Txt("Automatic backup (weekly)", "Automatisches Backup (wöchentlich)", "Otomatik yedek (haftalık)", "Copia automática (semanal)", "自动备份（每周）")
    val autoBackupDesc = Txt(
        "Keeps the last 5 backups in %s. Deleted when the app is uninstalled – export to another place from time to time.",
        "Behält die letzten 5 Backups in %s. Wird beim Deinstallieren gelöscht – exportiere ab und zu auch woanders hin.",
        "Son 5 yedeği %s içinde tutar. Uygulama kaldırılınca silinir – ara sıra başka bir yere de dışa aktar.",
        "Guarda las últimas 5 copias en %s. Se borran al desinstalar la app: exporta de vez en cuando a otro sitio.", "在 %s 中保留最近 5 份备份。卸载应用时会被删除 – 请不时导出到其他位置。"
    )
    val lastBackup = Txt("Last backup: %s", "Letztes Backup: %s", "Son yedek: %s", "Última copia: %s", "上次备份：%s")
    val noBackupYet = Txt("No backup yet", "Noch kein Backup", "Henüz yedek yok", "Aún no hay copia", "尚无备份")
    val chooseFolder = Txt("Choose folder", "Ordner wählen", "Klasör seç", "Elegir carpeta", "选择文件夹")
    val copyFolder = Txt("Also copied to: %s", "Zusätzlich kopiert nach: %s", "Ayrıca kopyalanır: %s", "También se copia en: %s", "同时复制到：%s")
    val copyFolderNone = Txt(
        "Optional: copy to a folder of your choice (e.g. Google Drive) – survives uninstalling.",
        "Optional: Kopie in einen Ordner deiner Wahl (z. B. Google Drive) – übersteht Deinstallation.",
        "İsteğe bağlı: seçtiğin klasöre kopya (örn. Google Drive) – kaldırmaya dayanır.",
        "Opcional: copia en una carpeta (p. ej. Google Drive): sobrevive a la desinstalación.", "可选：复制到你选择的文件夹（如 Google Drive）– 卸载后仍会保留。"
    )
    val backupNow = Txt("Back up now", "Jetzt sichern", "Şimdi yedekle", "Copiar ahora", "立即备份")
    val title = Txt("Backup & import", "Backup & Import", "Yedek ve içe aktarma", "Copia e importación", "备份与导入")
    val fullBackup = Txt("Full backup", "Komplett-Backup", "Tam yedek", "Copia completa", "完整备份")
    val fullBackupDesc = Txt(
        "Events, notes, labels, templates, waypoints and document links in one file – e.g. for a new phone.",
        "Termine, Notizen, Labels, Vorlagen, Waypoints und Dokument-Verweise in einer Datei – z. B. fürs neue Handy.",
        "Etkinlikler, notlar, etiketler, şablonlar, konumlar ve belge bağlantıları tek dosyada – örn. yeni telefon için.",
        "Eventos, notas, etiquetas, plantillas, lugares y enlaces de documentos en un archivo, p. ej. para un móvil nuevo.", "日程、笔记、标签、模板、地点和文档链接都在一个文件中 – 例如用于新手机。"
    )
    val exportBtn = Txt("Export", "Exportieren", "Dışa aktar", "Exportar", "导出")
    val importBtn = Txt("Import file", "Datei importieren", "Dosya içe aktar", "Importar archivo", "导入文件")
    val working = Txt("Working…", "Wird verarbeitet…", "İşleniyor…", "Procesando…", "处理中…")
    val exported = Txt("Backup saved", "Backup gespeichert", "Yedek kaydedildi", "Copia guardada", "备份已保存")
    val imported = Txt("Import finished", "Import abgeschlossen", "İçe aktarma tamamlandı", "Importación completada", "导入完成")
    val failed = Txt("Something went wrong", "Etwas ist schiefgelaufen", "Bir şeyler ters gitti", "Algo salió mal", "出了点问题")
    val unknownFormat = Txt(
        "This file wasn't recognised. Supported: LifeHub backup, .ics (Google, Outlook …).",
        "Diese Datei wurde nicht erkannt. Unterstützt: LifeHub-Backup, .ics (Google, Outlook …).",
        "Bu dosya tanınmadı. Desteklenenler: LifeHub yedeği, .ics (Google, Outlook …).",
        "No se reconoció el archivo. Compatibles: copia de LifeHub, .ics (Google, Outlook …).", "无法识别此文件。支持：LifeHub 备份、.ics（Google、Outlook …）。"
    )
    val detected = Txt("Detected", "Erkannt", "Algılandı", "Detectado", "已识别")
    val added = Txt("added", "hinzugefügt", "eklendi", "añadidos", "已添加")
    val skippedDup = Txt("already existed (skipped)", "schon vorhanden (übersprungen)", "zaten vardı (atlandı)", "ya existían (omitidos)", "已存在（已跳过）")
    val ok = Str.ok
    val events = Str.events
    val notes = Str.notes
    val waypoints = Str.waypoints
    val documents = Txt("Documents", "Dokumente", "Belgeler", "Documentos", "文档")
    val transactions = Txt("Transactions", "Buchungen", "İşlemler", "Movimientos", "交易")
}
