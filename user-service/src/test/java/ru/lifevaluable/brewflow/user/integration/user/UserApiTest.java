package ru.lifevaluable.brewflow.user.integration.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.lifevaluable.brewflow.user.dto.UserProfileResponse;
import ru.lifevaluable.brewflow.user.entity.User;
import ru.lifevaluable.brewflow.user.integration.common.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class UserApiTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /users/profile должен вернуть профиль для существующего пользователя")
    public void getProfile_WhenHeadersArePresent_ShouldReturnProfile() {
        User user = createUser("user@example.com", "12345678");
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Email", user.getEmail());
        headers.add("X-User-Role", "USER");

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<UserProfileResponse> response = restTemplate.exchange(
                "/users/profile",
                HttpMethod.GET,
                requestEntity,
                UserProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("GET /users/profile должен вернуть 401, если заголовков нет")
    public void getProfile_WhenHeadersAreMissed_ShouldReturn401() {
        ResponseEntity<UserProfileResponse> response = restTemplate.getForEntity(
                "/users/profile",
                UserProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /users/profile должен вернуть профиль для существующего пользователя")
    public void getProfile_WhenUserIsNotFound_Should404() {
        User user = createUser("user@example.com", "12345678");
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Email", "another" + user.getEmail());
        headers.add("X-User-Role", "USER");

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<UserProfileResponse> response = restTemplate.exchange(
                "/users/profile",
                HttpMethod.GET,
                requestEntity,
                UserProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
    }
}
