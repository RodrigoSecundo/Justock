import { playFeedback } from './accessibility.js';

let counter = 0;

export function notify(type, message, options = {}) {
  const id = ++counter;
  const duration = options.duration ?? (type === 'success' ? 3000 : 4000);
  try {
    playFeedback(type === 'success' ? 'success' : 'error');
  } catch { /* para aqui */ }
  try {
    const evt = new CustomEvent('jt:notify', {
      detail: { id, type, message, duration }
    });
    window.dispatchEvent(evt);
  } catch {
    console[type === 'success' ? 'log' : 'error'](message);
  }
  return id;
}

export const notifySuccess = (msg, opts) => notify('success', msg, opts);
export const notifyError = (msg, opts) => notify('error', msg, opts);

export default { notify, notifySuccess, notifyError };
