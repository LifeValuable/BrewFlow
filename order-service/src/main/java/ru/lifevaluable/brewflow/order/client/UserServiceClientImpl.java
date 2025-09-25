package ru.lifevaluable.brewflow.order.client;

import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ru.lifevaluable.brewflow.order.dto.UserData;
import ru.lifevaluable.brewflow.order.exception.InvalidTokenException;
import ru.lifevaluable.brewflow.order.exception.UserServiceException;
import ru.lifevaluable.brewflow.order.exception.UserServiceUnavailableException;


@Component
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {
    private final RestClient restClient;

    @Retryable(
        retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
        noRetryFor = {HttpClientErrorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000L, multiplier = 2)
    )
    @Override
    public UserData getUserByToken(String jwtToken) throws UserServiceException {
        try {
            return restClient
                    .get()
                    .uri("/users/profile")
                    .header("Authorization", ensureBearerPrefix(jwtToken))
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

    private String ensureBearerPrefix(String token) {
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }

}
