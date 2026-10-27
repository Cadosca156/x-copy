package com.net.cardo.xcopybackend.auth.service;

import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class AccountPasswordService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public record PasswordStatus(boolean passwordSet) {}

    public PasswordStatus getStatus(String email) {
        return new PasswordStatus(hasPassword(findUser(email)));
    }

    @Transactional
    public PasswordStatus setInitialPassword(String email, String password) {
        User user = findUser(email);
        if (hasPassword(user)) {
            throw new ResponseStatusException(CONFLICT,
                    "Пароль уже задано. Для його зміни скористайтеся відновленням пароля.");
        }
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Пароль має містити щонайменше 8 символів і не перевищувати 72 байти.");
        }
        // A conditional update prevents simultaneous requests from replacing a password.
        if (users.setInitialPassword(email, passwords.encode(password)) != 1) {
            throw new ResponseStatusException(CONFLICT,
                    "Пароль уже задано. Оновіть сторінку, щоб продовжити.");
        }
        return new PasswordStatus(true);
    }

    private User findUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    private boolean hasPassword(User user) {
        return user.getPassword() != null && !user.getPassword().isBlank();
    }
}
