package com.net.cardo.xcopybackend.dto.request;

import lombok.Getter;
import lombok.Setter;


public record LoginRequest(
        String email,
         String password) {


}
