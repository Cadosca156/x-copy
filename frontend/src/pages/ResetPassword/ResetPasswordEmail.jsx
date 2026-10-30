import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ResetPasswordEmail.css';
import {sendemail} from "../../api/passwordResetApi.js";
import Toast from '../../components/Toast.jsx';

export default function ResetPasswordEmail() {
  const navigate = useNavigate();
  const onBack = () => navigate('/login');
  const [email, setEmail] = useState('');
  const [toast, setToast] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;

    setIsSubmitting(true);

    const result = await sendemail(email);
    setToast(result.success
      ? { type: 'success', message: 'Якщо акаунт із таким email існує, ми надішлемо лист для зміни пароля.' }
      : { type: 'error', message: 'Не вдалося надіслати лист для зміни пароля.' });
    setIsSubmitting(false);
  };

  return (
    <main className="reset-email-page">
      <section className="reset-email-card" aria-labelledby="reset-email-title">
        <span className="reset-email-logo" aria-hidden="true">𝕏</span>
        <div className="reset-email-content">
          <p className="reset-email-eyebrow">ВІДНОВЛЕННЯ ДОСТУПУ</p>
          <h1 id="reset-email-title">Знайдіть свій обліковий запис</h1>
          <p className="reset-email-description">Введіть email, пов’язаний з вашим акаунтом. Ми допоможемо вам змінити пароль.</p>
          <form onSubmit={handleSubmit}>
            <label htmlFor="reset-email">Email</label>
            <input id="reset-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com" autoComplete="email" required />
            <button type="submit">Продовжити</button>
          </form>
          <button type="button" className="reset-email-link" onClick={onBack}>Повернутися до входу</button>
        </div>
      </section>
      <Toast {...toast} onClose={() => setToast(null)} />
    </main>
  );
}
