import React, { useState } from "react";
import "../../styles/Login.css";
import  {loginUser} from "../../utils/AuthDBStorage.js"
import { useNavigate } from "react-router-dom";
export default function Login() {
  const [login, setLogin] = useState({
    email: "",
    password: "",
  });
  const navigate = useNavigate();
  const handleLogin = async (login) => {

    const result = await loginUser(login);

    if (result.response.ok) {
      const data = await result.response.json();

      navigate(`/2fa?userId=${data.userId}`);
    }
  }




  return (
      <main className="x-login">
        <section className="x-login__left">
          <div className="x-login__form">
            <h1>В курсе<br/>происходящего.</h1>

            <button className="auth-button" type="button">
              <span className="auth-icon phone-icon" aria-hidden="true">⌕</span>
              <span>Продолжить с телефоном</span>
            </button>

            <button className="auth-button" type="button"
                    onClick={() => {
                      window.location.href = "http://localhost:8080/oauth2/authorization/google";
                    }}>
            <span className="google-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path fill="#4285F4"
                      d="M21.6 12.23c0-.7-.06-1.37-.18-2H12v3.79h5.38a4.6 4.6 0 0 1-2 3.02v2.51h3.24c1.9-1.75 2.98-4.33 2.98-7.32Z"/>
                <path fill="#34A853"
                      d="M12 22c2.7 0 4.96-.9 6.62-2.45l-3.24-2.51c-.9.6-2.04.96-3.38.96-2.6 0-4.8-1.76-5.59-4.12H3.06v2.59A10 10 0 0 0 12 22Z"/>
                <path fill="#FBBC05"
                      d="M6.41 13.88A6.01 6.01 0 0 1 6.1 12c0-.65.11-1.29.31-1.88V7.53H3.06A10 10 0 0 0 2 12c0 1.61.38 3.13 1.06 4.47l3.35-2.59Z"/>
                <path fill="#EA4335"
                      d="M12 6c1.47 0 2.8.51 3.84 1.51l2.88-2.88C16.96 2.99 14.7 2 12 2a10 10 0 0 0-8.94 5.53l3.35 2.59C7.2 7.76 9.4 6 12 6Z"/>
              </svg>
            </span>
              <span>Продолжить с Google</span>
            </button>

            <button className="auth-button" type="button">
              <span className="apple-icon" aria-hidden="true">●</span>
              <span>Продолжить с Apple</span>
            </button>

            <div className="divider"><span>или</span></div>

            <label className={`identifier-field ${login.email ? "has-value" : ""}`}>
              <span>Электронная почта</span>
              <input
                  value={login.email}
                  onChange={(e) => setLogin({
                    ...login,
                    email: e.target.value
                  })}
                  autoComplete="email"
                  spellCheck="false"
              />
            </label>
            <label className={`identifier-field ${login.password ? "has-value" : ""}`}>
              <span>Пароль</span>
              <input
                  value={login.password}
                  onChange={(e) => setLogin({
                    ...login,
                    password: e.target.value
                  })}
                  autoComplete="password"
                  spellCheck="false"
              />
            </label>

            <button
                className={`continue-button ${login.password ? "enabled" : ""}`}
                type="button"
                disabled={!login.password}
                onClick={() => {
                  handleLogin(login)
                }}
            >
              Продолжить
            </button>

            <p className="legal">
              Продолжая, ты соглашаешься с нашими{" "}
              <a href="#terms">Условиями использования</a>,{" "}
              <a href="#privacy">Политикой конфиденциальности</a> и{" "}
              <a href="#cookies">Использованием файлов cookie</a>.
            </p>
          </div>
        </section>

        <section className="x-login__right" aria-hidden="true">
          <div className="x-mark">
            <svg viewBox="0 0 500 500">
              <path
                  d="M92 64h93l112 157 116-157h51L320 257l151 179h-93L264 278 137 436H86l152-190L92 64Zm78 22h-37l129 177 18-23L170 86Zm159 354h37L238 264l-18 23 109 153Z"/>
            </svg>
          </div>
        </section>

        <div className="app-card">
          <div className="app-card__title">Просканируйте для загрузки<br/>приложения</div>
          <div className="fake-qr">
            {Array.from({length: 169}).map((_, i) => (
                <i key={i} className={(i * 17 + i * i * 3) % 7 < 3 ? "on" : ""}/>
            ))}
            <b className="qr-logo">X</b>
          </div>
        </div>

        <footer className="x-footer">
          <span>О нас</span><span>Скачать приложение</span><span>Grok</span><span>Помощь</span>
          <span>Условия</span><span>Конфиденциальность</span><span>Файлы cookie</span>
          <span>Работа</span><span>Реклама и бизнес</span><span>Разработчикам</span>
          <span>Новости</span><span>Специальные возможности</span><span>© 2026 X Corp.</span>
        </footer>
      </main>
  );
}
