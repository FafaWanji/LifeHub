'use strict';
// LifeHub in the browser: talks to the phone's local server (/api). No external resources.

const T = {
  en: {
    calendar: 'Calendar', notes: 'Notes', money: 'Finance', logout: 'Disconnect',
    pairTitle: 'Connect to LifeHub', pairHint: 'Enter the 6-digit code shown in LifeHub on your phone (Menu → PC access).',
    connect: 'Connect', wrongCode: 'Wrong code. Check the code on your phone.', offline: 'Phone not reachable. Is PC access still on?', offlineBanner: 'Phone not connected – read only. You can edit again as soon as LifeHub is reachable.', readOnly: 'Read only while the phone is not connected',
    today: 'Today', newEvent: 'New event', editEvent: 'Edit event', title: 'Title', allDay: 'All day', date: 'Date',
    start: 'Start', end: 'End', repeat: 'Repeat', none: 'None', daily: 'Daily', weekly: 'Weekly', monthly: 'Monthly', yearly: 'Yearly',
    asInApp: 'As set in the app', category: 'Category', reminder: 'Reminder', atStart: 'At start', min10: '10 minutes before',
    min30: '30 minutes before', hour1: '1 hour before', day1: '1 day before', location: 'Location', description: 'Description',
    save: 'Save', cancel: 'Cancel', delete: 'Delete', confirmDelete: 'Really delete?', seriesHint: 'Changes apply to the whole series.',
    birthdayHint: 'Birthdays can only be edited in the app.', noEvents: 'No events', saved: 'Saved',
    newNote: 'New note', search: 'Search', untitled: 'Untitled', preview: 'Preview', edit: 'Edit', pin: 'Pin', unpin: 'Unpin',
    noNotes: 'No notes yet', pickNote: 'Choose a note or create a new one.', saving: 'Saving…',
    income: 'Income', expenses: 'Expenses', left: 'Left', expected: 'Still expected: {0} fixed costs', byCategory: 'By category',
    transactions: 'Transactions', add: 'Add', expense: 'Expense', incomeOne: 'Income', amount: 'Amount', auto: 'Automatic',
    noTx: 'No transactions this month', budgetOf: '{0} of {1}', error: 'Something went wrong'
  },
  de: {
    calendar: 'Kalender', notes: 'Notizen', money: 'Finanzen', logout: 'Trennen',
    pairTitle: 'Mit LifeHub verbinden', pairHint: 'Gib den 6-stelligen Code ein, den LifeHub auf deinem Handy zeigt (Menü → PC-Zugriff).',
    connect: 'Verbinden', wrongCode: 'Falscher Code. Prüfe den Code auf dem Handy.', offline: 'Handy nicht erreichbar. Ist der PC-Zugriff noch an?', offlineBanner: 'Handy nicht verbunden – nur lesen. Bearbeiten geht wieder, sobald LifeHub erreichbar ist.', readOnly: 'Nur lesen, solange das Handy nicht verbunden ist',
    today: 'Heute', newEvent: 'Neuer Termin', editEvent: 'Termin bearbeiten', title: 'Titel', allDay: 'Ganztägig', date: 'Datum',
    start: 'Beginn', end: 'Ende', repeat: 'Wiederholung', none: 'Keine', daily: 'Täglich', weekly: 'Wöchentlich', monthly: 'Monatlich', yearly: 'Jährlich',
    asInApp: 'Wie in der App eingestellt', category: 'Kategorie', reminder: 'Erinnerung', atStart: 'Zum Beginn', min10: '10 Minuten vorher',
    min30: '30 Minuten vorher', hour1: '1 Stunde vorher', day1: '1 Tag vorher', location: 'Ort', description: 'Beschreibung',
    save: 'Speichern', cancel: 'Abbrechen', delete: 'Löschen', confirmDelete: 'Wirklich löschen?', seriesHint: 'Änderungen gelten für die ganze Serie.',
    birthdayHint: 'Geburtstage lassen sich nur in der App bearbeiten.', noEvents: 'Keine Termine', saved: 'Gespeichert',
    newNote: 'Neue Notiz', search: 'Suchen', untitled: 'Ohne Titel', preview: 'Vorschau', edit: 'Bearbeiten', pin: 'Anheften', unpin: 'Lösen',
    noNotes: 'Noch keine Notizen', pickNote: 'Wähle eine Notiz oder lege eine neue an.', saving: 'Speichert…',
    income: 'Einnahmen', expenses: 'Ausgaben', left: 'Rest', expected: 'Noch erwartet: {0} Fixkosten', byCategory: 'Nach Kategorie',
    transactions: 'Buchungen', add: 'Hinzufügen', expense: 'Ausgabe', incomeOne: 'Einnahme', amount: 'Betrag', auto: 'Automatisch',
    noTx: 'Keine Buchungen in diesem Monat', budgetOf: '{0} von {1}', error: 'Etwas ist schiefgelaufen',
    subNotes: 'Unternotizen', addSubNote: 'Unternotiz hinzufügen', subNoteOf: 'Unternotiz von {0}',
    detachSubNote: 'Eigenständige Notiz machen', doneLabel: 'Erledigt',
    subCount: '{0} Unternotizen', subCountOne: '1 Unternotiz',
    moveContent: 'Inhalt in Unternotiz verschieben', moveContentTitle: 'Titel der neuen Unternotiz',
    linked: 'Handy verbunden', unlinked: 'Handy nicht erreichbar',
    themeSystem: 'Design: System', themeLight: 'Design: Hell', themeDark: 'Design: Dunkel'
  },
  tr: {
    calendar: 'Takvim', notes: 'Notlar', money: 'Finans', logout: 'Bağlantıyı kes',
    pairTitle: "LifeHub'a bağlan", pairHint: 'Telefonundaki LifeHub\'ın gösterdiği 6 haneli kodu gir (Menü → PC erişimi).',
    connect: 'Bağlan', wrongCode: 'Yanlış kod. Telefondaki kodu kontrol et.', offline: 'Telefona ulaşılamıyor. PC erişimi hâlâ açık mı?', offlineBanner: "Telefon bağlı değil – yalnızca okuma. LifeHub'a ulaşılınca yeniden düzenleyebilirsin.", readOnly: 'Telefon bağlı değilken yalnızca okuma',
    today: 'Bugün', newEvent: 'Yeni etkinlik', editEvent: 'Etkinliği düzenle', title: 'Başlık', allDay: 'Tüm gün', date: 'Tarih',
    start: 'Başlangıç', end: 'Bitiş', repeat: 'Tekrar', none: 'Yok', daily: 'Günlük', weekly: 'Haftalık', monthly: 'Aylık', yearly: 'Yıllık',
    asInApp: 'Uygulamadaki gibi', category: 'Kategori', reminder: 'Hatırlatıcı', atStart: 'Başlangıçta', min10: '10 dakika önce',
    min30: '30 dakika önce', hour1: '1 saat önce', day1: '1 gün önce', location: 'Konum', description: 'Açıklama',
    save: 'Kaydet', cancel: 'İptal', delete: 'Sil', confirmDelete: 'Gerçekten silinsin mi?', seriesHint: 'Değişiklikler tüm seriye uygulanır.',
    birthdayHint: 'Doğum günleri yalnızca uygulamada düzenlenebilir.', noEvents: 'Etkinlik yok', saved: 'Kaydedildi',
    newNote: 'Yeni not', search: 'Ara', untitled: 'Başlıksız', preview: 'Önizleme', edit: 'Düzenle', pin: 'Sabitle', unpin: 'Sabitlemeyi kaldır',
    noNotes: 'Henüz not yok', pickNote: 'Bir not seç ya da yenisini oluştur.', saving: 'Kaydediliyor…',
    income: 'Gelir', expenses: 'Giderler', left: 'Kalan', expected: 'Beklenen: {0} sabit gider', byCategory: 'Kategoriye göre',
    transactions: 'İşlemler', add: 'Ekle', expense: 'Gider', incomeOne: 'Gelir', amount: 'Tutar', auto: 'Otomatik',
    noTx: 'Bu ay işlem yok', budgetOf: '{0} / {1}', error: 'Bir şeyler ters gitti'
  },
  es: {
    calendar: 'Calendario', notes: 'Notas', money: 'Finanzas', logout: 'Desconectar',
    pairTitle: 'Conectar con LifeHub', pairHint: 'Introduce el código de 6 cifras que muestra LifeHub en tu móvil (Menú → Acceso desde PC).',
    connect: 'Conectar', wrongCode: 'Código incorrecto. Revisa el código en el móvil.', offline: 'Móvil no accesible. ¿Sigue activo el acceso desde PC?', offlineBanner: 'Móvil no conectado: solo lectura. Podrás editar de nuevo en cuanto LifeHub esté accesible.', readOnly: 'Solo lectura mientras el móvil no esté conectado',
    today: 'Hoy', newEvent: 'Nuevo evento', editEvent: 'Editar evento', title: 'Título', allDay: 'Todo el día', date: 'Fecha',
    start: 'Inicio', end: 'Fin', repeat: 'Repetir', none: 'Ninguna', daily: 'Diario', weekly: 'Semanal', monthly: 'Mensual', yearly: 'Anual',
    asInApp: 'Como en la app', category: 'Categoría', reminder: 'Recordatorio', atStart: 'Al inicio', min10: '10 minutos antes',
    min30: '30 minutos antes', hour1: '1 hora antes', day1: '1 día antes', location: 'Lugar', description: 'Descripción',
    save: 'Guardar', cancel: 'Cancelar', delete: 'Eliminar', confirmDelete: '¿Eliminar de verdad?', seriesHint: 'Los cambios se aplican a toda la serie.',
    birthdayHint: 'Los cumpleaños solo se editan en la app.', noEvents: 'Sin eventos', saved: 'Guardado',
    newNote: 'Nueva nota', search: 'Buscar', untitled: 'Sin título', preview: 'Vista previa', edit: 'Editar', pin: 'Fijar', unpin: 'Soltar',
    noNotes: 'Aún no hay notas', pickNote: 'Elige una nota o crea una nueva.', saving: 'Guardando…',
    income: 'Ingresos', expenses: 'Gastos', left: 'Restante', expected: 'Aún previstos: {0} en gastos fijos', byCategory: 'Por categoría',
    transactions: 'Movimientos', add: 'Añadir', expense: 'Gasto', incomeOne: 'Ingreso', amount: 'Importe', auto: 'Automático',
    noTx: 'Sin movimientos este mes', budgetOf: '{0} de {1}', error: 'Algo salió mal'
  },
  zh: {
    calendar: '日历', notes: '笔记', money: '财务', logout: '断开连接',
    pairTitle: '连接 LifeHub', pairHint: '输入手机上 LifeHub 显示的 6 位代码（菜单 → 电脑访问）。',
    connect: '连接', wrongCode: '代码错误。请核对手机上的代码。', offline: '无法连接手机。电脑访问是否仍开启？', offlineBanner: '手机未连接 – 仅可查看。LifeHub 可访问后即可再次编辑。', readOnly: '手机未连接时仅可查看',
    today: '今天', newEvent: '新建日程', editEvent: '编辑日程', title: '标题', allDay: '全天', date: '日期',
    start: '开始', end: '结束', repeat: '重复', none: '无', daily: '每天', weekly: '每周', monthly: '每月', yearly: '每年',
    asInApp: '与应用中设置相同', category: '分类', reminder: '提醒', atStart: '开始时', min10: '提前 10 分钟',
    min30: '提前 30 分钟', hour1: '提前 1 小时', day1: '提前 1 天', location: '地点', description: '描述',
    save: '保存', cancel: '取消', delete: '删除', confirmDelete: '确定删除？', seriesHint: '更改将应用于整个系列。',
    birthdayHint: '生日只能在应用中编辑。', noEvents: '没有日程', saved: '已保存',
    newNote: '新建笔记', search: '搜索', untitled: '无标题', preview: '预览', edit: '编辑', pin: '置顶', unpin: '取消置顶',
    noNotes: '还没有笔记', pickNote: '选择一条笔记或新建一条。', saving: '正在保存…',
    income: '收入', expenses: '支出', left: '结余', expected: '尚待支出：{0} 固定支出', byCategory: '按分类',
    transactions: '交易', add: '添加', expense: '支出', incomeOne: '收入', amount: '金额', auto: '自动',
    noTx: '本月没有交易', budgetOf: '{0} / {1}', error: '出了点问题'
  }
};

