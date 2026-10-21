import { useEffect } from 'react';
import './Toast.css';

export default function Toast({ message, type = 'success', onClose }) {
    useEffect(() => {
        if (!message || !onClose) return;
        const timer = setTimeout(onClose, 5000);
        return () => clearTimeout(timer);
    }, [message, onClose]);
    if (!message) return null;
    return (
        <div className={`toast toast--${type}`} role={type === 'error' ? 'alert' : 'status'}>
            <span>{message}</span>
            <button type="button" aria-label="Закрити повідомлення" onClick={onClose}>×</button>
        </div>
    );
}
