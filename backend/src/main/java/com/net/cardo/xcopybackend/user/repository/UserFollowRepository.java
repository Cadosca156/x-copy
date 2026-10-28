package com.net.cardo.xcopybackend.user.repository;

import com.net.cardo.xcopybackend.user.entity.UserFollow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface UserFollowRepository extends JpaRepository<UserFollow, String> {
    @Query("select f.followedId from UserFollow f where f.followerId = :id")
    List<UUID> findFollowingIds(@Param("id") UUID followerId);

    @Modifying
    @Query(value = "insert into user_follows (id, follower_id, followed_id) values (:id, :follower, :followed) on conflict do nothing", nativeQuery = true)
    void addFollow(@Param("id") String id, @Param("follower") UUID follower, @Param("followed") UUID followed);

    void deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
}
