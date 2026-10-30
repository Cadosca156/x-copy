package com.net.cardo.xcopybackend.auth.service;
import com.net.cardo.xcopybackend.auth.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.security.EphemeralAuthStore;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UserServiceSecurityTest {
    private final UserRepository users=mock(UserRepository.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private final TwoFactorService codes=mock(TwoFactorService.class);
    private final EphemeralAuthStore store=mock(EphemeralAuthStore.class);
    private final UserService service=new UserService(users,encoder,codes,store);
    @Test void accountLimitAppliesBeforePasswordChecksRegardlessOfTheClientIp() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(store)
            .limit("password:target@example.test",10,Duration.ofMinutes(15));
        assertEquals(429,assertThrows(ResponseStatusException.class,()->
            service.loginOrRegister(new LoginRequest("TARGET@example.test","password"))).getStatusCode().value());
        verifyNoInteractions(users,encoder,codes);
    }
    @Test void malformedOrOversizedCredentialsDoNotReachDatabaseOrPasswordHasher() {
        for(var request:new LoginRequest[]{new LoginRequest(null,"password"),new LoginRequest("invalid","password"),
            new LoginRequest("target@example.test",null),new LoginRequest("target@example.test","я".repeat(37))}) {
            assertEquals(400,assertThrows(ResponseStatusException.class,()->service.loginOrRegister(request)).getStatusCode().value());
        }
        verifyNoInteractions(users,encoder,codes,store);
    }
}
