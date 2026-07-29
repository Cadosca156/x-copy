package com.net.cardo.notebookbackend.controller;

import com.net.cardo.notebookbackend.dto.LoginRequest;
import com.net.cardo.notebookbackend.dto.LoginResponse;
import com.net.cardo.notebookbackend.dto.RegisterRequest;

import com.net.cardo.notebookbackend.dto.RegisterResponse;
import com.net.cardo.notebookbackend.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor


public class UserController {
    private final UserService userService;
    @PostMapping("/register")
    public RegisterResponse registerUser(@RequestBody RegisterRequest registerRequest) {return userService.registerUser(registerRequest);
    }
    @PostMapping("/login")
    public LoginResponse loginUser(@RequestBody LoginRequest loginRequest) {return userService.loginUser(loginRequest);
    }

}
