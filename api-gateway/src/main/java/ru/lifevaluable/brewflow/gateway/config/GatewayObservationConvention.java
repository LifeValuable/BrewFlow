package ru.lifevaluable.brewflow.gateway.config;

import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

@Component
public class GatewayObservationConvention extends DefaultServerRequestObservationConvention {

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        KeyValues keyValues = super.getLowCardinalityKeyValues(context);
        
        // Получаем URI из request
        String path = context.getCarrier().getURI().getPath();
        
        // Нормализуем путь для метрик
        String normalizedUri = normalizePath(path);
        
        // Заменяем стандартный тег uri на нормализованный
        return keyValues.and(KeyValue.of("uri", normalizedUri));
    }
    
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // Группируем пути по паттернам
        if (path.startsWith("/api/users/")) {
            return "/api/users/{id}";
        }
        if (path.equals("/api/users")) {
            return "/api/users";
        }
        
        if (path.startsWith("/api/orders/")) {
            return "/api/orders/{id}";
        }
        if (path.equals("/api/orders")) {
            return "/api/orders";
        }
        
        if (path.startsWith("/api/cart")) {
            return "/api/cart";
        }
        
        if (path.startsWith("/api/products/")) {
            return "/api/products/{id}";
        }
        if (path.equals("/api/products")) {
            return "/api/products";
        }
        
        if (path.startsWith("/api/payments/")) {
            return "/api/payments/{id}";
        }
        if (path.equals("/api/payments")) {
            return "/api/payments";
        }
        
        if (path.startsWith("/api/auth")) {
            return "/api/auth";
        }
        
        if (path.startsWith("/actuator/")) {
            // Для actuator оставляем детализацию
            return path;
        }
        
        // Для всех остальных
        return extractFirstSegment(path);
    }
    
    private String extractFirstSegment(String path) {
        if (path.equals("/")) {
            return "/";
        }
        
        String[] segments = path.split("/");
        if (segments.length > 1) {
            return "/" + segments[1];
        }
        
        return path;
    }
}
