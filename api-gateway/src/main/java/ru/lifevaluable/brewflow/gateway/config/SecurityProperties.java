package ru.lifevaluable.brewflow.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
@Data
@Component
public class SecurityProperties {
    private List<String> publicPaths;
}
