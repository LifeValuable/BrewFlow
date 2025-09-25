package ru.lifevaluable.brewflow.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "user-service")
public class UserServiceProperties {
    private String baseUrl;
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}
