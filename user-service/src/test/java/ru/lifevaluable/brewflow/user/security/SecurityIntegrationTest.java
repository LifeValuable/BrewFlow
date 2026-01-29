package ru.lifevaluable.brewflow.user.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // НОВЫЙ ИМПОРТ
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.lifevaluable.brewflow.user.config.SecurityConfig;
import ru.lifevaluable.brewflow.user.controller.UserController;
import ru.lifevaluable.brewflow.user.dto.UserProfileResponse;
import ru.lifevaluable.brewflow.user.service.UserService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class) // Явно укажи ОДИН контроллер
@Import(SecurityConfig.class)
public class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // Мокаем сам фильтр


    @ParameterizedTest
    @ValueSource(strings = {"/auth/login", "/auth/register", "/actuator/health"})
    @DisplayName("Публичные эндпоинты должны быть доступны всем")
    void publicEndpoints_ShouldBeAccessible(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is(not(HttpStatus.UNAUTHORIZED.value())))
                .andExpect(status().is(not(HttpStatus.FORBIDDEN.value())));
    }

    @Test
    @DisplayName("GET /users/profile должен вернуть 401, если отсутствуют заголовки аутентификации")
    void getProfile_WhenAuthenticationHeadersMissing_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/users/profile")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/profile должен вернуть 200 и для роли админа")
    void getProfile_WhenRoleIsAdmin_ShouldReturn200() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                UUID.randomUUID(),
                "admin@example.com",
                "Admin",
                "Adminov",
                LocalDateTime.now()
        );

        when(userService.getProfile(response.email())).thenReturn(response);

        mockMvc.perform(get("/users/profile")
                        .header("X-User-Email", response.email())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(response.email()));
    }
}
