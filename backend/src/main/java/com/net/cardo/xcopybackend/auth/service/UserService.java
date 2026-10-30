package com.net.cardo.xcopybackend.auth.service;


import com.net.cardo.xcopybackend.auth.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.auth.dto.response.LoginResponse;
import com.net.cardo.xcopybackend.exception.exceptions.InvalidUsernameException;
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
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Перевірте email та пароль");
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

            if(request.password().length()<8) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Пароль має містити щонайменше 8 символів");
            user = new User();

            user.setEmail(request.email());
            user.setPassword(
                    passwordEncoder.encode(request.password())
            );

            user = userRepository.save(user);
        }

        return new LoginResponse(user.getId(), twoFactorService.beginLogin(user));
    }
    public void updateUsername(User user,String username) {
        String normalized = username.trim().toLowerCase();

        if (!normalized.matches("^[a-z0-9_]{3,30}$")) {
            throw new IllegalArgumentException("Invalid username");
        }

        if(userRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Username already taken");
        }
        user.setUsername(normalized);
        userRepository.save(user);
    }



}
