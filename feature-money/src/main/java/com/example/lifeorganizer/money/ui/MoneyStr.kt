package com.example.lifeorganizer.money.ui

import com.example.lifeorganizer.core.i18n.Txt

object MoneyStr {
    val finance = Txt("Finance", "Finanzen", "Finans", "Finanzas", "财务")
    val overview = Txt("Overview", "Übersicht", "Genel bakış", "Resumen", "概览")
    val transactions = Txt("Transactions", "Buchungen", "İşlemler", "Movimientos", "交易")
    val fixedCosts = Txt("Fixed costs", "Fixkosten", "Sabit giderler", "Gastos fijos", "固定支出")
    val categories = Txt("Categories", "Kategorien", "Kategoriler", "Categorías", "分类")
    val income = Txt("Income", "Einnahmen", "Gelir", "Ingresos", "收入")
    val expenses = Txt("Expenses", "Ausgaben", "Giderler", "Gastos", "支出")
    val rest = Txt("Left", "Rest", "Kalan", "Restante", "结余")
    val expectedFixed = Txt("Still expected: %s fixed costs", "Noch erwartet: %s Fixkosten", "Beklenen: %s sabit gider", "Aún previstos: %s en gastos fijos", "尚待支出：%s 固定支出")
    val noTransactions = Txt("No transactions this month", "Keine Buchungen in diesem Monat", "Bu ay işlem yok", "Sin movimientos este mes", "本月没有交易")
    val newTransaction = Txt("New transaction", "Neue Buchung", "Yeni işlem", "Nuevo movimiento", "新建交易")
    val editTransaction = Txt("Edit transaction", "Buchung bearbeiten", "İşlemi düzenle", "Editar movimiento", "编辑交易")
    val amount = Txt("Amount", "Betrag", "Tutar", "Importe", "金额")
    val expense = Txt("Expense", "Ausgabe", "Gider", "Gasto", "支出")
    val incomeOne = Txt("Income", "Einnahme", "Gelir", "Ingreso", "收入")
    val category = Txt("Category", "Kategorie", "Kategori", "Categoría", "分类")
    val date = Txt("Date", "Datum", "Tarih", "Fecha", "日期")
    val title = Txt("Title", "Titel", "Başlık", "Título", "标题")
    val note = Txt("Note", "Notiz", "Not", "Nota", "笔记")
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar", "保存")
    val cancel = Txt("Cancel", "Abbrechen", "İptal", "Cancelar", "取消")
    val delete = Txt("Delete", "Löschen", "Sil", "Eliminar", "删除")
    val undo = Txt("Undo", "Rückgängig", "Geri al", "Deshacer", "撤销")
    val deleted = Txt("Transaction deleted", "Buchung gelöscht", "İşlem silindi", "Movimiento eliminado", "交易已删除")
    val search = Txt("Search", "Suchen", "Ara", "Buscar", "搜索")
    val all = Txt("All", "Alle", "Tümü", "Todas", "全部")
    val budget = Txt("Monthly budget", "Budget pro Monat", "Aylık bütçe", "Presupuesto mensual", "月度预算")
    val spentOf = Txt("%1\$s of %2\$s", "%1\$s von %2\$s", "%1\$s / %2\$s", "%1\$s de %2\$s", "%1\$s / %2\$s")
    val monthly = Txt("Monthly", "Monatlich", "Aylık", "Mensual", "每月")
    val quarterly = Txt("Quarterly", "Quartalsweise", "Üç aylık", "Trimestral", "每季度")
    val yearly = Txt("Yearly", "Jährlich", "Yıllık", "Anual", "每年")
    val dayOfMonth = Txt("Day of month", "Tag im Monat", "Ayın günü", "Día del mes", "每月几号")
    val showInCalendar = Txt("Show in calendar", "Im Kalender anzeigen", "Takvimde göster", "Mostrar en el calendario", "在日历中显示")
    val newFixed = Txt("New fixed cost", "Neue Fixkosten", "Yeni sabit gider", "Nuevo gasto fijo", "新建固定支出")
    val editFixed = Txt("Edit fixed cost", "Fixkosten bearbeiten", "Sabit gideri düzenle", "Editar gasto fijo", "编辑固定支出")
    val fixedSum = Txt("Fixed costs: %s / month", "Fixkosten: %s / Monat", "Sabit giderler: %s / ay", "Gastos fijos: %s / mes", "固定支出：%s / 月")
    val deleteFixedTitle = Txt("Delete %s?", "%s löschen?", "%s silinsin mi?", "¿Eliminar %s?", "删除 %s？")
    val deleteFixedBody = Txt("Months already booked stay.", "Gebuchte Monate bleiben erhalten.", "Kaydedilmiş aylar kalır.", "Los meses ya registrados se conservan.", "已记账的月份会保留。")
    val noFixed = Txt("No fixed costs yet", "Noch keine Fixkosten", "Henüz sabit gider yok", "Aún no hay gastos fijos", "还没有固定支出")
    val newCategory = Txt("New category", "Neue Kategorie", "Yeni kategori", "Nueva categoría", "新分类")
    val editCategory = Txt("Edit category", "Kategorie bearbeiten", "Kategoriyi düzenle", "Editar categoría", "编辑分类")
    val name = Txt("Name", "Name", "Ad", "Nombre", "名称")
    val deleteCategoryBody = Txt("Its transactions move to “%s”.", "Die Buchungen wandern nach „%s“.", "İşlemleri “%s” kategorisine taşınır.", "Sus movimientos pasan a «%s».", "其交易将移至“%s”。")
    val rules = Txt("Learned payees", "Gelernte Händler", "Öğrenilen alıcılar", "Comercios aprendidos", "已学习的收款方")
    val noRules = Txt("Nothing learned yet", "Noch nichts gelernt", "Henüz öğrenilen yok", "Nada aprendido todavía", "尚未学习任何内容")
    val importCsv = Txt("Import bank CSV", "Bank-CSV importieren", "Banka CSV içe aktar", "Importar CSV del banco", "导入银行 CSV")
    val importHint = Txt(
        "Export your statement as CSV in online banking and choose the file here. Duplicates are skipped.",
        "Exportiere deinen Kontoauszug im Online-Banking als CSV und wähle die Datei hier aus. Doppelte Buchungen werden übersprungen.",
        "Hesap özetini çevrimiçi bankacılıkta CSV olarak dışa aktar ve dosyayı burada seç. Tekrarlar atlanır.",
        "Exporta tu extracto como CSV en la banca online y elige el archivo aquí. Los duplicados se omiten.", "在网上银行中将对账单导出为 CSV，然后在此选择文件。重复项会被跳过。"
    )
    val aiCategorize = Txt("Sort unknown payees with AI", "Unbekannte Händler per KI zuordnen", "Bilinmeyen alıcıları yapay zekâ ile sınıflandır", "Clasificar comercios desconocidos con IA", "用 AI 归类未知收款方")
    val aiHint = Txt(
        "Only payee names are sent to Groq, no amounts or IBANs. Needs a Groq key in the settings.",
        "Nur Händlernamen gehen an Groq, keine Beträge oder IBANs. Braucht einen Groq-Key in den Einstellungen.",
        "Groq'a yalnızca alıcı adları gönderilir; tutar veya IBAN gönderilmez. Ayarlarda Groq anahtarı gerekir.",
        "Solo se envían nombres de comercios a Groq, sin importes ni IBAN. Necesita una clave de Groq en los ajustes.", "只会将收款方名称发送给 Groq，不含金额或 IBAN。需要在设置中填写 Groq 密钥。"
    )
    val chooseFile = Txt("Choose file", "Datei wählen", "Dosya seç", "Elegir archivo", "选择文件")
    val mapColumns = Txt("Which column is what?", "Welche Spalte ist was?", "Hangi sütun ne?", "¿Qué columna es qué?", "哪一列是什么？")
    val columnDate = Txt("Date", "Datum", "Tarih", "Fecha", "日期")
    val columnAmount = Txt("Amount", "Betrag", "Tutar", "Importe", "金额")
    val columnPayee = Txt("Payee", "Händler / Empfänger", "Alıcı", "Comercio", "收款方")
    val columnPurpose = Txt("Purpose", "Verwendungszweck", "Açıklama", "Concepto", "用途")
    val none = Txt("— none —", "— keine —", "— yok —", "— ninguna —", "— 无 —")
    val apply = Txt("Continue", "Weiter", "Devam", "Continuar", "继续")
    val importN = Txt("Import %d transactions", "%d Buchungen importieren", "%d işlemi içe aktar", "Importar %d movimientos", "导入 %d 笔交易")
    val skippedRows = Txt("%d lines without date or amount skipped", "%d Zeilen ohne Datum oder Betrag übersprungen", "Tarih veya tutarı olmayan %d satır atlandı", "%d líneas sin fecha o importe omitidas", "已跳过 %d 行没有日期或金额的内容")
    val importDone = Txt(
        "%1\$d imported, %2\$d duplicates skipped, %3\$d matched to fixed costs",
        "%1\$d importiert, %2\$d Duplikate übersprungen, %3\$d Fixkosten zugeordnet",
        "%1\$d içe aktarıldı, %2\$d tekrar atlandı, %3\$d sabit gidere eşlendi",
        "%1\$d importados, %2\$d duplicados omitidos, %3\$d asignados a gastos fijos", "已导入 %1\$d 笔，跳过 %2\$d 笔重复，%3\$d 笔匹配到固定支出"
    )
    val importFailed = Txt("The file could not be read as a bank CSV.", "Die Datei konnte nicht als Bank-CSV gelesen werden.", "Dosya banka CSV'si olarak okunamadı.", "No se pudo leer el archivo como CSV bancario.", "无法将该文件读取为银行 CSV。")
    val tryAgain = Txt("Try again", "Nochmal", "Tekrar dene", "Reintentar", "重试")
    val done = Txt("Done", "Fertig", "Tamam", "Listo", "完成")
    val previousMonth = Txt("Previous month", "Vorheriger Monat", "Önceki ay", "Mes anterior", "上个月")
    val nextMonth = Txt("Next month", "Nächster Monat", "Sonraki ay", "Mes siguiente", "下个月")
}