let lang = 'de';
// New texts come in German only and show for every language until translated
const t = (k, ...a) => ((T[lang] || T.en)[k] || T.en[k] || T.de[k] || k).replace(/\{(\d)\}/g, (_, i) => a[i]);
const $ = (sel, root = document) => root.querySelector(sel);

/** Small DOM builder: el('div', {class: 'x', onclick: f}, child, 'text'). Text is always set as text, never as HTML. */
function el(tag, attrs = {}, ...children) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === undefined || v === null || v === false) continue;
    if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else if (k === 'class') e.className = v;
    // Custom properties like '--c' only work through setProperty
    else if (k === 'style') for (const [p, s] of Object.entries(v)) p.startsWith('--') ? e.style.setProperty(p, s) : (e.style[p] = s);
    else if (k === 'value') e.value = v;
    else if (k === 'checked') e.checked = !!v;
    else e.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) if (c !== null && c !== undefined && c !== false) e.append(c instanceof Node ? c : String(c));
  return e;
}

// Line icons (lucide, ISC licence), one size and stroke everywhere; the markup is our own constants
const ICONS = {
  calendar: '<path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/>',
  notes: '<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/>',
  money: '<path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/>',
  left: '<path d="m15 18-6-6 6-6"/>',
  right: '<path d="m9 18 6-6-6-6"/>',
  plus: '<path d="M5 12h14"/><path d="M12 5v14"/>',
  pin: '<path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z"/>',
  search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
  trash: '<path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>',
  logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>',
  eye: '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/>',
  pencil: '<path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>',
  sub: '<path d="m15 10 5 5-5 5"/><path d="M4 4v7a4 4 0 0 0 4 4h12"/>',
  up: '<path d="m9 14-5-5 5-5"/><path d="M20 20v-7a4 4 0 0 0-4-4H4"/>',
  place: '<path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/>',
  clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
  split: '<path d="M16 3h5v5"/><path d="M8 3H3v5"/><path d="M12 22v-8.3a4 4 0 0 0-1.172-2.872L3 3"/><path d="m15 9 6-6"/>',
  system: '<rect width="20" height="14" x="2" y="3" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/>',
  light: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>',
  dark: '<path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>'
};

