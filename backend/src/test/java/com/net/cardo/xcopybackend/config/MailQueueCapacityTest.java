package com.net.cardo.xcopybackend.config;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import static org.junit.jupiter.api.Assertions.*;

class MailQueueCapacityTest {
    @Test void executorRejectsAfterTwoWorkersAndSixteenQueuedTasks() throws Exception {
        var executor=(ThreadPoolTaskExecutor)new com.net.cardo.xcopybackend.security.SecurityMailConfig().securityMailExecutor();
        executor.initialize();var entered=new CountDownLatch(2);var release=new CountDownLatch(1);
        Runnable waiting=()->{entered.countDown();try {release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}};
        try {
            executor.execute(waiting);executor.execute(waiting);assertTrue(entered.await(5,TimeUnit.SECONDS));
            for(int i=0;i<16;i++)executor.execute(waiting);
            assertThrows(org.springframework.core.task.TaskRejectedException.class,()->executor.execute(waiting));
        } finally {release.countDown();executor.shutdown();}
    }
}
