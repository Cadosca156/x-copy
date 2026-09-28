package com.net.cardo.xcopybackend.service;



import com.net.cardo.xcopybackend.entity.User;
import com.net.cardo.xcopybackend.repository.UserRepository;
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
        System.out.println("🔥🔥🔥 CUSTOM Oidc SERVICE CALLED 🔥🔥🔥");

        OidcUser oidcUser = super.loadUser(OidcUserRequest);

        String email = oidcUser.getAttribute("email");


        System.out.println("GOOGLE EMAIL: " + email);


        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            User newUser = new User();

            newUser.setEmail(email);
            System.out.println("GOOGLE EMAIL: " + email);

            User savedUser = userRepository.save(newUser);
            System.out.println("SAVED USER ID: " + savedUser.getId());
            System.out.println("SAVED USER EMAIL: " + savedUser.getEmail());
        }


        return oidcUser;
    }



    }





