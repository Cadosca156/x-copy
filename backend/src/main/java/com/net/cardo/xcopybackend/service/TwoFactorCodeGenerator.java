package com.net.cardo.xcopybackend.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TwoFactorCodeGenerator {
  private final   SecureRandom random = new SecureRandom();

  public String generateTwoFactorCode() {
      return String.valueOf(100000 + random.nextInt(900000));
  }

}
