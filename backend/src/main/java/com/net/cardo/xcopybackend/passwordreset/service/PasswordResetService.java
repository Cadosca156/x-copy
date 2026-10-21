package com.net.cardo.xcopybackend.passwordreset.service;

import com.net.cardo.xcopybackend.email.EmailService;

import com.net.cardo.xcopybackend.passwordreset.dto.request.ResetPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.entity.PasswordResetToken;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


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
                 "http://localhost:5173/reset-password?token=" + token;
         emailService.sendEmail(
                 user.getEmail(),
                 "Reset password",
                 "Click this link to reset your password: " + resetLink
         );
     }

    public void resetPassword(ResetPasswordRequest request) {

        Optional<PasswordResetToken> optionalToken =
                passwordResetTokenRepository.findByToken(request.getToken());

        if (optionalToken.isEmpty()) {
            return;
        }

        PasswordResetToken resetToken = optionalToken.get();

        if (resetToken.isUsed()) {
            return;
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return;
        }
        Optional<User> optionalUser =
                userRepository.findById(resetToken.getUserId());

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(hashedPassword);

        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

    }

}
