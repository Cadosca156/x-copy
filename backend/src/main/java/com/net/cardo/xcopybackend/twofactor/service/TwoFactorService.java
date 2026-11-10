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
        store.put(key(challenge),user.getId()+"|"+user.getCredentialVersion()+"|"+encoder.encode(code),Duration.ofMinutes(5));
        mail.sendEmail(user.getEmail(),"Verification code","Your verification code "+code);
        return challenge;
    }
    public String beginRegistration(User pending) {
        pending.setEmail(com.net.cardo.xcopybackend.auth.EmailAddress.clean(pending.getEmail()));
        store.limit("mail-registration:"+com.net.cardo.xcopybackend.auth.EmailAddress.key(pending.getEmail()),5,Duration.ofMinutes(15));
        String challenge=EphemeralAuthStore.random(), code=generator.generateTwoFactorCode();
        String email=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                pending.getEmail().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // The entire pending registration expires with the OTP, including its password hash.
        store.put(key(challenge),UUID.randomUUID()+"|0|"+encoder.encode(code)+"|"+email+"|"+pending.getPassword(),Duration.ofMinutes(5));
        mail.sendEmail(pending.getEmail(),"Verification code","Your verification code "+code);
        return challenge;
    }
    private User pendingUser(String[] fields) {
        User user=new User();
        user.setEmail(com.net.cardo.xcopybackend.auth.EmailAddress.clean(new String(java.util.Base64.getUrlDecoder().decode(fields[3]),java.nio.charset.StandardCharsets.UTF_8)));
        user.setPassword(fields[4]);
        // Another verified registration or Google login must not inherit pending credentials.
        if(users.findByEmail(user.getEmail()).isPresent())throw invalid();
        return user;
    }
    public void resend(String challenge) {
        String k=key(challenge), value=store.get(k);
        if(value==null) throw invalid();
        String[] fields=value.split("\\|");
        boolean registration=fields.length==5;
        User user=registration?pendingUser(fields):users.findById(UUID.fromString(fields[0])).orElseThrow(this::invalid);
        if(user.getCredentialVersion()!=Integer.parseInt(fields[1])) throw invalid();
        store.limit("resend:"+k,1,Duration.ofSeconds(60));
        store.limit(registration?"mail-registration:"+com.net.cardo.xcopybackend.auth.EmailAddress.key(user.getEmail()):"mail-login:"+user.getId(),5,Duration.ofMinutes(15));
        String code=generator.generateTwoFactorCode();
        fields[2]=encoder.encode(code);
        if(!store.replace(k,value,String.join("|",fields))) throw invalid();
        mail.sendEmail(user.getEmail(),"Verification code","Your verification code "+code);
    }
    @org.springframework.transaction.annotation.Transactional
    public LoginProof verifyCode(String challenge,String code) {
        String k=key(challenge), value=store.get(k);
        if(value==null) throw invalid();
        // Failed attempts survive resends and are shared by all app instances.
        store.limit("otp:"+k,5,Duration.ofMinutes(5));
        if(code==null || !code.matches("[0-9]{6}")) throw invalid();
        String[] fields=value.split("\\|");
        if(!encoder.matches(code,fields[2])) throw invalid();
        boolean registration=fields.length==5;
        User user=registration?pendingUser(fields):users.findById(UUID.fromString(fields[0])).orElseThrow(this::invalid);
        if(user.getCredentialVersion()!=Integer.parseInt(fields[1]) || !store.consume(k,value)) throw invalid();
        if(registration) {
            try { user=users.saveAndFlush(user); }
            catch(org.springframework.dao.DataIntegrityViolationException failure) {
                // A verified registration races only on account identity; never overwrite an existing password.
                for(Throwable cause=failure;cause!=null;cause=cause.getCause()) {
                    if(cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                            && "23505".equals(constraint.getSQLState()))
                        throw new com.net.cardo.xcopybackend.exception.exceptions.EmailAlreadyExistsException("Цей email уже зареєстрований. Увійдіть в акаунт або відновіть пароль.");
                }
                throw failure;
            }
        }
        return LoginProof.from(user);
    }
}
