export function getAccountSetupPath(profile, passwordStatus) {
    if (typeof passwordStatus?.passwordSet !== 'boolean') {
        throw new Error('Не вдалося перевірити, чи задано пароль. Спробуй увійти ще раз.');
    }
    if (!passwordStatus.passwordSet) return '/setup-password';
    return profile?.username ? '/feed' : '/setup-profile';
}

export function validateInitialPassword(password, confirmation) {
    if (!password.trim() || password.length < 8) return 'Пароль має містити щонайменше 8 символів.';
    if (new TextEncoder().encode(password).length > 72) return 'Пароль занадто довгий. Використай коротший пароль.';
    if (password !== confirmation) return 'Паролі мають збігатися.';
    return '';
}
