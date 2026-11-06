package com.net.cardo.xcopybackend.post.service;

import com.net.cardo.xcopybackend.post.dto.response.LikeState;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    public LikeState setLiked(String postId,String userId,boolean desired) {
        if(desired) {
            if(!postRepository.existsActiveById(postId))throw missing();
            try {likeRepository.ensureLiked(postId,userId,LocalDateTime.now());}
            catch(DuplicateKeyException concurrentInsert) {
                if(!likeRepository.existsByPostIdAndUserId(postId,userId))throw concurrentInsert;
            }
            if(!postRepository.existsActiveById(postId)) {
                likeRepository.deleteByPostIdAndUserId(postId,userId);
                throw missing();
            }
        } else {
            likeRepository.deleteByPostIdAndUserId(postId,userId);
            if(!postRepository.existsActiveById(postId))
                return new LikeState(false,0);
        }
        return new LikeState(desired,likeRepository.countByPostId(postId));
    }
    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Допис не знайдено");
    }

    /** Compatibility only; callers needing safe retries must use setLiked. */
    @Deprecated
    public boolean toggleLike(String postId, String userId) {

        if (!postRepository.existsActiveById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Допис не знайдено");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Допис видалено");
        }
        return true;
    }
}
