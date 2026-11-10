package com.net.cardo.xcopybackend.auth.service;


import com.net.cardo.xcopybackend.auth.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.auth.dto.response.LoginResponse;
import com.net.cardo.xcopybackend.exception.exceptions.*;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.exception.exceptions.InvalidPasswordException;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TwoFactorService twoFactorService;
    private final com.net.cardo.xcopybackend.security.EphemeralAuthStore securityStore;

    public LoginResponse loginOrRegister(LoginRequest request) {
        if(request==null || request.email()==null || request.email().length()>254 || !request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ||
           request.password()==null || request.password().isBlank() || request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) {
            throw new InvalidLoginRequestException();
        }
        securityStore.limit("password:"+request.email().toLowerCase(java.util.Locale.ROOT),10,java.time.Duration.ofMinutes(15));


        Optional<User> optionalUser =
                userRepository.findByEmail(request.email());

        User user;

        if (optionalUser.isPresent()) {

            user = optionalUser.get();

            String storedPassword = user.getPassword();
            if (storedPassword == null || storedPassword.isBlank() || !passwordEncoder.matches(
                    request.password(),
                    storedPassword
            )) {
                throw new InvalidPasswordException();
            }

        } else {

            if(request.password().length()<8) throw new InvalidPasswordException("Пароль має містити щонайменше 8 символів");
            user = new User();

            user.setEmail(request.email());
            user.setPassword(
                    passwordEncoder.encode(request.password())
            );

            // Keep unverified credentials out of the account table. Google login must
            // never inherit a password submitted by someone who does not own the email.
            return new LoginResponse(null, twoFactorService.beginRegistration(user));
        }

        return new LoginResponse(user.getId(), twoFactorService.beginLogin(user));
    }
    public void updateUsername(User user,String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT);

        if (!normalized.matches("^[a-z0-9_]{3,30}$")) {
            throw new InvalidUsernameException("Ім’я користувача має містити 3–30 латинських літер, цифр або підкреслень");
        }

        if(userRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new UsernameAlreadyExistsException();
        }
        user.setUsername(normalized);
        userRepository.save(user);
    }



}