/** The phone's look: background tint (data-bg, the stylesheet has the tones) and accent colour (none: the page's ink blue). */
function applyLook(o) {
  const root = document.documentElement;
  if (o.background && root.dataset.bg !== o.background) root.dataset.bg = o.background;
  if (o.accent === undefined || applyLook.accent === o.accent) return;
  applyLook.accent = o.accent;
  if (!o.accent) { ['--acc-l', '--acc-d', '--acc-d-on'].forEach(p => root.style.removeProperty(p)); return; }
  const rgb = [1, 3, 5].map(i => parseInt(o.accent.substr(i, 2), 16));
  const lum = c => { const [r, g, b] = c.map(v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }); return 0.2126 * r + 0.7152 * g + 0.0722 * b; };
  // Light mode: darken until white text on it reads (4.5:1), as the app does for light accents like Lemon
  let light = rgb, step = 0;
  while (1.05 / (lum(light) + 0.05) < 4.5 && step < 20) { step++; light = rgb.map(v => Math.round(v * (1 - 0.05 * step))); }
  const hex = c => '#' + c.map(v => v.toString(16).padStart(2, '0')).join('');
  root.style.setProperty('--acc-l', hex(light));
  root.style.setProperty('--acc-d', o.accent);
  root.style.setProperty('--acc-d-on', lum(rgb) > 0.3 ? '#0f1320' : '#ffffff');
}

// Light or dark: the system's choice unless one is picked in the sidebar. Only this browser remembers it.
const THEMES = ['system', 'light', 'dark'];
function storedTheme() { try { return localStorage.getItem('lh_theme') || 'system'; } catch (e) { return 'system'; } }
function applyTheme(name) {
  if (name === 'system') delete document.documentElement.dataset.theme; else document.documentElement.dataset.theme = name;
  const b = $('#theme');
  if (b) b.replaceChildren(icon(name), el('span', {}, t('theme' + name[0].toUpperCase() + name.slice(1))));
}
applyTheme(storedTheme());
function icon(name) {
  const s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  s.setAttribute('viewBox', '0 0 24 24');
  s.setAttribute('class', 'i');
  s.setAttribute('aria-hidden', 'true');
  s.innerHTML = ICONS[name];
  return s;
}

function toast(msg) {
  const box = $('#toast');
  box.textContent = msg;
  box.hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => (box.hidden = true), 2200);
}

// ---------------------------------------------------------------- API

let token = localStorage.getItem('lh_token');

// Read and write only while the phone answers. When it is not reachable (PC access off, phone away from the
// Wi-Fi), what was already loaded stays readable and every change is held back until it answers again.
const cache = new Map();
let online = true;

function setOnline(on) {
  if (on === online) return;
  online = on;
  document.body.classList.toggle('readonly', !on);
  offlineBanner.hidden = on;
  showLink();
  lockInputs();
  if (!on) return;
  // Back online: first save what was typed before the connection dropped, then fresh data
  if (Notes.dirty) Notes.flush().then(() => { if (!busy()) render(); });
  else render();
}

/** Text fields cannot be typed into while read-only (the search field still filters what is shown). */
function lockInputs() {
  document.querySelectorAll('main input, main textarea, main select, .backdrop input, .backdrop textarea, .backdrop select').forEach(e => {
    if (e.dataset.offlineOk === undefined) e.readOnly = e.disabled = !online && e.type !== 'search';
  });
}
new MutationObserver(() => { if (!online) lockInputs(); }).observe(document.documentElement, { childList: true, subtree: true });

/** The phone link in the sidebar: the one place that always says whether the phone answers. */
function showLink() {
  const link = $('#link');
  link.classList.toggle('off', !online);
  link.lastElementChild.textContent = online ? t('linked') : t('unlinked');
  link.title = online ? '' : t('offline');
}

// Stays the same element across views, so going offline shows it at once
const offlineBanner = el('div', { class: 'offline', hidden: true }, el('span', { class: 'dot' }), el('span', {}));

/** Puts a view into the main area; fill views (calendar, notes) take the whole window height. */
function show(fill, ...children) {
  offlineBanner.lastElementChild.textContent = t('offlineBanner');
  $('#main').classList.toggle('fill', fill);
  $('#main').replaceChildren(offlineBanner, ...children);
  // Only a change of tab fades in; redraws after a click or a background refresh stay still
  if (show.enter) $('#main').lastElementChild.classList.add('view');
  show.enter = false;
}

/** Page title with month arrows, as calendar and finance share it. */
function monthHead(title, shift, ...extra) {
  return el('div', { class: 'pagehead' },
    el('h2', {}, title),
    el('button', { class: 'icon', onclick: () => shift(-1), 'aria-label': '‹' }, icon('left')),
    el('button', { class: 'icon', onclick: () => shift(1), 'aria-label': '›' }, icon('right')),
    ...extra);
}

async function api(method, path, body) {
  if (method !== 'GET' && !online) {
    toast(t('readOnly'));
    throw new Error('offline');
  }
  let res;
  try {
    res = await fetch('/api/' + path, {
      method,
      headers: Object.assign({ 'Content-Type': 'application/json' }, token ? { Authorization: 'Bearer ' + token } : {}),
      body: body === undefined ? undefined : JSON.stringify(body)
    });
  } catch (e) {
    const wasOnline = online;
    setOnline(false);
    if (method === 'GET' && cache.has(path)) return cache.get(path);
    if (wasOnline || method !== 'GET') toast(t('offline'));
    throw e;
  }
  setOnline(true);
  if (res.status === 401) {
    localStorage.removeItem('lh_token');
    token = null;
    showPairing();
    throw new Error('unauthorized');
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    toast(t('error'));
    throw new Error(data.error || res.status);
  }
  if (method === 'GET') cache.set(path, data);
  return data;
}

