package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Like;
import com.net.cardo.xcopybackend.post.repository.LikeRepository;
import com.net.cardo.xcopybackend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final FeedCacheService feedCacheService;

    public boolean toggleLike(String postId, String userId) {

        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }

        boolean alreadyLiked =
                likeRepository.existsByPostIdAndUserId(
                        postId,
                        userId
                );

        if (alreadyLiked) {

            likeRepository.deleteByPostIdAndUserId(
                    postId,
                    userId
            );
            feedCacheService.invalidate();

            return false;
        }

        Like like = new Like();

        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());

        likeRepository.save(like);
        feedCacheService.invalidate();

        return true;
    }
}
