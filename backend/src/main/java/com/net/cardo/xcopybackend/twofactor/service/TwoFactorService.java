package com.net.cardo.xcopybackend.twofactor.service;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.security.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
@Service @RequiredArgsConstructor
public class TwoFactorService {
    private final EmailService mail;
    private final TwoFactorCodeGenerator generator;
    private final UserRepository users;
    private final JwtService jwt;
    private final EphemeralAuthStore store;
    private final PasswordEncoder encoder;
    private String key(String challenge) {
        if(challenge==null || !challenge.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        return "security:challenge:"+EphemeralAuthStore.digest(challenge);
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(BAD_REQUEST,"Код або сесія входу недійсні. Почніть вхід знову."); }
    public String beginLogin(User user) {
        store.limit("mail-login:"+user.getId(),5,Duration.ofMinutes(15));
        String challenge=EphemeralAuthStore.random(), code=generator.generateTwoFactorCode();
        store.put(key(challenge),user.getId()+"|"+user.getTokenVersion()+"|"+encoder.encode(code),Duration.ofMinutes(5));
        mail.sendEmail(user.getEmail(),"Verification code","Your verification code "+code);
        return challenge;
    }
    public void resend(String challenge) {
        String k=key(challenge), value=store.get(k);
        if(value==null) throw invalid();
        String[] fields=value.split("\\|");
        User user=users.findById(UUID.fromString(fields[0])).orElseThrow(this::invalid);
        if(user.getTokenVersion()!=Integer.parseInt(fields[1])) throw invalid();
        store.limit("resend:"+k,1,Duration.ofSeconds(60));
        store.limit("mail-login:"+user.getId(),5,Duration.ofMinutes(15));
        String code=generator.generateTwoFactorCode();
        if(!store.replace(k,value,fields[0]+"|"+fields[1]+"|"+encoder.encode(code))) throw invalid();
        mail.sendEmail(user.getEmail(),"Verification code","Your verification code "+code);
    }
    public String verifyCode(String challenge,String code) {
        String k=key(challenge), value=store.get(k);
        if(value==null) throw invalid();
        // Failed attempts survive resends and are shared by all app instances.
        store.limit("otp:"+k,5,Duration.ofMinutes(5));
        if(code==null || !code.matches("[0-9]{6}")) throw invalid();
        String[] fields=value.split("\\|");
        if(!encoder.matches(code,fields[2])) throw invalid();
        User user=users.findById(UUID.fromString(fields[0])).orElseThrow(this::invalid);
        if(user.getTokenVersion()!=Integer.parseInt(fields[1]) || !store.consume(k,value)) throw invalid();
        return jwt.generateToken(user.getEmail());
    }
}
