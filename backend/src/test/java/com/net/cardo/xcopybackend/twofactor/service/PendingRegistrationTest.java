package com.net.cardo.xcopybackend.twofactor.service;

import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.security.EphemeralAuthStore;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PendingRegistrationTest {
    private final EphemeralAuthStore store=mock(EphemeralAuthStore.class);
    private final EmailService mail=mock(EmailService.class);
    private final UserRepository users=mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    private final TwoFactorCodeGenerator generator=mock(TwoFactorCodeGenerator.class);
    private final TwoFactorService service=new TwoFactorService(mail,generator,users,store,encoder);

    private String pending(String challenge) {
        String email=Base64.getUrlEncoder().withoutPadding().encodeToString("owner@example.test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String value=UUID.randomUUID()+"|0|"+encoder.encode("123456")+"|"+email+"|"+encoder.encode("pending-password");
        when(store.get("security:challenge:"+EphemeralAuthStore.digest(challenge))).thenReturn(value);
        return value;
    }

    @Test void createsAccountOnlyAfterCorrectCodeAndAtomicConsumption() {
        String challenge=EphemeralAuthStore.random(), value=pending(challenge);
        when(store.consume(anyString(),eq(value))).thenReturn(true);
        when(users.saveAndFlush(any())).thenAnswer(i->{User user=i.getArgument(0);user.setId(UUID.randomUUID());return user;});
        var proof=service.verifyCode(challenge,"123456");
        assertNotNull(proof.userId());
        verify(users).saveAndFlush(argThat(u->u.getEmail().equals("owner@example.test") && encoder.matches("pending-password",u.getPassword())));
    }

    @Test void wrongCodeOrConsumedChallengeCannotPersistAnAccount() {
        String challenge=EphemeralAuthStore.random();pending(challenge);
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"000000"));
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"123456"));
        verify(users,never()).saveAndFlush(any());
    }

    @Test void googleAccountCreatedDuringRegistrationNeverInheritsPendingPassword() {
        String challenge=EphemeralAuthStore.random();pending(challenge);
        User google=new User();google.setId(UUID.randomUUID());google.setEmail("owner@example.test");
        when(users.findByEmail(google.getEmail())).thenReturn(Optional.of(google));
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"123456"));
        assertThrows(ResponseStatusException.class,()->service.resend(challenge));
        assertNull(google.getPassword());
        verify(users,never()).saveAndFlush(any());
        verifyNoInteractions(mail);
    }

    @Test void resendPreservesPendingCredentialsAndCannotExtendExpiry() {
        String challenge=EphemeralAuthStore.random(), value=pending(challenge);
        when(generator.generateTwoFactorCode()).thenReturn("654321");
        when(store.replace(anyString(),eq(value),anyString())).thenReturn(true);
        service.resend(challenge);
        String[] original=value.split("\\|");
        verify(store).replace(anyString(),eq(value),argThat(next->{
            String[] fields=next.split("\\|");return fields.length==5 && fields[0].equals(original[0])
                    && fields[3].equals(original[3]) && fields[4].equals(original[4]) && encoder.matches("654321",fields[2]);
        }));
        verify(store,never()).put(anyString(),anyString(),any());
    }
}
