package com.net.cardo.xcopybackend.auth;

import com.net.cardo.xcopybackend.auth.service.AccountPasswordService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailIdentityIntegrationTests {
    @Autowired UserRepository users;
    @Autowired AccountPasswordService passwords;
    @Autowired PasswordEncoder encoder;
    private final List<UUID> created = new ArrayList<>();
    @AfterEach void cleanup() { users.deleteAllById(created); }

    private User save(String email) {
        var user = new User();user.setEmail(email);user = users.saveAndFlush(user);created.add(user.getId());return user;
    }

    @Test void repositoryFindsEveryCaseAndPreservesTheDeliveryAddress() {
        String local = "Owner." + UUID.randomUUID() + "+Tag";
        var user = save("  " + local + "@Example.TEST  ");
        assertEquals(local + "@Example.TEST", user.getEmail());
        assertEquals((local + "@example.test").toLowerCase(Locale.ROOT), user.getEmailKey());
        for (String email : new String[]{user.getEmail().toLowerCase(Locale.ROOT), user.getEmail().toUpperCase(Locale.ROOT), "  " + user.getEmail() + "  "}) {
            assertEquals(user.getId(), users.findByEmail(email).orElseThrow().getId());assertTrue(users.existsByEmail(email));
        }
    }

    @Test void databaseRejectsCaseVariantsEvenWhenTheCallerSkipsLookup() {
        var user = save("Case-" + UUID.randomUUID() + "@Example.TEST");
        var duplicate = new User();duplicate.setEmail(user.getEmail().toLowerCase(Locale.ROOT));
        assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(duplicate));
        assertEquals(user.getId(), users.findByEmail(duplicate.getEmail()).orElseThrow().getId());
    }

    @Test void concurrentRegistrationsCannotPersistTwoCaseVariants() throws Exception {
        String email = "parallel-" + UUID.randomUUID() + "@example.test";
        var barrier = new CyclicBarrier(2);var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<User>> calls = List.of(
                    () -> insertAtBarrier(email, barrier), () -> insertAtBarrier(email.toUpperCase(Locale.ROOT), barrier));
            int successes = 0;
            for (var future : executor.invokeAll(calls)) {
                User user = future.get();
                if (user != null) { successes++;created.add(user.getId()); }
            }
            assertEquals(1, successes);assertTrue(users.findByEmail(email).isPresent());
        } finally { executor.shutdownNow(); }
    }

    private User insertAtBarrier(String email, CyclicBarrier barrier) throws Exception {
        var user = new User();user.setEmail(email);barrier.await(5, TimeUnit.SECONDS);
        try { return users.saveAndFlush(user); }
        catch (DataIntegrityViolationException duplicate) { return null; }
    }

    @Test void googlePasswordSetupFindsTheSameAccountUnderDifferentCase() {
        var user = save("Google-" + UUID.randomUUID() + "@Example.TEST");
        assertTrue(passwords.setInitialPassword(user.getEmail().toUpperCase(Locale.ROOT), "new-password-123").passwordSet());
        assertTrue(encoder.matches("new-password-123", users.findById(user.getId()).orElseThrow().getPassword()));
    }
}
