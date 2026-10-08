# Finanzen – Design

Stand: 2026-10-08 · Status: Entwurf, vom User abschnittsweise bestätigt

## Ziel

Monatlicher Überblick über Einnahmen und Ausgaben in LifeHub: Was kam rein, was ging raus, was bleibt übrig,
wofür wurde Geld ausgegeben und ist man im Budget.

## Entscheidungen des Users

- Funktionen: Kassensturz, Kategorien mit Diagramm, Budgets pro Kategorie, Fixkosten automatisch jeden Monat.
- Eingabe: per Hand, Smart Add (gemeinsames Feld mit Terminen/Notizen), CSV-Import deutscher Banken.
- Ein gemeinsamer Topf, keine getrennten Konten.
- Fixkosten im Kalender nur, wenn pro Vorlage selbst eingeschaltet.
- Kategorisierung: gelernte Händler-Regeln zuerst, unbekannte Händler optional per Groq (Schalter, nur mit Key).
- Fixkosten als Vorlagen, die echte Buchungen erzeugen (Variante A).
- Fixkosten löschen nur nach Bestätigung; gebuchte Monate bleiben erhalten.

## Annahmen

- Nur Euro, kein Kontostand, nur Monatsbilanz.
- Offline-first, alles im Backup.
- Eigener Menüpunkt „Finanzen“ wie Notizen/Dokumente.

## Nicht im Umfang

- Bank-API / Online-Banking-Anbindung (Kosten, Zugangsdaten, Datenschutz).
- Mehrere Konten, Fremdwährungen, Kontostand.
- Homescreen-Widget.

## Architektur

Neues Gradle-Modul `feature-money`, aufgebaut wie `feature-notes`:

- `data/` – Room-Datenbank `MoneyDatabase` (v1, `exportSchema = true`, Schemas in `feature-money/schemas`), DAOs, Repository.
- `domain/` – reine Kotlin-Logik ohne Android: `CsvImporter`, `PayeeNormalizer`, `RecurringScheduler`, `MoneyMath`.
- `ui/` – Compose-Screens und `MoneyViewModel`.
- `work/` – `RecurringWorker` (WorkManager, täglich).

Abhängigkeiten: `core` (i18n `Str`, Settings, Smart Add). Kalender-Anbindung über `app` (wie bestehende Querverbindungen),
damit `feature-money` nicht von `feature-calendar` abhängt.

## Datenmodell

Beträge immer als `Long` in Cent. Negativ = Ausgabe, positiv = Einnahme.

`Transaction`
- `id`, `date` (LocalDate als epochDay), `amountCents`, `title` (Händler/Titel), `note`, `categoryId?`
- `source`: MANUAL | SMART_ADD | CSV | RECURRING
- `recurringId?` – Vorlage, aus der die Buchung stammt
- `importHash?` – Hash aus Datum, Betrag, normalisiertem Text; eindeutiger Index (Duplikatschutz)

`Category`
- `id`, `name`, `color`, `icon`, `kind` (INCOME | EXPENSE), `monthlyBudgetCents?`, `sortOrder`
- Standard-Set beim ersten Start, sprachabhängig benannt, danach frei editierbar:
  Lebensmittel, Wohnen/Miete, Mobilität, Freizeit, Abos, Gesundheit, Shopping, Sonstiges (Ausgaben);
  Gehalt, Sonstige Einnahmen (Einnahmen).

`Recurring` (Fixkosten-Vorlage)
- `id`, `title`, `amountCents`, `categoryId?`, `interval` (MONTHLY | QUARTERLY | YEARLY), `dayOfMonth`,
  `startDate`, `nextDue`, `showInCalendar`, `calendarEventId?`

`PayeeRule`
- `normalizedPayee` (PK), `categoryId`, `updatedAt`

Löschen einer Vorlage: Bestätigungsdialog; danach `recurringId` alter Buchungen auf `null`, Buchungen bleiben;
verknüpfter Kalendertermin wird entfernt.

Löschen einer Kategorie: Buchungen und Regeln dieser Kategorie fallen auf „Sonstiges“ bzw. werden entfernt.

## Bildschirme

1. **Monatsübersicht** (Start): Monat wechseln (Pfeile/Wischen); Einnahmen, Ausgaben, Rest (grün/rot);
   Ring-Diagramm Ausgaben nach Kategorie; Budget-Balken pro Kategorie (gelb ab 80 %, rot ab 100 %);
   im laufenden Monat „noch erwartet: X € Fixkosten“.
2. **Buchungen**: Liste des Monats nach Tag gruppiert, Suche, Kategorie-Filter; Tippen = bearbeiten,
   Wischen = löschen mit Rückgängig; Plus-Knopf: Betrag, Ausgabe/Einnahme, Kategorie, Datum, Notiz.
3. **Fixkosten**: Liste mit Summe pro Monat (quartals-/jahresweise anteilig umgerechnet); hinzufügen,
   bearbeiten, löschen mit Bestätigung, Kalender-Schalter.
4. **Kategorien & Budgets**: bearbeiten, Budget setzen, gelernte Händler-Regeln ansehen/löschen.
5. **Import**: Datei wählen (SAF) → Vorschau mit erkannten Spalten und vorgeschlagenen Kategorien →
   „X Buchungen importieren, Y Duplikate übersprungen“.

Alle Texte in en/de/tr/es über `Str`. Landscape/Tablet über `ReadableWidth`. Diagramme mit Compose Canvas,
keine neue Bibliothek.

## CSV-Import

