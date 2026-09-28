package com.net.cardo.xcopybackend.service;

import com.net.cardo.xcopybackend.entity.User;
import com.net.cardo.xcopybackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
     private final UserRepository userRepository;

     @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{

         User user = userRepository.findByEmail(email)
                 .orElseThrow(() -> new UsernameNotFoundException("User not found" + email));

         return org.springframework.security.core.userdetails.User
                 .withUsername(user.getEmail())
                 .password(user.getPassword())
                 .authorities("USER")
                 .build();

     }



    }

