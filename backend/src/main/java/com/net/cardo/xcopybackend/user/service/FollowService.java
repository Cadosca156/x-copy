package com.net.cardo.xcopybackend.user.service;

import com.net.cardo.xcopybackend.user.dto.response.SuggestedUserResponse;
import com.net.cardo.xcopybackend.user.repository.UserFollowRepository;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final UserFollowRepository follows;
    private final UserRepository users;

    public List<UUID> followingIds(UUID viewer) {
        return follows.findFollowingIds(viewer);
    }

    public List<SuggestedUserResponse> suggestions(UUID viewer) {
        Set<UUID> following = new HashSet<>(followingIds(viewer));
        return users.findFollowSuggestions(viewer, PageRequest.of(0, 3)).stream()
                .map(user -> new SuggestedUserResponse(user.getId(), user.getUsername(),
                        user.getDisplayName(), user.getAvatarUrl(), following.contains(user.getId())))
                .toList();
    }

    @Transactional
    public void follow(UUID viewer, UUID target) {
        if (viewer.equals(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не можна підписатися на себе");
        }
        if (!users.existsById(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Користувача не знайдено");
        }
        follows.addFollow(viewer + ":" + target, viewer, target);
    }

    @Transactional
    public void unfollow(UUID viewer, UUID target) {
        follows.deleteByFollowerIdAndFollowedId(viewer, target);
    }
}
