import {useState} from 'react';
import './ResetPassword.css';
import {useNavigate, useSearchParams} from "react-router-dom";
import {resetpassword} from "../../api/passwordResetApi.js";
import Toast from '../../components/Toast.jsx';

export default function ResetPassword() {
  const navigate = useNavigate();
  const onBack = () => navigate('/login');
  const [newPassword, setNewPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [toast, setToast] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const passwordsMatch = newPassword === confirmation;
  const [searchParams] = useSearchParams();
  const token = searchParams.get("userId");


  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;

    setIsSubmitting(true);

    const result = await resetpassword(token, newPassword);
    setToast(result.success
        ? {type: 'success', message: 'Пароль успішно змінено.'}
        : {type: 'error', message: 'Не вдалося змінити пароль. Пароль не змінено.'});
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
            <form onSubmit={handleSubmit}>
              <label htmlFor="new-password">Новий пароль</label>
              <input id="new-password" type="password" value={newPassword}
                     onChange={(event) => setNewPassword(event.target.value)} placeholder="Введіть новий пароль"
                     autoComplete="new-password" minLength="8" required/>
              <label htmlFor="confirm-password">Повторіть пароль</label>
              <input id="confirm-password" type="password" value={confirmation}
                     onChange={(event) => setConfirmation(event.target.value)} placeholder="Повторіть новий пароль"
                     autoComplete="new-password" minLength="8" required aria-describedby="password-match-message"/>
              {confirmation && !passwordsMatch &&
                  <p id="password-match-message" className="reset-password-error">Паролі мають збігатися.</p>}
              <button type="submit" disabled={!passwordsMatch}>Змінити пароль</button>
            </form>
            <button type="button" className="reset-password-link" onClick={onBack}>Назад</button>
          </div>
        </section>
        <Toast {...toast} onClose={() => setToast(null)}/>
      </main>
  );
}
