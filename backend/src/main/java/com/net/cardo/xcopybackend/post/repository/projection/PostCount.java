package com.net.cardo.xcopybackend.post.repository.projection;

/** Aggregated count for one post; contains no individual like/comment documents. */
public record PostCount(String postId, long count) {}
