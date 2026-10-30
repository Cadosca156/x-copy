package com.net.cardo.xcopybackend.auth.service;


import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest OidcUserRequest) {

        OidcUser oidcUser = super.loadUser(OidcUserRequest);

        String email = oidcUser.getAttribute("email");
        if (email == null || !Boolean.TRUE.equals(oidcUser.getAttribute("email_verified"))) throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("Google email is not verified");




        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            User newUser = new User();

            newUser.setEmail(email);

            User savedUser = userRepository.save(newUser);
        }


        return oidcUser;
    }



    }
