package com.example.lifeorganizer.core.i18n

/** Texts of the Backup & Import screen. */
object BkStr {
    val autoBackup = Txt("Automatic backup (weekly)", "Automatisches Backup (wöchentlich)", "Otomatik yedek (haftalık)", "Copia automática (semanal)", "自动备份（每周）")
    val autoBackupDesc = Txt(
        "Keeps the last %1\$d backups in %2\$s and a copy in Documents/LifeHub, which stays when the app is uninstalled. After reinstalling, restore it in the setup or here with “Import”.",
        "Behält die letzten %1\$d Backups in %2\$s und eine Kopie in Dokumente/LifeHub, die beim Deinstallieren bleibt. Nach einer Neuinstallation im Setup oder hier mit „Importieren“ zurückholen.",
        "Son %1\$d yedeği %2\$s içinde ve uygulama kaldırılınca da kalan bir kopyayı Belgeler/LifeHub içinde tutar. Yeniden kurulumdan sonra kurulumda veya burada “İçe aktar” ile geri yükle.",
        "Guarda las últimas %1\$d copias en %2\$s y una copia en Documentos/LifeHub, que se conserva al desinstalar la app. Tras reinstalar, restáurala en la configuración o aquí con «Importar».",
        "在 %2\$s 中保留最近 %1\$d 份备份，并在“文档/LifeHub”中保留一份卸载应用后仍存在的副本。重新安装后可在设置向导或此处通过“导入”恢复。"
    )
    val autoBackupDescOld = Txt(
        "Keeps the last %1\$d backups in %2\$s. Deleted when the app is uninstalled – choose a copy folder below or export to another place from time to time.",
        "Behält die letzten %1\$d Backups in %2\$s. Wird beim Deinstallieren gelöscht – wähle unten einen Kopie-Ordner oder exportiere ab und zu woanders hin.",
        "Son %1\$d yedeği %2\$s içinde tutar. Uygulama kaldırılınca silinir – aşağıdan bir kopya klasörü seç veya ara sıra başka bir yere dışa aktar.",
        "Guarda las últimas %1\$d copias en %2\$s. Se borran al desinstalar: elige abajo una carpeta de copia o exporta de vez en cuando a otro sitio.",
        "在 %2\$s 中保留最近 %1\$d 份备份。卸载应用时会被删除 – 请在下方选择副本文件夹，或不时导出到其他位置。"
    )
    val restoreBackup = Txt("Restore from a backup", "Aus einem Backup wiederherstellen", "Yedekten geri yükle", "Restaurar desde una copia", "从备份恢复")
    val restoreBackupHint = Txt(
        "Used LifeHub before? Pick a backup, e.g. from Documents/LifeHub.",
        "LifeHub schon mal genutzt? Wähle ein Backup, z. B. aus Dokumente/LifeHub.",
        "LifeHub'ı daha önce kullandın mı? Bir yedek seç, ör. Belgeler/LifeHub içinden.",
        "¿Ya usaste LifeHub? Elige una copia, p. ej. de Documentos/LifeHub.",
        "以前用过 LifeHub？选择一个备份，例如来自“文档/LifeHub”。"
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
