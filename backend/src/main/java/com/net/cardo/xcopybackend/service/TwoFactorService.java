package com.net.cardo.xcopybackend.service;

import com.net.cardo.xcopybackend.entity.TwoFactorCode;
import com.net.cardo.xcopybackend.entity.User;
import com.net.cardo.xcopybackend.exception.exceptions.InvalidTwoFactorCodeException;
import com.net.cardo.xcopybackend.exception.exceptions.UserNotFoundException;
import com.net.cardo.xcopybackend.repository.TwoFactorRepository;
import com.net.cardo.xcopybackend.repository.UserRepository;
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

        System.out.println("🔥🔥 SEND 2FA CODE: " + email);

        String code = twoFactorCodeGenerator.generateTwoFactorCode();

        TwoFactorCode twoFactorCode = new TwoFactorCode();

        twoFactorCode.setUserId(userId);
        twoFactorCode.setCode(code);
        twoFactorCode.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );

        twoFactorRepository.save(twoFactorCode);

        emailService.sendTwoFactorCode(email, code);
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
