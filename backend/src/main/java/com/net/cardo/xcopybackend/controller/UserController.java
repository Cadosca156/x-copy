package com.net.cardo.xcopybackend.controller;

import com.net.cardo.xcopybackend.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.dto.response.LoginResponse;

import com.net.cardo.xcopybackend.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor


public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    public LoginResponse loginUser(@RequestBody LoginRequest loginRequest) {
        return userService.loginOrRegister(loginRequest);

    }

}
