package ru.lifevaluable.brewflow.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.lifevaluable.brewflow.user.dto.*;
import ru.lifevaluable.brewflow.user.entity.User;
import ru.lifevaluable.brewflow.user.exception.InvalidCredentialsException;
import ru.lifevaluable.brewflow.user.exception.UserAlreadyRegisteredException;
import ru.lifevaluable.brewflow.user.exception.UserNotFoundException;
import ru.lifevaluable.brewflow.user.mapper.UserMapper;
import ru.lifevaluable.brewflow.user.repository.UserRepository;
import ru.lifevaluable.brewflow.user.security.JwtUtil;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("register() должен успешно создать пользователя, если email свободен")
    void register_WhenEmailIsUnique_ShouldCreateUser() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "12345678", "Ivan", "Ivanov"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });


        RegisterResponse actualResponse = userService.register(request);


        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.email())
                .isEqualTo(request.email());
        assertThat(actualResponse.firstName())
                .isEqualTo(request.firstName());
        assertThat(actualResponse.id()).isNotNull();

        verify(userRepository).save(argThat(user ->
            user.getPassword().equals("encoded_password")
        ));
    }

    @Test
    @DisplayName("register() должен выбросить исключение, если email занят")
    void register_WhenEmailIsBusy_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "12345678", "Ivan", "Ivanov"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserAlreadyRegisteredException.class)
                .hasMessageContaining(request.email());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login() должен успешно вернуть токен, если верны логин и пароль")
    void login_WhenCredentialsAreValid_ShouldReturnToken () {
        LoginRequest request = new LoginRequest("test@example.com", "12345678");
        User user = new User();
        user.setEmail(request.email());
        user.setId(UUID.randomUUID());
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("token");


        LoginResponse actualResponse = userService.login(request);


        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.id()).isNotNull();
        assertThat(actualResponse.jwtToken()).isEqualTo("token");
    }

    @Test
    @DisplayName("login() должен выбросить исключение, если пользователь с email не найден")
    void login_WhenUserIsNotRegistered_ShouldThrowException() {
        LoginRequest request = new LoginRequest("test@example.com", "12345678");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login((request)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("login() должен выбросить исключение при неверном пароле")
    void login_WhenPasswordIsInvalid_ShouldThrowException() {
        LoginRequest request = new LoginRequest("test@example.com", "12345678");
        User user = new User();
        user.setPassword("another_password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("getProfile() должен вернуть профиль, если email используется")
    void getProfile_WhenUserExistsByEmail_ShouldReturnProfile() {
        User user = new User();
        user.setEmail("test@example.com");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserProfileResponse actualResponse = userService.getProfile(user.getEmail());

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.email()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("getProfile() должен выбросить исключение, если пользователь не найден")
    void getProfile_WhenEmailIsInvalid_ShouldThrowException() {
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(email))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(email);
    }

    @Test
    @DisplayName("getProfile() должен вернуть профиль, если id используется")
    void getProfile_WhenUserExistsById_ShouldReturnProfile() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserProfileResponse actualResponse = userService.getProfile(user.getId());

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.id()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("getProfile() должен выбросить исключение, если id не найден")
    void getProfile_WhenIdIsInvalid_ShouldThrowException() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