// ---------------------------------------------------------------- helpers

const pad = n => String(n).padStart(2, '0');
const isoDate = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const isoTime = d => `${pad(d.getHours())}:${pad(d.getMinutes())}`;
const fromParts = (date, time) => { const [y, m, d] = date.split('-').map(Number); const [h, mi] = (time || '00:00').split(':').map(Number); return new Date(y, m - 1, d, h, mi).getTime(); };
const sameDay = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
const money = cents => new Intl.NumberFormat(lang, { style: 'currency', currency: 'EUR' }).format(cents / 100);
const colorOf = n => (n === null || n === undefined) ? 'var(--accent)' : '#' + ((n >>> 0) & 0xffffff).toString(16).padStart(6, '0');

/** "12,50" / "1.234,56" / "12.50" → cents (positive), or null. */
function parseCents(s) {
  s = String(s || '').replace(/[\s€]/g, '');
  if (!s) return null;
  const lastComma = s.lastIndexOf(','), lastDot = s.lastIndexOf('.');
  if (lastComma > lastDot) s = s.replace(/\./g, '').replace(',', '.');
  else if (lastDot > lastComma && lastComma >= 0) s = s.replace(/,/g, '');
  else if (lastDot >= 0 && s.length - lastDot - 1 === 3 && (s.match(/\./g) || []).length === 1) s = s.replace('.', '');
  const v = Number(s);
  return Number.isFinite(v) && v > 0 ? Math.round(v * 100) : null;
}

function modal(title, body, buttons) {
  const close = () => back.remove();
  const back = el('div', { class: 'backdrop', onclick: e => { if (e.target === back) close(); } },
    el('div', { class: 'modal', role: 'dialog' }, el('h3', {}, title), body, el('div', { class: 'actions' }, buttons(close))));
  back.addEventListener('keydown', e => { if (e.key === 'Escape') close(); });
  document.body.append(back);
  const first = back.querySelector('input, textarea, select');
  if (first) first.focus();
  return close;
}

// ---------------------------------------------------------------- pairing

function showPairing() {
  $('#side').hidden = true;
  const input = el('input', { inputmode: 'numeric', maxlength: '6', autocomplete: 'one-time-code', placeholder: '••••••' });
  const err = el('div', { class: 'error' });
  const go = async () => {
    err.textContent = '';
    try {
      const res = await fetch('/api/pair', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ code: input.value.trim(), name: browserName() }) });
      if (!res.ok) { err.textContent = t('wrongCode'); input.select(); return; }
      token = (await res.json()).token;
      localStorage.setItem('lh_token', token);
      start();
    } catch (e) { err.textContent = t('offline'); }
  };
  input.addEventListener('keydown', e => { if (e.key === 'Enter') go(); });
  $('#main').classList.remove('fill');
  $('#main').replaceChildren(el('div', { class: 'pairwrap' }, el('div', { class: 'pair card view' },
    el('div', { class: 'brand' }, el('img', { src: '/favicon.svg', alt: '' }), el('span', {}, 'LifeHub')),
    el('h2', {}, t('pairTitle')), el('p', {}, t('pairHint')), input, err, el('button', { onclick: go }, t('connect')))));
  input.focus();
}

function browserName() {
  const ua = navigator.userAgent;
  const b = /Edg\//.test(ua) ? 'Edge' : /Firefox\//.test(ua) ? 'Firefox' : /Chrome\//.test(ua) ? 'Chrome' : /Safari\//.test(ua) ? 'Safari' : 'Browser';
  const os = /Windows/.test(ua) ? 'Windows' : /Mac OS/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : '';
  return os ? `${b} · ${os}` : b;
}

// ---------------------------------------------------------------- shell

let tab = 'calendar';
let financeOn = true; // the finance module can be switched off in the app
let render = () => {};
let busy = () => false; // a view with unsaved input says so, then polling does not reload it

function start() {
  $('#side').hidden = false;
  document.querySelectorAll('#tabs button').forEach(b => {
    b.hidden = b.dataset.tab === 'money' && !financeOn;
    b.replaceChildren(icon(b.dataset.tab), el('span', {}, t(b.dataset.tab)));
    b.onclick = () => { location.hash = b.dataset.tab; };
  });
  showLink();
  applyTheme(storedTheme());
  $('#theme').onclick = () => {
    const next = THEMES[(THEMES.indexOf(storedTheme()) + 1) % THEMES.length];
    try { localStorage.setItem('lh_theme', next); } catch (e) { /* private window: applies until reload */ }
    applyTheme(next);
  };
  $('#logout').replaceChildren(icon('logout'), el('span', {}, t('logout')));
  $('#logout').onclick = () => { localStorage.removeItem('lh_token'); token = null; showPairing(); };
  route();
  poll();
}

function route() {
  tab = ['calendar', 'notes', ...(financeOn ? ['money'] : [])].includes(location.hash.slice(1)) ? location.hash.slice(1) : 'calendar';
  document.querySelectorAll('#tabs button').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
  busy = () => false;
  show.enter = true;
  ({ calendar: Calendar, notes: Notes, money: Money })[tab].mount();
}
window.addEventListener('hashchange', () => { if (token) route(); });
// The calendar fits as many events per day as the window height allows
window.addEventListener('resize', () => {
  clearTimeout(route.resize);
  route.resize = setTimeout(() => { if (token && tab === 'calendar' && Calendar.events) Calendar.draw(); }, 150);
});
document.addEventListener('visibilitychange', () => { if (!document.hidden && token) poll(); });

let version = null;
async function poll() {
  clearTimeout(poll.timer);
  if (!token) return;
  // A tab in the background does not ask: the phone can sleep, and PC access ends itself after 30 idle minutes
  if (document.hidden) return;
  try {
    const changed = await api('GET', 'changes');
    applyLook(changed);
    const v = changed.version;
    if (version !== null && v !== version && !document.querySelector('.backdrop') && !busy()) render();
    version = v;
  } catch (e) { /* toast already shown */ }
  poll.timer = setTimeout(poll, 3000);
}

// ---------------------------------------------------------------- calendar

