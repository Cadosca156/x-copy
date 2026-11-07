package com.net.cardo.xcopybackend.config;
import com.net.cardo.xcopybackend.post.service.PostCleanupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="app.post-cleanup.scheduling-enabled",havingValue="true",matchIfMissing=true)
public class PostCleanupScheduler {
    private final PostCleanupService cleanup;
    public PostCleanupScheduler(PostCleanupService cleanup){this.cleanup=cleanup;}
    @Scheduled(fixedDelayString="${app.post-cleanup.delay-ms:5000}",initialDelayString="${app.post-cleanup.initial-delay-ms:5000}")
    public void tick(){cleanup.runBatch();}
}
