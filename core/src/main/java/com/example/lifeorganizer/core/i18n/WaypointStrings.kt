package com.example.lifeorganizer.core.i18n

/** UI strings of the embedded Waypoints module. */
object WpStr {
    val title = Txt("Waypoints", "Waypoints", "Konumlar", "Lugares", "地点")
    val sort = Str.sort
    val toggleView = Txt("Switch list / map", "Liste / Karte wechseln", "Liste / harita", "Lista / mapa", "切换列表 / 地图")
    val search = Txt("Search", "Suchen", "Ara", "Buscar", "搜索")
    val searchWaypoints = Txt("Search waypoints", "Waypoints durchsuchen", "Konumlarda ara", "Buscar lugares", "搜索地点")
    val addWaypoint = Txt("Add waypoint", "Waypoint hinzufügen", "Konum ekle", "Añadir lugar", "添加地点")
    val editWaypoint = Txt("Edit waypoint", "Waypoint bearbeiten", "Konumu düzenle", "Editar lugar", "编辑地点")
    val fromHome = Txt("Navigating from: Home", "Start: Zuhause", "Başlangıç: Ev", "Salida: casa", "导航起点：家")
    val fromCurrent = Txt("Navigating from: Current location", "Start: Aktueller Standort", "Başlangıç: Mevcut konum", "Salida: ubicación actual", "导航起点：当前位置")
    val noWaypoints = Txt("No waypoints found.", "Keine Waypoints gefunden.", "Konum bulunamadı.", "No se encontraron lugares.", "未找到地点。")
    val sortRecent = Txt("Recently added", "Zuletzt hinzugefügt", "Son eklenen", "Añadidos recientemente", "最近添加")
    val sortName = Txt("Name", "Name", "Ad", "Nombre", "名称")
    val sortLastUsed = Txt("Last used", "Zuletzt verwendet", "Son kullanılan", "Último uso", "最近使用")
    val sortLastEdited = Txt("Last edited", "Zuletzt bearbeitet", "Son düzenlenen", "Última edición", "最近编辑")

    val customName = Txt("Name (e.g. Sarah's house)", "Name (z. B. Bei Sarah)", "Ad (örn. Sarah'nın evi)", "Nombre (p. ej. Casa de Sara)", "名称（例如：Sarah 家）")
    val address = Txt("Address", "Adresse", "Adres", "Dirección", "地址")
    val notesOptional = Txt("Notes (optional)", "Notizen (optional)", "Notlar (isteğe bağlı)", "Notas (opcional)", "备注（可选）")
    val noLabelsAvailable = Txt(
        "No labels yet – create them in the waypoint settings.",
        "Noch keine Labels – lege sie in den Waypoint-Einstellungen an.",
        "Henüz etiket yok – konum ayarlarında oluştur.",
        "Aún no hay etiquetas: créalas en los ajustes de lugares.", "还没有标签 – 请在地点设置中创建。"
    )
    val enableLocation = Txt("Please enable location services", "Bitte Standortdienste aktivieren", "Lütfen konum servislerini aç", "Activa los servicios de ubicación", "请开启定位服务")
    val useCurrentLocation = Txt("Use current location", "Aktuellen Standort verwenden", "Mevcut konumu kullan", "Usar ubicación actual", "使用当前位置")

    val appearance = Txt("Appearance", "Darstellung", "Görünüm", "Apariencia", "外观")
    val cardSize = Txt("Card size", "Kartengröße", "Kart boyutu", "Tamaño de tarjeta", "卡片大小")
    val smaller = Txt("Smaller", "Kleiner", "Daha küçük", "Más pequeño", "更小")
    val default = Txt("Default", "Standard", "Varsayılan", "Predeterminado", "默认")
    val larger = Txt("Larger", "Größer", "Daha büyük", "Más grande", "更大")
    val hideAddresses = Txt("Hide addresses", "Adressen ausblenden", "Adresleri gizle", "Ocultar direcciones", "隐藏地址")
    val hideAddressesDesc = Txt(
        "Hide all addresses in the list",
        "Alle Adressen in der Liste verbergen",
        "Listedeki tüm adresleri gizle",
        "Oculta todas las direcciones de la lista", "隐藏列表中的所有地址"
    )
    val newLabelName = Str.newLabel
    val addLabel = Txt("Add label", "Label hinzufügen", "Etiket ekle", "Añadir etiqueta", "添加标签")
    val labelColor = Txt("Label color", "Label-Farbe", "Etiket rengi", "Color de etiqueta", "标签颜色")
    val noLabelsCreated = Str.noLabels
    val deleteLabel = Txt("Delete label", "Label löschen", "Etiketi sil", "Eliminar etiqueta", "删除标签")
    val navigation = Txt("Navigation", "Navigation", "Navigasyon", "Navegación", "导航")
    val homeAddress = Txt("Home address", "Heimadresse", "Ev adresi", "Dirección de casa", "家庭地址")
    val enterHomeAddress = Txt("Enter home address", "Heimadresse eingeben", "Ev adresini gir", "Introduce la dirección de casa", "输入家庭地址")
    val transportMode = Txt("Default transport mode", "Standard-Verkehrsmittel", "Varsayılan ulaşım", "Transporte predeterminado", "默认出行方式")
    val car = Txt("Car", "Auto", "Araba", "Coche", "汽车")
    val bike = Txt("Bike", "Fahrrad", "Bisiklet", "Bicicleta", "自行车")
    val transit = Txt("Public transport", "ÖPNV", "Toplu taşıma", "Transporte público", "公共交通")
    val dataBackup = Txt("Data & backup", "Daten & Backup", "Veri ve yedek", "Datos y copia", "数据与备份")
    val export = Txt("Export", "Exportieren", "Dışa aktar", "Exportar", "导出")
    val import = Txt("Import", "Importieren", "İçe aktar", "Importar", "导入")

    val planEvent = Txt("Plan an event here", "Termin hier planen", "Burada etkinlik planla", "Planear un evento aquí", "在此计划日程")
    val pin = Txt("Pin", "Anheften", "Sabitle", "Fijar", "置顶")
    val unpin = Txt("Unpin", "Lösen", "Sabitlemeyi kaldır", "Desfijar", "取消置顶")
    val moreOptions = Str.more
    val navigate = Str.navigate
    val deleteTitle = Txt("Delete %s?", "%s löschen?", "%s silinsin mi?", "¿Eliminar %s?", "删除 %s？")
    val deleteMessage = Txt(
        "This location will be removed permanently.",
        "Dieser Ort wird endgültig entfernt.",
        "Bu konum kalıcı olarak silinecek.",
        "Este lugar se eliminará definitivamente.", "此地点将被永久删除。"
    )
}
