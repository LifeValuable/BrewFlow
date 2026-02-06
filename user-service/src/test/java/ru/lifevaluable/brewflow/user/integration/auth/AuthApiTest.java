package ru.lifevaluable.brewflow.user.integration.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.lifevaluable.brewflow.user.dto.LoginRequest;
import ru.lifevaluable.brewflow.user.dto.LoginResponse;
import ru.lifevaluable.brewflow.user.dto.RegisterRequest;
import ru.lifevaluable.brewflow.user.dto.RegisterResponse;
import ru.lifevaluable.brewflow.user.entity.User;
import ru.lifevaluable.brewflow.user.integration.common.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /auth/register должен создать пользователя в БД и вернуть 201")
    public void register_ShouldCreateUser() {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com", "12345678", "Ivan", "Ivanov"
        );

        ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(
                "/auth/register", request, RegisterResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(request.email());

        assertThat(userRepository.existsByEmail(request.email())).isTrue();
    }

    @Test
    @DisplayName("POST /auth/register должен не сохранять пользователя с дублирующейся почтой и вернуть 409")
    public void register_WhenEmailIsBusy_ShouldReturn409() {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com", "12345678", "Ivan", "Ivan"
        );
        User user = createUser(request.email(), request.password());
        userRepository.save(user);

        ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(
                "/auth/register", request, RegisterResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
    }
    
    @Test
    @DisplayName("POST /auth/register должен не пропустить невалидную почту и вернуть 400")
    public void register_WhenEmailIsInvalid_ShouldReturn400() {
        RegisterRequest request = new RegisterRequest(
                "ivalid_email", "12345678", "Ivan", "Ivan"
        );

        ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(
                "/auth/register", request, RegisterResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("POST /auth/login должен вернуть JWT при валидных данных")
    public void login_WhenCredentialsIsValid_ShouldReturnJwt() {
        LoginRequest request = new LoginRequest(
                "newuser@example.com", "12345678"
        );
        User user = createUser(request.email(), request.password());
        User savedUser = userRepository.save(user);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/auth/login", request, LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().jwtToken()).isNotBlank();
        assertThat(response.getBody().id()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("POST /auth/login должен вернуть 401 при неверных данных")
    public void login_WhenCredentialsIsInvalid_ShouldReturn401() {
        LoginRequest request = new LoginRequest(
                "newuser@example.com", "12345678"
        );
        User user = createUser(request.email(), request.password() + "9");
        userRepository.save(user);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/auth/login", request, LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
    }
}
