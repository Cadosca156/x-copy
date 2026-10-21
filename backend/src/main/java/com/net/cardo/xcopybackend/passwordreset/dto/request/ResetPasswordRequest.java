package com.net.cardo.xcopybackend.passwordreset.dto.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
   String token;
    String newPassword;
}
