'use strict';
// LifeHub in the browser: talks to the phone's local server (/api). No external resources.

const T = {
  en: {
    calendar: 'Calendar', notes: 'Notes', money: 'Finance', logout: 'Disconnect',
    pairTitle: 'Connect to LifeHub', pairHint: 'Enter the 6-digit code shown in LifeHub on your phone (Menu → PC access).',
    connect: 'Connect', wrongCode: 'Wrong code. Check the code on your phone.', offline: 'Phone not reachable. Is PC access still on?',
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
    connect: 'Verbinden', wrongCode: 'Falscher Code. Prüfe den Code auf dem Handy.', offline: 'Handy nicht erreichbar. Ist der PC-Zugriff noch an?',
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
    noTx: 'Keine Buchungen in diesem Monat', budgetOf: '{0} von {1}', error: 'Etwas ist schiefgelaufen'
  },
  tr: {
    calendar: 'Takvim', notes: 'Notlar', money: 'Finans', logout: 'Bağlantıyı kes',
    pairTitle: "LifeHub'a bağlan", pairHint: 'Telefonundaki LifeHub\'ın gösterdiği 6 haneli kodu gir (Menü → PC erişimi).',
    connect: 'Bağlan', wrongCode: 'Yanlış kod. Telefondaki kodu kontrol et.', offline: 'Telefona ulaşılamıyor. PC erişimi hâlâ açık mı?',
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
    connect: 'Conectar', wrongCode: 'Código incorrecto. Revisa el código en el móvil.', offline: 'Móvil no accesible. ¿Sigue activo el acceso desde PC?',
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
    connect: '连接', wrongCode: '代码错误。请核对手机上的代码。', offline: '无法连接手机。电脑访问是否仍开启？',
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
const t = (k, ...a) => ((T[lang] || T.en)[k] || T.en[k] || k).replace(/\{(\d)\}/g, (_, i) => a[i]);
const $ = (sel, root = document) => root.querySelector(sel);

/** Small DOM builder: el('div', {class: 'x', onclick: f}, child, 'text'). Text is always set as text, never as HTML. */
function el(tag, attrs = {}, ...children) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === undefined || v === null || v === false) continue;
    if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else if (k === 'class') e.className = v;
    else if (k === 'style') Object.assign(e.style, v);
    else if (k === 'value') e.value = v;
    else if (k === 'checked') e.checked = !!v;
    else e.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) if (c !== null && c !== undefined && c !== false) e.append(c instanceof Node ? c : String(c));
  return e;
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

async function api(method, path, body) {
  let res;
  try {
    res = await fetch('/api/' + path, {
      method,
      headers: Object.assign({ 'Content-Type': 'application/json' }, token ? { Authorization: 'Bearer ' + token } : {}),
      body: body === undefined ? undefined : JSON.stringify(body)
    });
  } catch (e) {
    toast(t('offline'));
    throw e;
  }
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
  document.body.append(back);
  const first = back.querySelector('input, textarea, select');
  if (first) first.focus();
  return close;
}

// ---------------------------------------------------------------- pairing

