import {useState} from 'react';
import './ResetPassword.css';
import {useNavigate, useSearchParams} from "react-router-dom";
import {resetpassword} from "../../api/passwordResetApi.js";
import Toast from '../../components/Toast.jsx';
import { validateInitialPassword } from '../../api/accountSetup.js';

export default function ResetPassword() {
  const navigate = useNavigate();
  const onBack = () => navigate('/login');
  const [newPassword, setNewPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [toast, setToast] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState('');
  const passwordsMatch = newPassword === confirmation;
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const missingToken = !token?.trim();


  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting || isComplete || missingToken) return;

    const validationError = validateInitialPassword(newPassword, confirmation);
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    setError('');

    const result = await resetpassword(token, newPassword);
    if (result.success) {
      setIsComplete(true);
      setNewPassword('');
      setConfirmation('');
      setToast({type: 'success', message: 'Пароль успішно змінено. Увійдіть із новим паролем.'});
    } else {
      setError(result.message || 'Не вдалося змінити пароль. Пароль не змінено.');
    }
    setIsSubmitting(false);
  };

  return (
      <main className="reset-password-page">
        <section className="reset-password-card" aria-labelledby="reset-password-title">
          <span className="reset-password-logo" aria-hidden="true">𝕏</span>
          <div className="reset-password-content">
            <p className="reset-password-eyebrow">НОВИЙ ПАРОЛЬ</p>
            <h1 id="reset-password-title">Створіть новий пароль</h1>
            <p className="reset-password-description">Пароль має містити щонайменше 8 символів. Використайте унікальну
              комбінацію для захисту акаунта.</p>
            {missingToken && <p className="reset-password-error" role="alert">У посиланні немає токена. Запросіть новий лист для зміни пароля.</p>}
            {error && <p className="reset-password-error" role="alert">{error}</p>}
            {isComplete && <p role="status">Пароль змінено. Тепер можна увійти з новим паролем.</p>}
            {!isComplete && <form onSubmit={handleSubmit}>
              <label htmlFor="new-password">Новий пароль</label>
              <input id="new-password" type="password" value={newPassword}
                     onChange={(event) => setNewPassword(event.target.value)} placeholder="Введіть новий пароль"
                     autoComplete="new-password" minLength="8" required disabled={missingToken || isSubmitting}/>
              <label htmlFor="confirm-password">Повторіть пароль</label>
              <input id="confirm-password" type="password" value={confirmation}
                     onChange={(event) => setConfirmation(event.target.value)} placeholder="Повторіть новий пароль"
                     autoComplete="new-password" minLength="8" required disabled={missingToken || isSubmitting} aria-describedby="password-match-message"/>
              {confirmation && !passwordsMatch &&
                  <p id="password-match-message" className="reset-password-error">Паролі мають збігатися.</p>}
              <button type="submit" disabled={!passwordsMatch || missingToken || isSubmitting}>{isSubmitting ? 'Зберігаємо…' : 'Змінити пароль'}</button>
            </form>}
            {!isComplete && <button type="button" className="reset-password-link" onClick={() => navigate('/reset-password-email')}>Запросити новий лист</button>}
            <button type="button" className="reset-password-link" onClick={onBack}>{isComplete ? 'Перейти до входу' : 'Назад'}</button>
          </div>
        </section>
        <Toast {...toast} onClose={() => setToast(null)}/>
      </main>
  );
}
