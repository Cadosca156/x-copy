import { useEffect } from 'react';
import './Toast.css';

export default function Toast({ message, type = 'success', onClose }) {
  useEffect(() => {
    const timeoutId = window.setTimeout(onClose, 5000);

    return () => window.clearTimeout(timeoutId);
  }, [message, onClose]);

  if (!message) {
    return null;
  }

  return (
    <div className={`toast toast--${type}`} role="status" aria-live="polite">
      <span>{message}</span>
      <button type="button" className="toast__close" onClick={onClose} aria-label="Закрити повідомлення">×</button>
    </div>
  );
}
