import React, { useEffect, useRef, useState } from 'react';
// Tipos: sucesso (green/cinza escuro), erro (vermelho/cinza claro)

const Notifications = () => {
  const [items, setItems] = useState([]);
  const timers = useRef(new Map());

  useEffect(() => {
    const onNotify = (e) => {
      const { id, type, message, duration = 3000 } = e.detail || {};
      if (!message) return;
      setItems((prev) => [...prev, { id, type, message, createdAt: Date.now(), duration }]);
      const t = setTimeout(() => {
        setItems((prev) => prev.filter((i) => i.id !== id));
        timers.current.delete(id);
      }, duration);
      timers.current.set(id, t);
    };
    window.addEventListener('jt:notify', onNotify);
    const timersMap = timers.current;
    return () => {
      window.removeEventListener('jt:notify', onNotify);
      timersMap.forEach((t) => clearTimeout(t));
      timersMap.clear();
    };
  }, []);

  const close = (id) => {
    const t = timers.current.get(id);
    if (t) clearTimeout(t);
    timers.current.delete(id);
    setItems((prev) => prev.filter((i) => i.id !== id));
  };

  if (!items.length) return null;

  return (
    <div className="jt-notify-container" aria-live="polite" aria-atomic="false">
      {items.map((n) => (
        <div
          key={n.id}
          className={`jt-notify jt-notify-${n.type}`}
          role={n.type === 'error' ? 'alert' : 'status'}
        >
          <span className="jt-notify-text">{n.message}</span>
          <button className="jt-notify-close" onClick={() => close(n.id)} aria-label="Fechar aviso">×</button>
        </div>
      ))}
    </div>
  );
};

export default Notifications;
