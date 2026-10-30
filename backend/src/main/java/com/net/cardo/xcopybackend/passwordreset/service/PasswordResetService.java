package com.net.cardo.xcopybackend.passwordreset.service;

import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.config.FrontendLinks;

import com.net.cardo.xcopybackend.passwordreset.dto.request.ResetPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.entity.PasswordResetToken;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;


import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.net.cardo.xcopybackend.passwordreset.dto.request.ForgotPasswordRequest;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FrontendLinks frontendLinks;


     public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userEmail = userRepository.findByEmail(request.email());

        if (userEmail.isEmpty()) {
           throw new RuntimeException("If email exists, email has been sent");
        }
        User user = userEmail.get();

        String token = UUID.randomUUID().toString();

        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken resetToken = new PasswordResetToken();

        resetToken.setUserId(user.getId());
        resetToken.setToken(token);
        resetToken.setExpiryDate(expiryDate);
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);

         String resetLink =
                 frontendLinks.resetPassword(token);
         emailService.sendEmail(
                 user.getEmail(),
                 "Reset password",
                 "Click this link to reset your password: " + resetLink
         );
     }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw invalidResetLink();
        }
        String password = request.getNewPassword();
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пароль має містити щонайменше 8 символів і не перевищувати 72 байти.");
        }

        Optional<PasswordResetToken> optionalToken =
                passwordResetTokenRepository.findByToken(request.getToken());

        if (optionalToken.isEmpty()) {
            throw invalidResetLink();
        }

        PasswordResetToken resetToken = optionalToken.get();

        if (resetToken.isUsed()) {
            throw invalidResetLink();
        }

        if (resetToken.getExpiryDate() == null || !resetToken.getExpiryDate().isAfter(LocalDateTime.now())) {
            throw invalidResetLink();
        }
        Optional<User> optionalUser =
                userRepository.findById(resetToken.getUserId());

        if (optionalUser.isEmpty()) {
            throw invalidResetLink();
        }

        User user = optionalUser.get();

        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);

        userRepository.save(user);
        userRepository.invalidateSessions(user.getEmail());
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.invalidateForUser(user.getId());

    }

    private ResponseStatusException invalidResetLink() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Посилання для зміни пароля недійсне або вже використане. Запросіть новий лист.");
    }

}
