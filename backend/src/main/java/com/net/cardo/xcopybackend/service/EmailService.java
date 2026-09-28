package com.net.cardo.xcopybackend.service;

import lombok.RequiredArgsConstructor;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendTwoFactorCode(String email, String code) {


        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Your Verification Code");
        message.setText("Your Verification Code is: " + code);

        mailSender.send(message);

    }
}
