package com.net.cardo.xcopybackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;

@Configuration
public class MediaWebConfig implements WebMvcConfigurer {
    private final String location;

    public MediaWebConfig(@Value("${app.media.upload-dir:uploads/posts}") String directory) {
        location = Path.of(directory).toAbsolutePath().normalize().toUri() + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/posts/**").addResourceLocations(location);
    }
}
