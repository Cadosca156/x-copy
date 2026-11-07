package com.net.cardo.xcopybackend.post.repository;
import com.net.cardo.xcopybackend.post.entity.Post;
import com.net.cardo.xcopybackend.post.service.FeedCursor;
import java.util.List;

public interface PostWindowRepository {
    List<Post> findWindow(String author,List<String> authors,FeedCursor.Position before,int limit);
}