function showPairing() {
  $('#top').hidden = true;
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
  $('#main').replaceChildren(el('div', { class: 'pair card' }, el('h2', {}, t('pairTitle')), el('p', { class: 'muted' }, t('pairHint')), input, err, el('button', { onclick: go }, t('connect'))));
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
let render = () => {};
let busy = () => false; // a view with unsaved input says so, then polling does not reload it

function start() {
  $('#top').hidden = false;
  document.querySelectorAll('#tabs button').forEach(b => {
    b.textContent = t(b.dataset.tab);
    b.onclick = () => { location.hash = b.dataset.tab; };
  });
  $('#logout').textContent = t('logout');
  $('#logout').onclick = () => { localStorage.removeItem('lh_token'); token = null; showPairing(); };
  route();
  poll();
}

function route() {
  tab = ['calendar', 'notes', 'money'].includes(location.hash.slice(1)) ? location.hash.slice(1) : 'calendar';
  document.querySelectorAll('#tabs button').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
  busy = () => false;
  ({ calendar: Calendar, notes: Notes, money: Money })[tab].mount();
}
window.addEventListener('hashchange', () => { if (token) route(); });

let version = null;
async function poll() {
  clearTimeout(poll.timer);
  if (!token) return;
  try {
    const v = (await api('GET', 'changes')).version;
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
    const toolbar = el('div', { class: 'toolbar' },
      el('button', { class: 'icon', onclick: () => shiftMonth(-1), 'aria-label': '‹' }, '‹'),
      el('h2', {}, title),
      el('button', { class: 'icon', onclick: () => shiftMonth(1), 'aria-label': '›' }, '›'),
      el('button', { class: 'ghost', onclick: () => { this.month = new Date(new Date().getFullYear(), new Date().getMonth(), 1); this.selected = new Date(); this.load(); } }, t('today')),
      el('div', { class: 'spacer' }),
      el('button', { onclick: () => this.edit(null, this.selected) }, '+ ' + t('newEvent')));

    const grid = el('div', { class: 'grid' });
    const dowFmt = new Intl.DateTimeFormat(lang, { weekday: 'short' });
    for (let i = 0; i < 7; i++) grid.append(el('div', { class: 'dow' }, dowFmt.format(new Date(2024, 0, 1 + i))));
    const today = new Date();
    const d = this.gridStart();
    for (let i = 0; i < 42; i++) {
      const day = new Date(d);
      const evs = this.eventsOn(day);
      const cls = ['day', day.getMonth() !== this.month.getMonth() && 'other', sameDay(day, today) && 'today', sameDay(day, this.selected) && 'selected', evs.length && 'has'].filter(Boolean).join(' ');
      grid.append(el('div', { class: cls, onclick: () => { this.selected = day; this.draw(); }, ondblclick: () => this.edit(null, day) },
        el('span', { class: 'num' }, day.getDate()),
        evs.slice(0, 3).map(ev => el('div', { class: 'chip', style: { background: this.colorFor(ev) }, title: ev.title }, (ev.allDay ? '' : isoTime(new Date(ev.start)) + ' ') + ev.title)),
        evs.length > 3 ? el('div', { class: 'more' }, '+' + (evs.length - 3)) : null));
      d.setDate(d.getDate() + 1);
    }

    const list = this.eventsOn(this.selected);
    const side = el('div', { class: 'card daylist' },
      el('h3', {}, new Intl.DateTimeFormat(lang, { weekday: 'long', day: 'numeric', month: 'long' }).format(this.selected)),
      list.length ? list.map(ev => el('div', { class: 'event', onclick: () => this.edit(ev) },
        el('div', { class: 'dot', style: { background: this.colorFor(ev) } }),
        el('div', {}, el('div', {}, ev.title), el('div', { class: 'muted' }, ev.allDay ? t('allDay') : isoTime(new Date(ev.start)) + (ev.end ? '–' + isoTime(new Date(ev.end)) : '')), ev.location ? el('div', { class: 'muted' }, ev.location) : null)))
        : el('p', { class: 'muted' }, t('noEvents')),
      el('button', { class: 'ghost', onclick: () => this.edit(null, this.selected) }, '+ ' + t('newEvent')));

    $('#main').replaceChildren(toolbar, el('div', { class: 'cal-layout' }, grid, side));
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
      ev && ev.recurrence ? el('p', { class: 'muted' }, t('seriesHint')) : null);

    modal(ev ? t('editEvent') : t('newEvent'), body, close => [
      ev ? el('button', { class: 'danger', onclick: async () => { if (!confirm(t('confirmDelete'))) return; await api('DELETE', 'calendar/events/' + ev.id); close(); this.load(); } }, t('delete')) : null,
      el('div', { class: 'spacer' }),
      el('button', { class: 'ghost', onclick: close }, t('cancel')),
      el('button', { onclick: async () => {
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

  shown() {
    const q = this.query.toLowerCase();
    return this.notes.filter(n => !q || n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q));
  },

  draw() {
    const listBox = el('div', { class: 'notelist' });
    const search = el('input', { placeholder: t('search'), value: this.query, oninput: e => { this.query = e.target.value; this.drawList(listBox, this.shown()); } });
    this.drawList(listBox, this.shown());
    const left = el('div', { class: 'card' }, el('div', { class: 'row' }, search), el('p'), el('button', { onclick: () => { this.open({ id: null, title: '', content: '', isPinned: false }); } }, '+ ' + t('newNote')), el('p'), listBox);
    $('#main').replaceChildren(el('div', { class: 'notes-layout' }, left, el('div', { class: 'card editor' }, this.editor())));
  },

  drawList(box, shown) {
    box.replaceChildren(...(shown.length ? shown.map(n => el('div', { class: 'noteitem' + (this.current && this.current.id === n.id ? ' active' : ''), onclick: () => this.open(n) },
      el('div', { class: 't' }, (n.isPinned ? '★ ' : '') + (n.title || t('untitled'))),
      el('div', { class: 's' }, n.content.split('\n').find(l => l.trim()) || ''))) : [el('p', { class: 'muted' }, t('noNotes'))]));
  },

  open(n) {
    this.flush();
    this.current = Object.assign({}, n);
    this.previewMode = false;
    this.draw();
  },

  editor() {
    const n = this.current;
    if (!n) return el('p', { class: 'muted' }, t('pickNote'));
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
    const toggle = el('button', { class: 'icon', onclick: () => { this.previewMode = !this.previewMode; apply(); } });
    const apply = () => {
      text.hidden = this.previewMode;
      preview.hidden = !this.previewMode;
      toggle.textContent = this.previewMode ? t('edit') : t('preview');
      if (this.previewMode) showPreview();
    };
    apply();
    const pin = el('button', { class: 'icon', onclick: () => { n.isPinned = !n.isPinned; pin.textContent = n.isPinned ? '★ ' + t('unpin') : '☆ ' + t('pin'); this.changed(status); } }, n.isPinned ? '★ ' + t('unpin') : '☆ ' + t('pin'));
    const del = n.id ? el('button', { class: 'danger', onclick: async () => {
      if (!confirm(t('confirmDelete'))) return;
      clearTimeout(this.timer); this.dirty = false;
      await api('DELETE', 'notes/' + n.id);
      this.current = null;
      this.load();
    } }, t('delete')) : null;
    return el('div', {}, el('div', { class: 'row' }, title), el('div', { class: 'toolbar' }, toggle, pin, el('div', { class: 'spacer' }), status, del), text, preview);
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
    const payload = { title: n.title, content: n.content, isPinned: n.isPinned };
    if (n.id) await api('PUT', 'notes/' + n.id, payload);
    else n.id = (await api('POST', 'notes', payload)).id;
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
    const toolbar = el('div', { class: 'toolbar' },
      el('button', { class: 'icon', onclick: () => shift(-1) }, '‹'),
      el('h2', {}, new Intl.DateTimeFormat(lang, { month: 'long', year: 'numeric' }).format(this.month)),
      el('button', { class: 'icon', onclick: () => shift(1) }, '›'));
    const card = (label, value, cls) => el('div', { class: 'card' }, el('div', { class: 'muted' }, label), el('div', { class: 'v ' + cls }, value));
    const cards = el('div', { class: 'cards' },
      card(t('income'), money(d.income), 'pos'),
      card(t('expenses'), money(Math.abs(d.expense)), 'neg'),
      card(t('left'), money(d.rest), d.rest >= 0 ? 'pos' : 'neg'));

    const total = Math.max(1, Math.abs(d.expense));
    const breakdown = el('div', { class: 'card' }, el('h3', {}, t('byCategory')),
      d.byCategory.length ? d.byCategory.map(b => {
        const c = cat(b.categoryId);
        const budget = c && c.budget ? c.budget : null;
        const share = budget ? Math.min(1, b.cents / budget) : b.cents / total;
        const color = budget ? (b.cents >= budget ? 'var(--danger)' : b.cents >= budget * 0.8 ? 'var(--warn)' : 'var(--ok)') : (c ? colorOf(c.color) : 'var(--accent)');
        return el('div', {},
          el('div', { class: 'row' }, el('span', {}, c ? c.name : '—'), el('span', { class: 'muted', style: { textAlign: 'right' } }, budget ? t('budgetOf', money(b.cents), money(budget)) : money(b.cents))),
          el('div', { class: 'bar' }, el('div', { style: { width: (share * 100).toFixed(1) + '%', background: color } })));
      }) : el('p', { class: 'muted' }, t('noTx')),
      d.expected ? el('p', { class: 'muted' }, t('expected', money(Math.abs(d.expected)))) : null);

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
    const form = el('div', { class: 'card' }, el('h3', {}, t('add')),
      el('div', { class: 'row' }, kind, amount), el('p'), title, el('p'), el('div', { class: 'row' }, category, date), el('p'),
      el('button', { onclick: add }, t('add')));

    const rows = d.transactions.map(tx => {
      const c = cat(tx.categoryId);
      return el('tr', {},
        el('td', { class: 'muted' }, new Intl.DateTimeFormat(lang, { day: '2-digit', month: '2-digit' }).format(new Date(tx.date + 'T00:00'))),
        el('td', {}, tx.title || (c ? c.name : '')),
        el('td', { class: 'muted' }, c ? c.name : ''),
        el('td', { class: 'amt ' + (tx.amount >= 0 ? 'pos' : '') }, (tx.amount >= 0 ? '+' : '−') + money(Math.abs(tx.amount))),
        el('td', {}, el('button', { class: 'danger', title: t('delete'), onclick: async () => { if (!confirm(t('confirmDelete'))) return; await api('DELETE', 'money/transactions/' + tx.id); this.load(); } }, '×')));
    });
    const table = el('div', { class: 'card' }, el('h3', {}, t('transactions')), rows.length ? el('table', {}, el('tbody', {}, rows)) : el('p', { class: 'muted' }, t('noTx')));

    $('#main').replaceChildren(toolbar, cards, el('div', { class: 'money-layout' }, el('div', {}, breakdown, el('p'), form), table));
  }
};

// ---------------------------------------------------------------- boot

(async () => {
  try {
    const info = await (await fetch('/api/info')).json();
    if (T[info.lang]) lang = info.lang;
  } catch (e) { /* default language */ }
  document.documentElement.lang = lang;
  document.title = 'LifeHub';
  if (token) start(); else showPairing();
})();
