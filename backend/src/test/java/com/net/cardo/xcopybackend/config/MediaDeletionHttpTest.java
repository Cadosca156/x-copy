package com.net.cardo.xcopybackend.config;

import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import java.nio.file.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MediaDeletionHttpTest {
    @TempDir Path root;
    @TestConfiguration @EnableWebMvc static class Mvc {}
    GenericWebApplicationContext context(WebMvcConfigurer config) {
        var context=new GenericWebApplicationContext(new MockServletContext());
        new AnnotatedBeanDefinitionReader(context).register(Mvc.class);
        context.registerBean("mediaConfigurer",WebMvcConfigurer.class,()->config);
        context.registerBean(GlobalExceptionHandler.class,GlobalExceptionHandler::new);
        context.refresh();return context;
    }
    @Test void previouslyServedMediaReturns404AfterDeletion() throws Exception {
        Path file=root.resolve("image.png");Files.writeString(file,"png");
        try(var context=context(new MediaWebConfig(root.toString()))) {
            var mvc=MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(get("/media/posts/image.png")).andExpect(status().isOk());
            Files.delete(file);
            mvc.perform(get("/media/posts/image.png")).andExpect(status().isNotFound());
        }
    }
    @Test void disappearingFileBetweenResolutionAndReadIs404InsteadOf500() throws Exception {
        Path file=root.resolve("race.png");Files.writeString(file,"png");
        var config=new WebMvcConfigurer(){
            @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/media/posts/**").addResourceLocations(root.toUri().toString()+"/")
                        .resourceChain(false).addResolver(new PathResourceResolver(){
                            @Override protected Resource getResource(String path,Resource location) throws IOException {
                                Resource resource=super.getResource(path,location);
                                if(resource!=null)Files.delete(file);
                                return resource;
                            }
                        });
            }
        };
        try(var context=context(config)) {
            MockMvcBuilders.webAppContextSetup(context).build().perform(get("/media/posts/race.png"))
                    .andExpect(status().isNotFound()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(root.toString()))));
        }
    }
}
