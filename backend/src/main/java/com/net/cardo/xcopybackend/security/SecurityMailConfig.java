package com.net.cardo.xcopybackend.security;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration @EnableAsync
public class SecurityMailConfig implements AsyncConfigurer {
    @Bean("securityMailExecutor") public Executor securityMailExecutor() {
        var executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);executor.setMaxPoolSize(2);executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("security-mail-");return executor;
    }
    @Override public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (error,method,args)->org.slf4j.LoggerFactory.getLogger(SecurityMailConfig.class)
                .warn("Password recovery request could not be completed");
    }
}
