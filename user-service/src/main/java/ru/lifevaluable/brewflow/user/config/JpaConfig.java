package ru.lifevaluable.brewflow.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "ru.lifevaluable.brewflow.user.repository")
public class JpaConfig {
}
