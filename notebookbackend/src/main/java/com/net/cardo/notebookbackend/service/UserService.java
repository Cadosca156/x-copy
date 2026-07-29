package com.net.cardo.notebookbackend.service;


import com.net.cardo.notebookbackend.dto.LoginRequest;
import com.net.cardo.notebookbackend.dto.LoginResponse;
import com.net.cardo.notebookbackend.dto.RegisterRequest;
import com.net.cardo.notebookbackend.dto.RegisterResponse;
import com.net.cardo.notebookbackend.entity.User;
import com.net.cardo.notebookbackend.exception.exceptions.*;
import com.net.cardo.notebookbackend.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    public RegisterResponse registerUser(RegisterRequest registerRequest) {
        if (registerRequest.getPassword() == null ||
                registerRequest.getPassword().length() < 8)
            throw new InvalidPasswordException("Invalid password");

        if (registerRequest.getEmail() == null ||
                registerRequest.getEmail().isBlank())

            throw new InvalidEmailException("Invalid email");

        if (registerRequest.getUsername() == null
                ||
                registerRequest.getUsername().isBlank()
                ||
                registerRequest.getUsername().length() < 3)
            throw new InvalidUsernameException("Invalid username");

        if (userRepository.existsByEmail(registerRequest.getEmail()))
            throw new EmailAlreadyExistsException("Email already exists");

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());

        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        userRepository.save(user);

        return new RegisterResponse(
                user.getEmail(),
                user.getUsername(),
                "registered successfully"
        );
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                        .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
            throw new InvalidPasswordException("Wrong password");


        String token = jwtService.generateToken(user.getEmail());
        System.out.println(jwtService.extractEmail(token));

        return new LoginResponse(
                token,
                user.getUsername()
        );
    }


}
