package com.net.cardo.xcopybackend.post.repository;
import java.time.Instant;

public interface PostDeletionRepository {
    void requestDeletion(String id,String owner,Instant now);
}