const Calendar = {
  month: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
  selected: new Date(),
  events: [],
  categories: [],

  async mount() {
    render = () => this.load();
    this.categories = await api('GET', 'calendar/categories');
    this.load();
  },

  gridStart() {
    const d = new Date(this.month);
    const shift = (d.getDay() + 6) % 7; // weeks start on Monday
    d.setDate(d.getDate() - shift);
    return d;
  },

  async load() {
    const from = this.gridStart();
    const to = new Date(from); to.setDate(to.getDate() + 41);
    this.events = await api('GET', `calendar/events?from=${isoDate(from)}&to=${isoDate(to)}`);
    this.draw();
  },

  eventsOn(day) {
    const s = new Date(day.getFullYear(), day.getMonth(), day.getDate()).getTime();
    const e = s + 86400000;
    return this.events.filter(ev => ev.start < e && (ev.end || ev.start + 1) > s);
  },

  colorFor(ev) {
    if (ev.color !== null && ev.color !== undefined) return colorOf(ev.color);
    const c = this.categories.find(c => c.id === ev.categoryId);
    return c ? colorOf(c.color) : 'var(--accent)';
  },

  draw() {
    const title = new Intl.DateTimeFormat(lang, { month: 'long', year: 'numeric' }).format(this.month);
    const shiftMonth = n => { this.month = new Date(this.month.getFullYear(), this.month.getMonth() + n, 1); this.load(); };
    const head = monthHead(title, shiftMonth,
      el('button', { class: 'soft sm', onclick: () => { this.month = new Date(new Date().getFullYear(), new Date().getMonth(), 1); this.selected = new Date(); this.load(); } }, t('today')),
      el('div', { class: 'spacer' }),
      el('button', { class: 'needs-phone', onclick: () => this.edit(null, this.selected) }, icon('plus'), t('newEvent')));

    // As many event rows per day as the window height leaves room for (header, page title and padding off)
    const narrow = window.innerWidth <= 860;
    const cellHeight = (window.innerHeight - 24 - 60 - 32 - 24) / 6;
    const fits = narrow ? 0 : Math.max(1, Math.floor((cellHeight - 16 - 26) / 22));

    const grid = el('div', { class: 'grid' });
    const dowFmt = new Intl.DateTimeFormat(lang, { weekday: 'short' });
    for (let i = 0; i < 7; i++) grid.append(el('div', { class: 'dow' }, dowFmt.format(new Date(2024, 0, 1 + i))));
    const today = new Date();
    const d = this.gridStart();
    for (let i = 0; i < 42; i++) {
      const day = new Date(d);
      const evs = this.eventsOn(day);
      // One row of "+n" replaces the last event when they do not all fit
      const shown = evs.length > fits ? Math.max(0, fits - 1) : evs.length;
      const cls = ['day', day.getMonth() !== this.month.getMonth() && 'other', sameDay(day, today) && 'today', sameDay(day, this.selected) && 'selected',
        evs.length && 'has', (day.getDay() === 0 || day.getDay() === 6) && 'weekend'].filter(Boolean).join(' ');
      grid.append(el('div', { class: cls, tabindex: '0', onclick: () => { this.selected = day; this.draw(); }, ondblclick: () => this.edit(null, day),
        onkeydown: e => { if (e.key === 'Enter') { this.selected = day; this.draw(); } } },
        el('span', { class: 'num' }, day.getDate()),
        evs.slice(0, shown).map(ev => el('div', { class: 'chip', style: { '--c': this.colorFor(ev) }, title: ev.title },
          ev.allDay ? null : el('time', {}, isoTime(new Date(ev.start))), el('span', {}, ev.title))),
        evs.length > shown && fits ? el('div', { class: 'more' }, '+' + (evs.length - shown)) : null));
      d.setDate(d.getDate() + 1);
    }

    const list = this.eventsOn(this.selected);
    const side = el('div', { class: 'card daylist' },
      el('div', { class: 'when' }, isoDate(this.selected)),
      el('h3', {}, new Intl.DateTimeFormat(lang, { weekday: 'long', day: 'numeric', month: 'long' }).format(this.selected)),
      list.length ? el('div', { class: 'events' }, list.map(ev => el('div', { class: 'event', style: { '--c': this.colorFor(ev) }, onclick: () => this.edit(ev) },
        el('div', { class: 'bar' }),
        el('div', {},
          el('div', { class: 't' }, ev.title),
          el('div', { class: 'meta' }, icon('clock'), ev.allDay ? t('allDay') : isoTime(new Date(ev.start)) + (ev.end ? '–' + isoTime(new Date(ev.end)) : '')),
          ev.location ? el('div', { class: 'meta' }, icon('place'), ev.location) : null))))
        : el('p', { class: 'empty' }, t('noEvents')),
      el('button', { class: 'ghost sm needs-phone', onclick: () => this.edit(null, this.selected) }, icon('plus'), t('newEvent')));

    show(true, head, el('div', { class: 'cal-layout' }, grid, side));
  },

  edit(ev, day) {
    if (ev && ev.isBirthday) { toast(t('birthdayHint')); return; }
    // Series are edited as a whole, starting from the series' first date
    const start = new Date(ev ? ev.seriesStart : new Date(day.getFullYear(), day.getMonth(), day.getDate(), 9, 0).getTime());
    const end = ev ? (ev.seriesEnd ? new Date(ev.seriesEnd) : null) : new Date(start.getTime() + 3600000);
    const f = {
      title: el('input', { value: ev ? ev.title : '' }),
      allDay: el('input', { type: 'checkbox', checked: ev ? ev.allDay : false }),
      date: el('input', { type: 'date', value: isoDate(start) }),
      start: el('input', { type: 'time', value: isoTime(start) }),
      end: el('input', { type: 'time', value: end ? isoTime(end) : '' }),
      repeat: el('select', {}),
      category: el('select', {}, el('option', { value: '' }, '—'), this.categories.map(c => el('option', { value: c.id }, c.name))),
      reminder: el('select', {}),
      location: el('input', { value: ev && ev.location ? ev.location : '' }),
      description: el('textarea', { rows: '3' })
    };
    f.description.value = ev ? ev.description : '';
    f.category.value = ev && ev.categoryId ? ev.categoryId : '';
    const rules = [['', t('none')], ['FREQ=DAILY', t('daily')], ['FREQ=WEEKLY', t('weekly')], ['FREQ=MONTHLY', t('monthly')], ['FREQ=YEARLY', t('yearly')]];
    if (ev && ev.recurrence && !rules.some(r => r[0] === ev.recurrence)) rules.push([ev.recurrence, t('asInApp')]);
    rules.forEach(([v, l]) => f.repeat.append(el('option', { value: v }, l)));
    f.repeat.value = ev && ev.recurrence ? ev.recurrence : '';
    const reminderOptions = [['', t('none')], ['0', t('atStart')], ['10', t('min10')], ['30', t('min30')], ['60', t('hour1')], ['1440', t('day1')]];
    const original = ev ? ev.reminders : [];
    if (original.length > 1 || (original.length === 1 && !reminderOptions.some(o => o[0] === String(original[0])))) reminderOptions.push(['keep', t('asInApp')]);
    reminderOptions.forEach(([v, l]) => f.reminder.append(el('option', { value: v }, l)));
    f.reminder.value = original.length === 0 ? '' : (original.length === 1 && reminderOptions.some(o => o[0] === String(original[0])) ? String(original[0]) : 'keep');
    const timeRow = el('div', { class: 'row' }, el('div', {}, el('label', {}, t('start')), f.start), el('div', {}, el('label', {}, t('end')), f.end));
    const syncAllDay = () => { timeRow.hidden = f.allDay.checked; };
    f.allDay.addEventListener('change', syncAllDay);
    syncAllDay();

    const body = el('div', {},
      el('label', {}, t('title')), f.title,
      el('label', { class: 'inline' }, f.allDay, t('allDay')),
      el('label', {}, t('date')), f.date, timeRow,
      el('div', { class: 'row' }, el('div', {}, el('label', {}, t('repeat')), f.repeat), el('div', {}, el('label', {}, t('category')), f.category)),
      el('label', {}, t('reminder')), f.reminder,
      el('label', {}, t('location')), f.location,
      el('label', {}, t('description')), f.description,
      ev && ev.recurrence ? el('p', { class: 'hint' }, t('seriesHint')) : null);

    modal(ev ? t('editEvent') : t('newEvent'), body, close => [
      ev ? el('button', { class: 'danger', onclick: async () => { if (!confirm(t('confirmDelete'))) return; await api('DELETE', 'calendar/events/' + ev.id); close(); this.load(); } }, icon('trash'), t('delete')) : null,
      el('div', { class: 'spacer' }),
      el('button', { class: 'ghost', onclick: close }, t('cancel')),
      el('button', { class: 'primary', onclick: async () => {
        if (!f.title.value.trim() || !f.date.value) { f.title.focus(); return; }
        const allDay = f.allDay.checked;
        const s = fromParts(f.date.value, allDay ? '00:00' : f.start.value || '09:00');
        let e = allDay ? null : (f.end.value ? fromParts(f.date.value, f.end.value) : null);
        if (e !== null && e < s) e += 86400000;
        const reminders = f.reminder.value === 'keep' ? original : (f.reminder.value === '' ? [] : [Number(f.reminder.value)]);
        const payload = { title: f.title.value.trim(), description: f.description.value, start: s, end: e, allDay, categoryId: f.category.value ? Number(f.category.value) : null, recurrence: f.repeat.value || null, location: f.location.value, reminders };
        if (ev) await api('PUT', 'calendar/events/' + ev.id, payload); else await api('POST', 'calendar/events', payload);
        close();
        this.selected = new Date(s);
        toast(t('saved'));
        this.load();
      } }, t('save'))
    ]);
  }
};

