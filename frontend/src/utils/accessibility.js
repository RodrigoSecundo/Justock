const ACCESS_KEY = 'jt-accessibility';
const defaultPrefs = {
  altoContraste: false,
  dislexico: false,
  focoVisivel: true,
  feedbackSonoro: false,
  toggleLeitor: false,
};

export function getAccessibilityPrefs() {
  if (typeof window === 'undefined') return { ...defaultPrefs };
  try {
    const raw = window.localStorage.getItem(ACCESS_KEY);
    if (!raw) return { ...defaultPrefs };
    const parsed = JSON.parse(raw);
    return { ...defaultPrefs, ...parsed };
  } catch {
    return { ...defaultPrefs };
  }
}

export function setAccessibilityPrefs(prefs) {
  try { window.localStorage.setItem(ACCESS_KEY, JSON.stringify(prefs)); } catch { /* para aqui */ }
  applyAccessibility(prefs);
}

export function playFeedback(type = 'success') {
  const prefs = getAccessibilityPrefs();
  if (!prefs.feedbackSonoro) return;
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const o = ctx.createOscillator();
    const g = ctx.createGain();
    o.type = 'sine';
    o.frequency.value = type === 'error' ? 220 : 880;
    o.connect(g);
    g.connect(ctx.destination);
    g.gain.setValueAtTime(0.0001, ctx.currentTime);
    g.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + 0.02);
    g.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.25);
    o.start();
    o.stop(ctx.currentTime + 0.3);
  } catch { /* para aqui */ }
}

export function applyAccessibility(p) {
  if (typeof document === 'undefined') return;
  const prefs = p || getAccessibilityPrefs();
  const body = document.body;
  if (!body) return;

  body.classList.toggle('high-contrast', !!prefs.altoContraste);
  body.classList.toggle('dyslexic-font', !!prefs.dislexico);
  body.classList.toggle('focus-visible-mode', !!prefs.focoVisivel);
  body.classList.toggle('sound-feedback', !!prefs.feedbackSonoro);
  body.classList.toggle('sr-optimized', !!prefs.toggleLeitor);

  try {
    const evt = new CustomEvent('jt:accessibility-updated', { detail: { ...prefs } });
    window.dispatchEvent(evt);
  } catch { /* para aqui */ }
}

export function initAccessibility() {
  applyAccessibility();
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (e) => {
      if (e.key === ACCESS_KEY) applyAccessibility();
    });
  }
}

export default {
  getAccessibilityPrefs,
  setAccessibilityPrefs,
  applyAccessibility,
  initAccessibility,
  playFeedback,
};
