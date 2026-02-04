const FONT_KEY = 'jt-font';
const SIDEBAR_KEY = 'jt-sidebar';
const THEME_KEY = 'jt-theme';

export function getFontPref() {
  const v = typeof window !== 'undefined' ? window.localStorage.getItem(FONT_KEY) : null;
  return v === 'pequena' || v === 'media' || v === 'grande' ? v : 'media';
}

export function getSidebarPref() {
  const v = typeof window !== 'undefined' ? window.localStorage.getItem(SIDEBAR_KEY) : null;
  return v === 'detalhada' || v === 'compacta' || v === 'mista' ? v : 'mista';
}

export function getThemePref() {
  if (typeof window === 'undefined') return 'light';
  const v = window.localStorage.getItem(THEME_KEY);
  if (v === 'light' || v === 'dark') return v;
  try {
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    return prefersDark ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

export function setFontPref(v) {
  try { window.localStorage.setItem(FONT_KEY, v); } catch { /* para aqui */ }
  applyAppearance();
}

export function setSidebarPref(v) {
  try { window.localStorage.setItem(SIDEBAR_KEY, v); } catch { /* para aqui */ }
  applyAppearance();
}

export function setThemePref(v) {
  try { window.localStorage.setItem(THEME_KEY, v); } catch { /* para aqui */ }
  applyAppearance();
}

export function applyAppearance() {
  if (typeof document === 'undefined') return;
  const body = document.body;
  const html = document.documentElement;
  if (!body) return;

  body.classList.remove('painel-fonte-pequena', 'painel-fonte-media', 'painel-fonte-grande');
  if (html) html.classList.remove('painel-fonte-pequena', 'painel-fonte-media', 'painel-fonte-grande');
  const font = getFontPref();
  const fontClass = font === 'pequena' ? 'painel-fonte-pequena' : font === 'grande' ? 'painel-fonte-grande' : 'painel-fonte-media';
  body.classList.add(fontClass);
  if (html) html.classList.add(fontClass);

  const sidebar = getSidebarPref();
  body.classList.toggle('sidebar-detalhada', sidebar === 'detalhada');
  body.classList.toggle('sidebar-mista', sidebar === 'mista');

  const theme = getThemePref();
  if (body.classList.contains('dashboard-scope')) {
    body.setAttribute('data-theme', theme);
  } else {
    body.removeAttribute('data-theme');
  }

  try {
    if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
      const evt = new CustomEvent('jt:appearance-updated', { detail: { font, sidebar, theme } });
      window.dispatchEvent(evt);
    }
  } catch { /* para aqui */ }
}

export function initAppearance() {
  applyAppearance();
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (e) => {
      if (e.key === FONT_KEY || e.key === SIDEBAR_KEY || e.key === THEME_KEY) {
        applyAppearance();
      }
    });
  }
}

export default {
  getFontPref,
  getSidebarPref,
  getThemePref,
  setFontPref,
  setSidebarPref,
  setThemePref,
  applyAppearance,
  initAppearance,
};
