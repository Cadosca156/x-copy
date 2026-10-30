package com.net.cardo.xcopybackend.twofactor.service;
import com.net.cardo.xcopybackend.security.*;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class TwoFactorSecurityTest {
    private final EphemeralAuthStore store=mock(EphemeralAuthStore.class);
    private final EmailService mail=mock(EmailService.class);
    private final UserRepository users=mock(UserRepository.class);
    private final JwtService jwt=mock(JwtService.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    private final TwoFactorCodeGenerator generator=mock(TwoFactorCodeGenerator.class);
    private final TwoFactorService service=new TwoFactorService(mail,generator,users,jwt,store,encoder);
    private String challenge=EphemeralAuthStore.random();
    private String key="security:challenge:"+EphemeralAuthStore.digest(challenge);
    @Test void publicUserIdCannotBeUsedAsLoginChallenge() {
        assertThrows(ResponseStatusException.class,()->service.resend(UUID.randomUUID().toString()));
        assertThrows(ResponseStatusException.class,()->service.verifyCode(UUID.randomUUID().toString(),"123456"));
        verifyNoInteractions(mail,jwt);
    }
    @Test void correctCodeRequiresLivePasswordChallengeAndIsConsumed() {
        User u=new User();u.setId(UUID.randomUUID());u.setEmail("user@example.test");
        String value=u.getId()+"|0|"+encoder.encode("123456");
        when(store.get(key)).thenReturn(value);
        when(store.consume(key,value)).thenReturn(true).thenReturn(false);
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        when(jwt.generateToken(u)).thenReturn("test-session");
        assertEquals("test-session",service.verifyCode(challenge,"123456"));
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"123456"));
        verify(jwt,times(1)).generateToken(u);
    }
    @Test void wrongCodeConsumesAnAttemptAndDoesNotIssueToken() {
        when(store.get(key)).thenReturn(UUID.randomUUID()+"|0|"+encoder.encode("123456"));
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"000000"));
        verify(store).limit("otp:"+key,5,Duration.ofMinutes(5));
        verifyNoInteractions(jwt,users);
    }
    @Test void attemptsRemainBoundedEvenWhenCodeIsCorrect() {
        when(store.get(key)).thenReturn(UUID.randomUUID()+"|0|"+encoder.encode("123456"));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)).when(store).limit("otp:"+key,5,Duration.ofMinutes(5));
        assertEquals(429,assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"123456")).getStatusCode().value());
        verifyNoInteractions(jwt);
    }
    @Test void startStoresHashedCodeOnlyAndResendRequiresChallenge() {
        User u=new User();u.setId(UUID.randomUUID());u.setEmail("user@example.test");
        when(generator.generateTwoFactorCode()).thenReturn("123456");
        String challenge=service.beginLogin(u);
        assertTrue(challenge.matches("[A-Za-z0-9_-]{43}"));
        verify(store).put(anyString(),argThat(v->!v.contains("123456") && encoder.matches("123456",v.split("\\|")[2])),eq(Duration.ofMinutes(5)));
        assertThrows(ResponseStatusException.class,()->service.resend(challenge));
        verify(mail,times(1)).sendEmail(anyString(),anyString(),anyString());
    }
}
