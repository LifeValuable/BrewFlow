package ru.lifevaluable.brewflow.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.lifevaluable.brewflow.user.dto.UserProfileResponse;
import ru.lifevaluable.brewflow.user.exception.GlobalExceptionHandler;
import ru.lifevaluable.brewflow.user.exception.UserNotFoundException;
import ru.lifevaluable.brewflow.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication createAuthentication(String email) {
        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("GET /users/profile должен вернуть 200 и данные пользователя")
    void getProfile_WhenAuthenticationHeadersPresent_ShouldReturn200() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Ivan",
                "Ivanov",
                LocalDateTime.now()
        );

        when(userService.getProfile(response.email())).thenReturn(response);

        mockMvc.perform(get("/users/profile")
                    .principal(createAuthentication(response.email()))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(response.email()));
    }

//    @Test
//    @DisplayName("GET /users/profile должен вернуть 401, если отсутствуют заголовки аутентификации")
//    void getProfile_WhenAuthenticationHeadersMissing_ShouldReturn401() throws Exception {
//        mockMvc.perform(get("/users/profile")
//                    .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isUnauthorized());
//    }

    @Test
    @DisplayName("GET /user/profile должен вернуть 404, если пользователь не найден")
    void getProfile_WhenUserNotFound_ShouldReturn404() throws Exception {
        String email = "nonexistent@example.com";

        when(userService.getProfile(email)).thenThrow(new UserNotFoundException(email));

        mockMvc.perform(get("/users/profile")
                        .principal(createAuthentication(email))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString(email)));
    }

//    @Test
//    @DisplayName("GET /users/profile должен вернуть 200 и для роли админа")
//    void getProfile_WhenRoleIsAdmin_ShouldReturn200() throws Exception {
//        UserProfileResponse response = new UserProfileResponse(
//                UUID.randomUUID(),
//                "admin@example.com",
//                "Admin",
//                "Adminov",
//                LocalDateTime.now()
//        );
//
//        when(userService.getProfile(response.email())).thenReturn(response);
//
//        mockMvc.perform(get("/users/profile")
//                        .header("X-User-Email", response.email())
//                        .header("X-User-Role", "ADMIN")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email").value(response.email()));
//    }
}
