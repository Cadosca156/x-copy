package com.net.cardo.xcopybackend.twofactor.service;

import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.security.JwtService;
import com.net.cardo.xcopybackend.twofactor.entity.TwoFactorCode;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.exception.exceptions.InvalidTwoFactorCodeException;
import com.net.cardo.xcopybackend.exception.exceptions.UserNotFoundException;
import com.net.cardo.xcopybackend.twofactor.repository.TwoFactorRepository;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

   private final TwoFactorRepository twoFactorRepository;
    private final EmailService emailService;
    private final TwoFactorCodeGenerator twoFactorCodeGenerator;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public void sendCode(UUID userId, String email) {
        User user;

        if (userId != null) {

            user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new RuntimeException("User not found by ID")
                    );

        } else if (email != null && !email.isBlank()) {

            user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new RuntimeException("User not found by email")
                    );

        } else {
            throw new IllegalArgumentException(
                    "User ID or email is required"
            );
        }


        UUID actualUserId = user.getId();
        String actualEmail = user.getEmail();

       Optional<TwoFactorCode> findcode = twoFactorRepository.findCodeByUserId(actualUserId);

        if (findcode.isPresent()) {

            TwoFactorCode existingCode = findcode.get();

            if (existingCode.getExpiresAt().isAfter(LocalDateTime.now())) {

                emailService.sendEmail(actualEmail,
                        "Verification code",
                        "Your verification code " + existingCode.getCode());
                return;
            }
            twoFactorRepository.delete(existingCode);

        }
        String code = twoFactorCodeGenerator.generateTwoFactorCode();

        TwoFactorCode twoFactorCode = new TwoFactorCode();

        twoFactorCode.setUserId(actualUserId);
        twoFactorCode.setCode(code);
        twoFactorCode.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );

        twoFactorRepository.save(twoFactorCode);

        emailService.sendEmail(actualEmail,
                "Verification code",
                "Your verification code " + code
        );

    }
    public String verifyCode(UUID userId, String code) {

        Optional<TwoFactorCode> optionalCode =
                twoFactorRepository.findByUserIdAndCode(userId, code);

        if (optionalCode.isEmpty()) {
            throw new InvalidTwoFactorCodeException("Code not found");
        }

        TwoFactorCode twoFactorCode = optionalCode.get();

        if (twoFactorCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            twoFactorRepository.delete(twoFactorCode);
            throw new InvalidTwoFactorCodeException("Code expired");
        }

        twoFactorRepository.delete(twoFactorCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return jwtService.generateToken(user.getEmail());
    }



}
