package com.example.lifeorganizer.core.i18n

/** UI strings of the embedded Waypoints module. */
object WpStr {
    val title = Txt("Waypoints", "Waypoints", "Konumlar", "Lugares")
    val sort = Str.sort
    val toggleView = Txt("Switch list / map", "Liste / Karte wechseln", "Liste / harita", "Lista / mapa")
    val search = Txt("Search", "Suchen", "Ara", "Buscar")
    val searchWaypoints = Txt("Search waypoints", "Waypoints durchsuchen", "Konumlarda ara", "Buscar lugares")
    val addWaypoint = Txt("Add waypoint", "Waypoint hinzufügen", "Konum ekle", "Añadir lugar")
    val editWaypoint = Txt("Edit waypoint", "Waypoint bearbeiten", "Konumu düzenle", "Editar lugar")
    val fromHome = Txt("Navigating from: Home", "Start: Zuhause", "Başlangıç: Ev", "Salida: casa")
    val fromCurrent = Txt("Navigating from: Current location", "Start: Aktueller Standort", "Başlangıç: Mevcut konum", "Salida: ubicación actual")
    val noWaypoints = Txt("No waypoints found.", "Keine Waypoints gefunden.", "Konum bulunamadı.", "No se encontraron lugares.")
    val sortRecent = Txt("Recently added", "Zuletzt hinzugefügt", "Son eklenen", "Añadidos recientemente")
    val sortName = Txt("Name", "Name", "Ad", "Nombre")
    val sortLastUsed = Txt("Last used", "Zuletzt verwendet", "Son kullanılan", "Último uso")
    val sortLastEdited = Txt("Last edited", "Zuletzt bearbeitet", "Son düzenlenen", "Última edición")

    val customName = Txt("Name (e.g. Sarah's house)", "Name (z. B. Bei Sarah)", "Ad (örn. Sarah'nın evi)", "Nombre (p. ej. Casa de Sara)")
    val address = Txt("Address", "Adresse", "Adres", "Dirección")
    val notesOptional = Txt("Notes (optional)", "Notizen (optional)", "Notlar (isteğe bağlı)", "Notas (opcional)")
    val noLabelsAvailable = Txt(
        "No labels yet – create them in the waypoint settings.",
        "Noch keine Labels – lege sie in den Waypoint-Einstellungen an.",
        "Henüz etiket yok – konum ayarlarında oluştur.",
        "Aún no hay etiquetas: créalas en los ajustes de lugares."
    )
    val enableLocation = Txt("Please enable location services", "Bitte Standortdienste aktivieren", "Lütfen konum servislerini aç", "Activa los servicios de ubicación")
    val useCurrentLocation = Txt("Use current location", "Aktuellen Standort verwenden", "Mevcut konumu kullan", "Usar ubicación actual")

    val appearance = Txt("Appearance", "Darstellung", "Görünüm", "Apariencia")
    val cardSize = Txt("Card size", "Kartengröße", "Kart boyutu", "Tamaño de tarjeta")
    val smaller = Txt("Smaller", "Kleiner", "Daha küçük", "Más pequeño")
    val default = Txt("Default", "Standard", "Varsayılan", "Predeterminado")
    val larger = Txt("Larger", "Größer", "Daha büyük", "Más grande")
    val hideAddresses = Txt("Hide addresses", "Adressen ausblenden", "Adresleri gizle", "Ocultar direcciones")
    val hideAddressesDesc = Txt(
        "Hide all addresses in the list",
        "Alle Adressen in der Liste verbergen",
        "Listedeki tüm adresleri gizle",
        "Oculta todas las direcciones de la lista"
    )
    val newLabelName = Str.newLabel
    val addLabel = Txt("Add label", "Label hinzufügen", "Etiket ekle", "Añadir etiqueta")
    val labelColor = Txt("Label color", "Label-Farbe", "Etiket rengi", "Color de etiqueta")
    val noLabelsCreated = Str.noLabels
    val deleteLabel = Txt("Delete label", "Label löschen", "Etiketi sil", "Eliminar etiqueta")
    val navigation = Txt("Navigation", "Navigation", "Navigasyon", "Navegación")
    val homeAddress = Txt("Home address", "Heimadresse", "Ev adresi", "Dirección de casa")
    val enterHomeAddress = Txt("Enter home address", "Heimadresse eingeben", "Ev adresini gir", "Introduce la dirección de casa")
    val transportMode = Txt("Default transport mode", "Standard-Verkehrsmittel", "Varsayılan ulaşım", "Transporte predeterminado")
    val car = Txt("Car", "Auto", "Araba", "Coche")
    val bike = Txt("Bike", "Fahrrad", "Bisiklet", "Bicicleta")
    val transit = Txt("Public transport", "ÖPNV", "Toplu taşıma", "Transporte público")
    val dataBackup = Txt("Data & backup", "Daten & Backup", "Veri ve yedek", "Datos y copia")
    val export = Txt("Export", "Exportieren", "Dışa aktar", "Exportar")
    val import = Txt("Import", "Importieren", "İçe aktar", "Importar")

    val planEvent = Txt("Plan an event here", "Termin hier planen", "Burada etkinlik planla", "Planear un evento aquí")
    val pin = Txt("Pin", "Anheften", "Sabitle", "Fijar")
    val unpin = Txt("Unpin", "Lösen", "Sabitlemeyi kaldır", "Desfijar")
    val moreOptions = Str.more
    val navigate = Str.navigate
    val deleteTitle = Txt("Delete %s?", "%s löschen?", "%s silinsin mi?", "¿Eliminar %s?")
    val deleteMessage = Txt(
        "This location will be removed permanently.",
        "Dieser Ort wird endgültig entfernt.",
        "Bu konum kalıcı olarak silinecek.",
        "Este lugar se eliminará definitivamente."
    )
}