// ---------------------------------------------------------------- notes

const Notes = {
  notes: [],
  current: null, // note being edited (object) or {id: null} for a new one
  query: '',
  previewMode: false,
  dirty: false,
  timer: null,

  async mount() {
    render = () => this.load();
    busy = () => this.dirty;
    await this.load();
  },

  async load() {
    this.notes = await api('GET', 'notes');
    if (this.current && this.current.id) this.current = this.notes.find(n => n.id === this.current.id) || null;
    this.draw();
  },

  /** Searching lists every match on its own; otherwise main notes come with their sub-notes right below. */
  shown() {
    const q = this.query.toLowerCase();
    if (q) return this.notes.filter(n => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q));
    const ids = new Set(this.notes.map(n => n.id));
    return this.notes.filter(n => !n.parentId || !ids.has(n.parentId))
      .flatMap(n => [n, ...this.subsOf(n.id)]);
  },

  /** Sub-notes of a main note, open ones first. */
  subsOf(id) {
    return this.notes.filter(n => n.parentId === id).sort((a, b) => a.isDone - b.isDone);
  },

  titleOf(id) {
    const n = this.notes.find(x => x.id === id);
    return n ? (n.title || t('untitled')) : t('untitled');
  },

  draw() {
    const listBox = el('div', { class: 'notelist' });
    const search = el('input', { type: 'search', placeholder: t('search'), value: this.query, 'data-offline-ok': '', oninput: e => { this.query = e.target.value; this.drawList(listBox, this.shown()); } });
    this.drawList(listBox, this.shown());
    const head = el('div', { class: 'pagehead' }, el('h2', {}, t('notes')), el('div', { class: 'spacer' }),
      el('button', { class: 'needs-phone', onclick: () => { this.open({ id: null, title: '', content: '', isPinned: false }); } }, icon('plus'), t('newNote')));
    const left = el('div', { class: 'notes-side' }, el('div', { class: 'field' }, icon('search'), search), listBox);
    show(true, head, el('div', { class: 'notes-layout' }, left, el('div', { class: 'card editor' }, this.editor())));
  },

  drawList(box, shown) {
    if (!shown.length) { box.replaceChildren(el('p', { class: 'muted' }, t('noNotes'))); return; }
    const firstLine = n => n.content.split(/\r?\n/).find(l => l.trim()) || '';
    const item = (n, cls, extra) => el('div', {
      class: cls + (n.isDone ? ' done' : '') + (this.current && this.current.id === n.id ? ' active' : ''), tabindex: '0', onclick: () => this.open(n),
      onkeydown: e => { if (e.key === 'Enter') this.open(n); } },
      el('div', { class: 't' }, n.isPinned ? icon('pin') : null, el('span', {}, n.title || t('untitled'))),
      firstLine(n) ? el('div', { class: 's' }, firstLine(n)) : null, ...extra);
    // Searching: every match on its own, with its main note above
    if (this.query) {
      box.replaceChildren(...shown.map(n => {
        const e = item(n, 'noteitem', []);
        if (n.parentId) e.prepend(el('div', { class: 's parent' }, icon('sub'), this.titleOf(n.parentId)));
        return e;
      }));
      return;
    }
    // Otherwise one block per main note, its sub-notes indented on a line below it
    box.replaceChildren(...shown.filter(n => !n.parentId || !this.notes.some(x => x.id === n.parentId)).map(n => {
      const subs = this.subsOf(n.id);
      const count = subs.length ? [el('span', { class: 'count' }, subs.length === 1 ? t('subCountOne') : t('subCount', subs.length))] : [];
      return el('div', { class: 'notegroup' + (subs.length ? ' has-subs' : '') },
        item(n, 'noteitem main', count),
        subs.length ? el('div', { class: 'subs' }, ...subs.map(s => el('div', { class: 'subrow' },
          el('input', { type: 'checkbox', checked: s.isDone, title: t('doneLabel'), onclick: e => e.stopPropagation(),
            onchange: async e => { await api('PUT', 'notes/' + s.id, { isDone: e.target.checked }); this.load(); } }),
          item(s, 'noteitem sub', [])))) : null);
    }));
  },

  open(n) {
    this.flush();
    this.current = Object.assign({}, n);
    this.previewMode = false;
    this.draw();
  },

  editor() {
    const n = this.current;
    if (!n) return el('div', { class: 'pick' }, t('pickNote'));
    const status = el('span', { class: 'saved' });
    const title = el('input', { class: 'title', placeholder: t('title'), value: n.title, oninput: e => { n.title = e.target.value; this.changed(status); } });
    const text = el('textarea', { oninput: e => { n.content = e.target.value; this.changed(status); } });
    text.value = n.content;
    const preview = el('div', { class: 'preview' });
    const showPreview = () => preview.replaceChildren(...renderMarkdown(n.content, (lineIndex, done) => {
      const lines = n.content.split('\n');
      lines[lineIndex] = lines[lineIndex].replace(/\[( |x|X)\]/, done ? '[x]' : '[ ]');
      n.content = lines.join('\n');
      text.value = n.content;
      this.changed(status);
      showPreview();
    }));
    const toggle = el('button', { class: 'ghost sm', onclick: () => { this.previewMode = !this.previewMode; apply(); } });
    const apply = () => {
      text.hidden = this.previewMode;
      preview.hidden = !this.previewMode;
      toggle.replaceChildren(icon(this.previewMode ? 'pencil' : 'eye'), this.previewMode ? t('edit') : t('preview'));
      if (this.previewMode) showPreview();
    };
    apply();
    const pinLabel = () => [icon('pin'), n.isPinned ? t('unpin') : t('pin')];
    const pin = el('button', { class: 'ghost sm', onclick: () => { n.isPinned = !n.isPinned; pin.replaceChildren(...pinLabel()); this.changed(status); } }, ...pinLabel());
    const del = n.id ? el('button', { class: 'danger sm', onclick: async () => {
      if (!confirm(t('confirmDelete'))) return;
      clearTimeout(this.timer); this.dirty = false;
      await api('DELETE', 'notes/' + n.id);
      this.current = null;
      this.load();
    } }, icon('trash'), t('delete')) : null;
    // Sub-note: its main note one click away. Main note: its sub-notes (tick off, open, add one)
    const parent = n.parentId ? el('button', { class: 'ghost sm crumb', onclick: () => { const p = this.notes.find(x => x.id === n.parentId); if (p) this.open(p); } },
      icon('up'), t('subNoteOf', this.titleOf(n.parentId))) : null;
    const subs = n.id && !n.parentId ? el('div', { class: 'subnotes' },
      el('h3', {}, t('subNotes')),
      ...this.subsOf(n.id).map(s => el('div', { class: 'subnote' + (s.isDone ? ' done' : '') },
        el('input', { type: 'checkbox', checked: s.isDone, onchange: async e => { await api('PUT', 'notes/' + s.id, { isDone: e.target.checked }); this.load(); } }),
        el('span', { onclick: () => this.open(s) }, s.title || (s.content.split(/\r?\n/).find(l => l.trim()) || t('untitled'))))),
      el('button', { class: 'ghost sm needs-phone', onclick: () => this.open({ id: null, title: '', content: '', isPinned: false, parentId: n.id }) }, icon('plus'), t('addSubNote'))
    ) : null;
    // A sub-note can be ticked off or stand alone again
    const move = async parentId => {
      await this.flush(status);
      await api('PUT', 'notes/' + n.id, { parentId: parentId });
      n.parentId = parentId;
      await this.load();
    };
    // As in the app: the text of a main note goes into a new sub-note (titled with today's date), the main note is emptied
    const moveContent = async () => {
      const today = new Date().toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
      const subTitle = prompt(t('moveContentTitle'), today);
      if (subTitle === null) return;
      await this.flush(status);
      await api('POST', 'notes', { title: subTitle.trim() || today, content: n.content, isPinned: false, parentId: n.id });
      n.content = '';
      await api('PUT', 'notes/' + n.id, { content: '' });
      await this.load();
      this.draw();
    };
    // Structure actions sit in the same tool row as the rest: tick off or detach a sub-note, split a main note
    const structure = !n.id ? [] : n.parentId ? [
      el('label', { class: 'inline' }, el('input', { type: 'checkbox', checked: n.isDone, onchange: async e => {
        n.isDone = e.target.checked; await api('PUT', 'notes/' + n.id, { isDone: n.isDone }); this.load(); } }), t('doneLabel')),
      el('button', { class: 'ghost sm needs-phone', onclick: () => move(null) }, icon('split'), t('detachSubNote'))
    ] : n.content.trim() ? [el('button', { class: 'ghost sm needs-phone', onclick: moveContent }, icon('sub'), t('moveContent'))] : [];
    const labels = n.labels && n.labels.length ? el('div', { class: 'labels' }, ...n.labels.map(l => el('span', { class: 'label' }, l))) : null;
    return el('div', { style: { display: 'contents' } }, parent, title, labels,
      el('div', { class: 'tools' }, toggle, pin, ...structure, el('div', { class: 'spacer' }), status, del), text, preview, subs);
  },

  changed(status) {
    this.dirty = true;
    status.textContent = t('saving');
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.flush(status), 700);
  },

  async flush(status) {
    clearTimeout(this.timer);
    if (!this.dirty || !this.current) return;
    const n = this.current;
    if (!n.id && !n.title.trim() && !n.content.trim()) return;
    this.dirty = false;
    const payload = { title: n.title, content: n.content, isPinned: n.isPinned, parentId: n.parentId || null };
    try {
      if (n.id) await api('PUT', 'notes/' + n.id, payload);
      else n.id = (await api('POST', 'notes', payload)).id;
    } catch (e) {
      // Phone not reachable: keep the text as unsaved, it goes out on reconnect
      this.dirty = true;
      if (status) status.textContent = t('readOnly');
      return;
    }
    if (status) status.textContent = t('saved');
    this.notes = await api('GET', 'notes');
    const box = document.querySelector('.notelist');
    if (box) this.drawList(box, this.shown());
  }
};
window.addEventListener('beforeunload', () => Notes.flush());

