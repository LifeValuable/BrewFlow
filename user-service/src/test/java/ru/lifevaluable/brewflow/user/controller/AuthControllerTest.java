package ru.lifevaluable.brewflow.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.lifevaluable.brewflow.user.dto.LoginRequest;
import ru.lifevaluable.brewflow.user.dto.LoginResponse;
import ru.lifevaluable.brewflow.user.dto.RegisterRequest;
import ru.lifevaluable.brewflow.user.dto.RegisterResponse;
import ru.lifevaluable.brewflow.user.exception.GlobalExceptionHandler;
import ru.lifevaluable.brewflow.user.exception.InvalidCredentialsException;
import ru.lifevaluable.brewflow.user.exception.UserAlreadyRegisteredException;
import ru.lifevaluable.brewflow.user.service.UserService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 201 и сгенерированный id")
    public void register_WhenEmailIsUnique_ShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "12345678",
                "Ivan",
                "Ivanov"
        );

        RegisterResponse response = new RegisterResponse(
                UUID.randomUUID(),
                request.email(),
                request.firstName(),
                request.lastName(),
                LocalDateTime.now()
        );

        when(userService.register(request)).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(request.lastName()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.jwtToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 409, если пользователь уже зарегистрирован")
    public void register_WhenEmailIsBusy_ShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "busy@email.com",
                "12345678",
                "Ivan",
                "Ivanov"
        );

        when(userService.register(request)).thenThrow(new UserAlreadyRegisteredException(request.email()));

        mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(containsString(request.email())));
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 400, если email в неправильном формате")
    public void register_WhenEmailIsInvalid_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "invalid_email.com",
                "12345678",
                "Ivan",
                "Ivanov"
        );

        mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("email")));

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 400, если email пустой")
    public void register_WhenEmailIsBlank_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                " ",
                "12345678",
                "Ivan",
                "Ivanov"
        );

        mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("email")));
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 400, если password пустой")
    public void register_WhenPasswordIsBlank_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                " ",
                "Ivan",
                "Ivanov"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("password")));
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 400, если password короче 8 символов")
    public void register_WhenPasswordIsShort_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "1234567",
                "Ivan",
                "Ivanov"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("password")));
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 400, если имя пустое")
    public void register_WhenFirstNameIsBlank_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "12345679",
                " ",
                "Ivanov"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("firstName")));
    }

    @Test
    @DisplayName("POST /auth/register должен вернуть 400, если фамилия пустая")
    public void register_WhenLastNameIsBlank_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "12345679",
                "Ivan",
                " "
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("lastName")));
    }

    @Test
    @DisplayName("POST /auth/login должен вернуть 200 и токен")
    public void login_WhenCredentialsAreValid_ShouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "12345678"
        );

        LoginResponse response = new LoginResponse(
                UUID.randomUUID(),
                "jwtToken"
        );

        when(userService.login(request)).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jwtToken").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/login должен вернуть 401 при неверном логине или пароле")
    public void login_WhenCredentialsAreInvalid_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest(
                "invalid@email.com",
                "wrongPassword"
        );

        when(userService.login(request)).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIAL"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login должен вернуть 400, если email в неправильном формате")
    public void login_WhenEmailIsInvalid_ShouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest(
                "ivalid_email.com",
                "12345678"
        );

        mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("email")));

        verify(userService, never()).login(any());
    }

    @Test
    @DisplayName("POST /auth/login должен вернуть 400, если email пустой")
    public void login_WhenEmailIsBlank_ShouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest(
                " ",
                "12345678"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("email")));

        verify(userService, never()).login(any());
    }

    @Test
    @DisplayName("POST /auth/login должен вернуть 400, если пароль пустой")
    public void login_WhenPasswordIsBlank_ShouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                " "
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("password")));

        verify(userService, never()).login(any());
    }
}
