package com.net.cardo.xcopybackend.service;


import com.net.cardo.xcopybackend.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.dto.response.LoginResponse;
import com.net.cardo.xcopybackend.entity.User;
import com.net.cardo.xcopybackend.exception.exceptions.*;
import com.net.cardo.xcopybackend.repository.UserRepository;


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



}
