package com.example.lifeorganizer.money.ui

import com.example.lifeorganizer.core.i18n.Txt

object MoneyStr {
    val finance = Txt("Finance", "Finanzen", "Finans", "Finanzas")
    val overview = Txt("Overview", "Übersicht", "Genel bakış", "Resumen")
    val transactions = Txt("Transactions", "Buchungen", "İşlemler", "Movimientos")
    val fixedCosts = Txt("Fixed costs", "Fixkosten", "Sabit giderler", "Gastos fijos")
    val categories = Txt("Categories", "Kategorien", "Kategoriler", "Categorías")
    val income = Txt("Income", "Einnahmen", "Gelir", "Ingresos")
    val expenses = Txt("Expenses", "Ausgaben", "Giderler", "Gastos")
    val rest = Txt("Left", "Rest", "Kalan", "Restante")
    val expectedFixed = Txt("Still expected: %s fixed costs", "Noch erwartet: %s Fixkosten", "Beklenen: %s sabit gider", "Aún previstos: %s en gastos fijos")
    val noTransactions = Txt("No transactions this month", "Keine Buchungen in diesem Monat", "Bu ay işlem yok", "Sin movimientos este mes")
    val newTransaction = Txt("New transaction", "Neue Buchung", "Yeni işlem", "Nuevo movimiento")
    val editTransaction = Txt("Edit transaction", "Buchung bearbeiten", "İşlemi düzenle", "Editar movimiento")
    val amount = Txt("Amount", "Betrag", "Tutar", "Importe")
    val expense = Txt("Expense", "Ausgabe", "Gider", "Gasto")
    val incomeOne = Txt("Income", "Einnahme", "Gelir", "Ingreso")
    val category = Txt("Category", "Kategorie", "Kategori", "Categoría")
    val date = Txt("Date", "Datum", "Tarih", "Fecha")
    val title = Txt("Title", "Titel", "Başlık", "Título")
    val note = Txt("Note", "Notiz", "Not", "Nota")
    val save = Txt("Save", "Speichern", "Kaydet", "Guardar")
    val cancel = Txt("Cancel", "Abbrechen", "İptal", "Cancelar")
    val delete = Txt("Delete", "Löschen", "Sil", "Eliminar")
    val undo = Txt("Undo", "Rückgängig", "Geri al", "Deshacer")
    val deleted = Txt("Transaction deleted", "Buchung gelöscht", "İşlem silindi", "Movimiento eliminado")
    val search = Txt("Search", "Suchen", "Ara", "Buscar")
    val all = Txt("All", "Alle", "Tümü", "Todas")
    val budget = Txt("Monthly budget", "Budget pro Monat", "Aylık bütçe", "Presupuesto mensual")
    val spentOf = Txt("%1\$s of %2\$s", "%1\$s von %2\$s", "%1\$s / %2\$s", "%1\$s de %2\$s")
    val monthly = Txt("Monthly", "Monatlich", "Aylık", "Mensual")
    val quarterly = Txt("Quarterly", "Quartalsweise", "Üç aylık", "Trimestral")
    val yearly = Txt("Yearly", "Jährlich", "Yıllık", "Anual")
    val dayOfMonth = Txt("Day of month", "Tag im Monat", "Ayın günü", "Día del mes")
    val showInCalendar = Txt("Show in calendar", "Im Kalender anzeigen", "Takvimde göster", "Mostrar en el calendario")
    val newFixed = Txt("New fixed cost", "Neue Fixkosten", "Yeni sabit gider", "Nuevo gasto fijo")
    val editFixed = Txt("Edit fixed cost", "Fixkosten bearbeiten", "Sabit gideri düzenle", "Editar gasto fijo")
    val fixedSum = Txt("Fixed costs: %s / month", "Fixkosten: %s / Monat", "Sabit giderler: %s / ay", "Gastos fijos: %s / mes")
    val deleteFixedTitle = Txt("Delete %s?", "%s löschen?", "%s silinsin mi?", "¿Eliminar %s?")
    val deleteFixedBody = Txt("Months already booked stay.", "Gebuchte Monate bleiben erhalten.", "Kaydedilmiş aylar kalır.", "Los meses ya registrados se conservan.")
    val noFixed = Txt("No fixed costs yet", "Noch keine Fixkosten", "Henüz sabit gider yok", "Aún no hay gastos fijos")
    val newCategory = Txt("New category", "Neue Kategorie", "Yeni kategori", "Nueva categoría")
    val editCategory = Txt("Edit category", "Kategorie bearbeiten", "Kategoriyi düzenle", "Editar categoría")
    val name = Txt("Name", "Name", "Ad", "Nombre")
    val deleteCategoryBody = Txt("Its transactions move to “%s”.", "Die Buchungen wandern nach „%s“.", "İşlemleri “%s” kategorisine taşınır.", "Sus movimientos pasan a «%s».")
    val rules = Txt("Learned payees", "Gelernte Händler", "Öğrenilen alıcılar", "Comercios aprendidos")
    val noRules = Txt("Nothing learned yet", "Noch nichts gelernt", "Henüz öğrenilen yok", "Nada aprendido todavía")
    val importCsv = Txt("Import bank CSV", "Bank-CSV importieren", "Banka CSV içe aktar", "Importar CSV del banco")
    val importHint = Txt(
        "Export your statement as CSV in online banking and choose the file here. Duplicates are skipped.",
        "Exportiere deinen Kontoauszug im Online-Banking als CSV und wähle die Datei hier aus. Doppelte Buchungen werden übersprungen.",
        "Hesap özetini çevrimiçi bankacılıkta CSV olarak dışa aktar ve dosyayı burada seç. Tekrarlar atlanır.",
        "Exporta tu extracto como CSV en la banca online y elige el archivo aquí. Los duplicados se omiten."
    )
    val aiCategorize = Txt("Sort unknown payees with AI", "Unbekannte Händler per KI zuordnen", "Bilinmeyen alıcıları yapay zekâ ile sınıflandır", "Clasificar comercios desconocidos con IA")
    val aiHint = Txt(
        "Only payee names are sent to Groq, no amounts or IBANs. Needs a Groq key in the settings.",
        "Nur Händlernamen gehen an Groq, keine Beträge oder IBANs. Braucht einen Groq-Key in den Einstellungen.",
        "Groq'a yalnızca alıcı adları gönderilir; tutar veya IBAN gönderilmez. Ayarlarda Groq anahtarı gerekir.",
        "Solo se envían nombres de comercios a Groq, sin importes ni IBAN. Necesita una clave de Groq en los ajustes."
    )
    val chooseFile = Txt("Choose file", "Datei wählen", "Dosya seç", "Elegir archivo")
    val mapColumns = Txt("Which column is what?", "Welche Spalte ist was?", "Hangi sütun ne?", "¿Qué columna es qué?")
    val columnDate = Txt("Date", "Datum", "Tarih", "Fecha")
    val columnAmount = Txt("Amount", "Betrag", "Tutar", "Importe")
    val columnPayee = Txt("Payee", "Händler / Empfänger", "Alıcı", "Comercio")
    val columnPurpose = Txt("Purpose", "Verwendungszweck", "Açıklama", "Concepto")
    val none = Txt("— none —", "— keine —", "— yok —", "— ninguna —")
    val apply = Txt("Continue", "Weiter", "Devam", "Continuar")
    val importN = Txt("Import %d transactions", "%d Buchungen importieren", "%d işlemi içe aktar", "Importar %d movimientos")
    val skippedRows = Txt("%d lines without date or amount skipped", "%d Zeilen ohne Datum oder Betrag übersprungen", "Tarih veya tutarı olmayan %d satır atlandı", "%d líneas sin fecha o importe omitidas")
    val importDone = Txt(
        "%1\$d imported, %2\$d duplicates skipped, %3\$d matched to fixed costs",
        "%1\$d importiert, %2\$d Duplikate übersprungen, %3\$d Fixkosten zugeordnet",
        "%1\$d içe aktarıldı, %2\$d tekrar atlandı, %3\$d sabit gidere eşlendi",
        "%1\$d importados, %2\$d duplicados omitidos, %3\$d asignados a gastos fijos"
    )
    val importFailed = Txt("The file could not be read as a bank CSV.", "Die Datei konnte nicht als Bank-CSV gelesen werden.", "Dosya banka CSV'si olarak okunamadı.", "No se pudo leer el archivo como CSV bancario.")
    val tryAgain = Txt("Try again", "Nochmal", "Tekrar dene", "Reintentar")
    val done = Txt("Done", "Fertig", "Tamam", "Listo")
    val previousMonth = Txt("Previous month", "Vorheriger Monat", "Önceki ay", "Mes anterior")
    val nextMonth = Txt("Next month", "Nächster Monat", "Sonraki ay", "Mes siguiente")
}
