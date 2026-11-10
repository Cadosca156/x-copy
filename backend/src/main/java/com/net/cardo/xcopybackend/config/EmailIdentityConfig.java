package com.net.cardo.xcopybackend.config;

import java.util.LinkedHashSet;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailIdentityConfig {
    @Bean(initMethod = "migrate")
    public EmailIdentityMigration emailIdentityMigration(DataSource dataSource) {
        return new EmailIdentityMigration(dataSource);
    }

    @Bean
    public static BeanFactoryPostProcessor emailMigrationBeforeHibernate() {
        return beans -> {
            if (!beans.containsBeanDefinition("entityManagerFactory")) return;
            var definition = beans.getBeanDefinition("entityManagerFactory");
            var dependencies = new LinkedHashSet<String>();
            if (definition.getDependsOn() != null) dependencies.addAll(List.of(definition.getDependsOn()));
            dependencies.add("emailIdentityMigration");
            definition.setDependsOn(dependencies.toArray(String[]::new));
        };
    }
}
