package ru.lifevaluable.brewflow.order.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import ru.lifevaluable.brewflow.order.dto.UserData;
import ru.lifevaluable.brewflow.order.exception.InvalidTokenException;
import ru.lifevaluable.brewflow.order.exception.UserServiceException;
import ru.lifevaluable.brewflow.order.exception.UserServiceUnavailableException;

import java.util.UUID;


@Component
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {
    private final RestClient restClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @Retry(name = "userService")
    @Override
    public UserData getUser(UUID userId) throws UserServiceException {
        try {
            return restClient
                    .get()
                    .uri("/internal/users/" + userId.toString())
                    .retrieve()
                    .body(UserData.class);

        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new InvalidTokenException("Invalid or expired token");

        } catch (HttpClientErrorException ex) {
            throw new UserServiceException("User service client error: " + ex.getMessage());

        } catch (ResourceAccessException | HttpServerErrorException ex) {
            throw new UserServiceUnavailableException("User service is unavailable", ex);
        }
    }

    private UserData getUserFallback(UUID userId, Throwable throwable) {
        throw new UserServiceUnavailableException("User service is temporarily unavailable. Please try again later.");
    }

}
