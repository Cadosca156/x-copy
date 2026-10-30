package com.net.cardo.xcopybackend.security;
import java.time.Duration;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Base64;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @RequiredArgsConstructor
public class EphemeralAuthStore {
    private final StringRedisTemplate redis;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> COUNT = new DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return n",Long.class);
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>(
        "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) end; return 0",Long.class);
    private static final DefaultRedisScript<Long> REPLACE = new DefaultRedisScript<>(
        "if redis.call('GET',KEYS[1])~=ARGV[1] then return 0 end; local t=redis.call('TTL',KEYS[1]); if t<=0 then return 0 end; redis.call('SET',KEYS[1],ARGV[2],'EX',t); return 1",Long.class);
    public static String random() { byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public void put(String key,String value,Duration ttl) { redis.opsForValue().set(key,value,ttl); }
    public String get(String key) { return redis.opsForValue().get(key); }
    public String take(String key) { return redis.opsForValue().getAndDelete(key); }
    public void delete(String key) { redis.delete(key); }
    public boolean consume(String key,String expected) { return Long.valueOf(1).equals(redis.execute(CONSUME,List.of(key),expected)); }
    public boolean replace(String key,String expected,String value) { return Long.valueOf(1).equals(redis.execute(REPLACE,List.of(key),expected,value)); }
    public void limit(String key,int max,Duration window) {
        Long n=redis.execute(COUNT,List.of("security:limit:"+digest(key)),String.valueOf(window.toSeconds()));
        if(n==null || n>max) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Забагато спроб. Спробуйте пізніше.");
    }
}
