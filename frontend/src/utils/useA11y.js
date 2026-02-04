import { useEffect, useState } from 'react';
import { getAccessibilityPrefs } from './accessibility';

export function useSrOptimized() {
  const [srOpt, setSrOpt] = useState(() => getAccessibilityPrefs().toggleLeitor === true);
  useEffect(() => {
    const onAcc = (e) => {
      const v = e?.detail?.toggleLeitor;
      if (typeof v === 'boolean') setSrOpt(v);
      else setSrOpt(getAccessibilityPrefs().toggleLeitor === true);
    };
    window.addEventListener('jt:accessibility-updated', onAcc);
    onAcc({ detail: getAccessibilityPrefs() });
    return () => window.removeEventListener('jt:accessibility-updated', onAcc);
  }, []);
  return srOpt;
}

export function srProps(enabled, props) {
  return enabled ? props : {};
}