/** Markdown subset as the app renders it: headings, lists, checklists, bold, italic, strike, code. */
function renderMarkdown(src, onToggle) {
  const inline = text => {
    const span = el('span');
    const esc = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    // Only our own tags are added to already escaped text
    span.innerHTML = esc
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/~~([^~]+)~~/g, '<s>$1</s>')
      .replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>');
    return span;
  };
  const out = [];
  let list = null;
  src.split('\n').forEach((line, i) => {
    const check = line.match(/^\s*[-*+]\s+\[( |x|X)\]\s?(.*)$/);
    const bullet = line.match(/^\s*[-*+]\s+(.*)$/);
    const numbered = line.match(/^\s*(\d+)[.)]\s+(.*)$/);
    const heading = line.match(/^(#{1,3})\s+(.*)$/);
    if (check) {
      list = null;
      const done = check[1] !== ' ';
      const box = el('input', { type: 'checkbox', checked: done, onchange: e => onToggle(i, e.target.checked) });
      out.push(el('div', { class: 'check' + (done ? ' done' : '') }, box, inline(check[2])));
    } else if (bullet || numbered) {
      const tag = bullet ? 'ul' : 'ol';
      if (!list || list.tagName.toLowerCase() !== tag) { list = el(tag); out.push(list); }
      list.append(el('li', {}, inline(bullet ? bullet[1] : numbered[2])));
    } else if (heading) {
      list = null;
      out.push(el('h' + heading[1].length, {}, inline(heading[2])));
    } else if (line.trim()) {
      list = null;
      out.push(el('p', {}, inline(line)));
    } else {
      list = null;
    }
  });
  return out;
}