`CsvImporter` (pure Kotlin, unit-testbar):

1. Encoding: UTF-8 (mit/ohne BOM), sonst Windows-1252.
2. Trennzeichen: `;`, `,` oder Tab – das häufigste in den ersten Zeilen außerhalb von Anführungszeichen.
3. Kopfzeile finden: erste Zeile, die mindestens Datum- und Betrag-Spalte enthält (Vorspann der Banken wird übersprungen).
   Bekannte Namen (case-insensitive, ohne Umlaut-Unterschiede):
   - Datum: Buchungstag, Buchungsdatum, Datum, Valuta, Wertstellung (Buchungsdatum bevorzugt)
   - Betrag: Betrag, Umsatz, Betrag (EUR), Betrag (€)
   - Händler: Empfänger, Auftraggeber, Auftraggeber/Empfänger, Name Zahlungsbeteiligter, Beguenstigter/Zahlungspflichtiger, Zahlungsempfänger
   - Text: Verwendungszweck, Buchungstext, Beschreibung
   - Soll/Haben: Spalte mit Werten S/H (Volksbank-Gruppe)
   - Getrennte Spalten Soll/Haben bzw. Ausgang/Eingang werden zu einem Betrag zusammengeführt.
4. Werte: Datum `dd.MM.yyyy`, `dd.MM.yy`, `yyyy-MM-dd`; Betrag mit Komma oder Punkt als Dezimaltrenner,
   Tausenderpunkte, Währungszeichen und Leerzeichen entfernt.
5. Scheitert die Erkennung: manuelle Spaltenzuordnung; gespeichert pro Hash der Kopfzeile (DataStore).
6. Zeilen ohne gültiges Datum oder Betrag (Fußzeilen, Saldo-Zeilen) werden übersprungen und gezählt.
7. Duplikate über `importHash`.
8. Fixkosten-Abgleich: Bankbuchung mit gleichem Betrag innerhalb ±3 Tagen um eine RECURRING-Buchung
   derselben Vorlage ersetzt diese (Bankdaten gewinnen, `recurringId` bleibt).

Testdaten: Beispiel-CSVs mit erfundenen Werten im Format von Sparkasse, ING, DKB, Volksbank (S/H), N26,
Commerzbank unter `feature-money/src/test/resources/csv/`. Vorlage: bank2ynab-Formatbeschreibungen.

## Kategorisierung

1. `PayeeNormalizer`: Kleinbuchstaben, Ziffern/Filialnummern, Ortsnamen-Endungen, Rechtsformen und Sonderzeichen
   entfernen → Schlüssel (z. B. „REWE MARKT 1234 BERLIN“ → „rewe markt“).
2. Treffer in `PayeeRule` → Kategorie.
3. Sonst, falls Schalter „Unbekannte Händler per KI zuordnen“ an und Groq-Key gesetzt: ein gesammelter Aufruf
   pro Import mit Händlernamen und Kategorienliste (keine Beträge, keine IBANs).
4. Sonst „Sonstiges“ (Ausgabe) bzw. „Sonstige Einnahmen“.
5. Manuelle Änderung einer Kategorie speichert/aktualisiert die `PayeeRule`.

## Smart Add

- Neuer Typ `SmartResult.Transaction(title, amountCents, date, categoryName?)`.
- Offline (`OfflineParser`): Betrag mit `€`/`EUR` oder Dezimalkomma erkannt → Transaktion; Datumswörter
  (heute, gestern, vorgestern, Wochentage) wie bei Terminen; „+“, „Gehalt“, „bekommen“ → Einnahme.
- Groq-Prompt um Typ `transaction` erweitert.
- `app` speichert das Ergebnis über das Money-Repository.

## Fixkosten-Buchung

`RecurringScheduler` (pure Kotlin): berechnet aus `nextDue` alle fälligen Termine bis heute, auch verpasste;
Tag größer als Monatslänge → letzter Tag des Monats. Aufruf beim App-Start und täglich per `RecurringWorker`.
Idempotent: pro Vorlage und Fälligkeitsdatum höchstens eine Buchung.

„Noch erwartet“ im laufenden Monat = Vorlagen mit Fälligkeit nach heute bis Monatsende.

## Kalender (optional pro Vorlage)

Schalter an → Serientermin in LifeHub-Kategorie „Fixkosten“ (wird bei Bedarf angelegt) mit RRULE passend
zum Intervall. Schalter aus oder Vorlage gelöscht → Termin entfernt. Änderungen an Betrag/Tag aktualisieren den Termin.

## Backup

Backup-JSON um Abschnitt `money` erweitert (categories, transactions, recurring, payeeRules).
Import älterer Backups ohne `money` bleibt gültig. Kalender-Verknüpfung wird beim Import neu aufgebaut.

## Fehlerfälle

- Unlesbare CSV → verständliche Meldung, nichts importiert.
- Groq-Fehler/Offline → Fallback auf Regeln/„Sonstiges“, Import läuft weiter.
- Sehr große CSV (> 10 000 Zeilen) → Import im Hintergrund mit Fortschritt.

## Tests

- Unit: `CsvImporter` (alle Beispielformate, Encoding, S/H, getrennte Soll/Haben-Spalten, Fußzeilen),
  `PayeeNormalizer`, `RecurringScheduler` (Monatsende, verpasste Monate, quartalsweise, jährlich),
  `MoneyMath` (Monatsbilanz, Budget-Prozent), Offline-Parser für Beträge.
- Instrumented: Backup-Roundtrip mit Money-Daten; Room-Schema v1 exportiert.
