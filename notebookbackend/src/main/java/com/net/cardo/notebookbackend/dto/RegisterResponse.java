package com.net.cardo.notebookbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private String username;
    private String email;
    private String message;
}
