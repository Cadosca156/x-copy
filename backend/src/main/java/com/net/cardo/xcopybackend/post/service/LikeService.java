package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.entity.Like;
import com.net.cardo.xcopybackend.post.repository.LikeRepository;
import com.net.cardo.xcopybackend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    public boolean toggleLike(String postId, String userId) {

        if (!postRepository.existsActiveById(postId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Допис не знайдено");
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


            return false;
        }

        Like like = new Like();

        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());

        try {
            likeRepository.save(like);
        } catch (DuplicateKeyException concurrentLike) {
            // Both requests can observe no like; the unique pair index chooses one insert.
            if (!likeRepository.existsByPostIdAndUserId(postId, userId)) throw concurrentLike;
        }


        if(!postRepository.existsActiveById(postId)) {
            likeRepository.deleteByPostIdAndUserId(postId,userId);
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Допис видалено");
        }
        return true;
    }
}
