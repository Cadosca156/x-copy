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

    public LoginResponse loginOrRegister(LoginRequest request) {

        Optional<User> optionalUser =
                userRepository.findByEmail(request.email());

        User user;

        if (optionalUser.isPresent()) {

            user = optionalUser.get();

            if (!passwordEncoder.matches(
                    request.password(),
                    user.getPassword()
            )) {
                throw new InvalidPasswordException();
            }

        } else {

            user = new User();

            user.setEmail(request.email());
            user.setPassword(
                    passwordEncoder.encode(request.password())
            );

            user = userRepository.save(user);
        }

        twoFactorService.sendCode(
                user.getId(),
                user.getEmail()
        );

        return new LoginResponse(user.getId());
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