// ---------------------------------------------------------------- money

const Money = {
  month: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
  data: null,
  categories: [],

  async mount() {
    render = () => this.load();
    this.categories = await api('GET', 'money/categories');
    this.load();
  },

  key() { return `${this.month.getFullYear()}-${pad(this.month.getMonth() + 1)}`; },

  async load() {
    this.data = await api('GET', 'money/month?month=' + this.key());
    this.draw();
  },

  draw() {
    const d = this.data;
    const cat = id => this.categories.find(c => c.id === id);
    const shift = n => { this.month = new Date(this.month.getFullYear(), this.month.getMonth() + n, 1); this.load(); };
    const head = monthHead(new Intl.DateTimeFormat(lang, { month: 'long', year: 'numeric' }).format(this.month), shift);
    // The balance leads; income and expenses are the two figures it comes from
    const stat = (label, value, cls, sub) => el('div', { class: 'card stat ' + cls }, el('div', { class: 'k' }, label), el('div', { class: 'v' }, value), sub ? el('div', { class: 'sub' }, sub) : null);
    const cards = el('div', { class: 'cards' },
      stat(t('left'), money(d.rest), 'lead', d.expected ? t('expected', money(Math.abs(d.expected))) : null),
      stat(t('income'), money(d.income), ''),
      stat(t('expenses'), money(Math.abs(d.expense)), ''));

    const total = Math.max(1, Math.abs(d.expense));
    const breakdown = el('div', { class: 'card' }, el('h3', {}, t('byCategory')),
      d.byCategory.length ? d.byCategory.map(b => {
        const c = cat(b.categoryId);
        const budget = c && c.budget ? c.budget : null;
        const share = budget ? Math.min(1, b.cents / budget) : b.cents / total;
        // With a budget the bar says how close it is; without one it shows the category's colour
        const color = budget ? (b.cents >= budget ? 'var(--neg)' : b.cents >= budget * 0.8 ? 'var(--warn)' : 'var(--accent)') : (c ? colorOf(c.color) : 'var(--accent)');
        return el('div', { class: 'cat' },
          el('div', { class: 'top' }, el('span', {}, c ? c.name : '—'), el('span', {}, budget ? t('budgetOf', money(b.cents), money(budget)) : money(b.cents))),
          el('div', { class: 'meter' }, el('div', { style: { width: (share * 100).toFixed(1) + '%', '--c': color } })));
      }) : el('p', { class: 'empty' }, t('noTx')));

    // Add form
    const kind = el('select', {}, el('option', { value: 'EXPENSE' }, t('expense')), el('option', { value: 'INCOME' }, t('incomeOne')));
    const amount = el('input', { placeholder: '12,50', inputmode: 'decimal' });
    const title = el('input', { placeholder: t('title') });
    const category = el('select', {});
    const fillCategories = () => category.replaceChildren(el('option', { value: '' }, t('auto')), ...this.categories.filter(c => c.kind === kind.value).map(c => el('option', { value: c.id }, c.name)));
    kind.addEventListener('change', fillCategories);
    fillCategories();
    const date = el('input', { type: 'date', value: isoDate(new Date()) });
    const add = async () => {
      const cents = parseCents(amount.value);
      if (!cents) { amount.focus(); return; }
      const [y, m, dd] = date.value.split('-').map(Number);
      await api('POST', 'money/transactions', {
        title: title.value.trim(), amount: kind.value === 'INCOME' ? cents : -cents,
        epochDay: Math.round(Date.UTC(y, m - 1, dd) / 86400000), categoryId: category.value ? Number(category.value) : null
      });
      toast(t('saved'));
      this.load();
    };
    amount.addEventListener('keydown', e => { if (e.key === 'Enter') add(); });
    // Typing in the form: background refreshes wait
    busy = () => !!(amount.value || title.value) || form.contains(document.activeElement);
    const form = el('div', { class: 'card form needs-phone' }, el('h3', {}, t('add')),
      el('div', { class: 'row' }, kind, amount), title, el('div', { class: 'row' }, category, date),
      el('button', { onclick: add }, icon('plus'), t('add')));

    const rows = d.transactions.map(tx => {
      const c = cat(tx.categoryId);
      return el('tr', {},
        el('td', { class: 'date' }, new Intl.DateTimeFormat(lang, { day: '2-digit', month: '2-digit' }).format(new Date(tx.date + 'T00:00'))),
        el('td', {}, tx.title || (c ? c.name : '')),
        el('td', { class: 'cat-name' }, c ? c.name : ''),
        el('td', { class: 'amt ' + (tx.amount >= 0 ? 'pos' : '') }, (tx.amount >= 0 ? '+' : '−') + money(Math.abs(tx.amount))),
        el('td', { class: 'del' }, el('button', { class: 'danger icon sm', title: t('delete'), 'aria-label': t('delete'), onclick: async () => { if (!confirm(t('confirmDelete'))) return; await api('DELETE', 'money/transactions/' + tx.id); this.load(); } }, icon('trash'))));
    });
    const table = el('div', { class: 'card' }, el('h3', {}, t('transactions')), rows.length
      ? el('table', {}, el('thead', {}, el('tr', {}, el('th', {}, t('date')), el('th', {}, t('title')), el('th', {}, t('category')), el('th', { class: 'amt' }, t('amount')), el('th'))), el('tbody', {}, rows))
      : el('p', { class: 'empty' }, t('noTx')));

    show(false, head, cards, el('div', { class: 'money-layout' }, table, el('div', { class: 'money-side' }, breakdown, form)));
  }
};

// ---------------------------------------------------------------- boot

(async () => {
  try {
    const info = await (await fetch('/api/info')).json();
    if (T[info.lang]) lang = info.lang;
    financeOn = info.finance !== false;
    applyLook(info);
  } catch (e) { /* default language */ }
  document.documentElement.lang = lang;
  document.title = 'LifeHub';
  if (token) start(); else showPairing();
})();
