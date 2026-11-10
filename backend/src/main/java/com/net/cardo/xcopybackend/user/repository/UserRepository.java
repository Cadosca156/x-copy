package com.net.cardo.xcopybackend.user.repository;

import com.net.cardo.xcopybackend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("select u from User u where u.id <> :viewer and u.username is not null and trim(u.username) <> '' order by u.username, u.id")
    java.util.List<User> findFollowSuggestions(@Param("viewer") UUID viewer, org.springframework.data.domain.Pageable pageable);

    default boolean existsByEmail(String email) {
        String key = com.net.cardo.xcopybackend.auth.EmailAddress.key(email);
        return key != null && existsByEmailKey(key);
    }
    default Optional<User> findByEmail(String email) {
        String key = com.net.cardo.xcopybackend.auth.EmailAddress.key(email);
        return key == null ? Optional.empty() : findByEmailKey(key);
    }
    boolean existsByEmailKey(String emailKey);
    Optional<User> findByEmailKey(String emailKey);
    Optional<User> findById(UUID userId);
    boolean existsByUsernameIgnoreCase(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.password = :password, u.rowVersion = u.rowVersion + 1 where u.emailKey = :emailKey "
            + "and (u.password is null or trim(u.password) = '')")
    int setInitialPassword(@Param("emailKey") String emailKey, @Param("password") String encodedPassword);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findForAuthentication(@Param("id") UUID id);
}

