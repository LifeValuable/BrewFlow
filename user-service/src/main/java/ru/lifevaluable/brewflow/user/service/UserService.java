package ru.lifevaluable.brewflow.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.lifevaluable.brewflow.user.dto.*;
import ru.lifevaluable.brewflow.user.entity.User;
import ru.lifevaluable.brewflow.user.exception.InvalidCredentialsException;
import ru.lifevaluable.brewflow.user.exception.UserAlreadyRegisteredException;
import ru.lifevaluable.brewflow.user.exception.UserNotFoundException;
import ru.lifevaluable.brewflow.user.mapper.UserMapper;
import ru.lifevaluable.brewflow.user.repository.UserRepository;
import ru.lifevaluable.brewflow.user.security.JwtUtil;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.debug("Start registration for user with email {}", request.email());
        if (userRepository.existsByEmail(request.email()))
            throw new UserAlreadyRegisteredException(request.email());
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with email {}", user.getEmail());
        return userMapper.toRegisterResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        log.debug("Start login user with email {}", request.email());
        User user = userRepository.findByEmail(request.email()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new InvalidCredentialsException();
        log.debug("Generating jwt token for user with email {}", user.getEmail());
        String token = jwtUtil.generateToken(user);
        log.info("Login user with email {}", user.getEmail());
        return new LoginResponse(user.getId(), token);
    }

    public UserProfileResponse getProfile(String email) {
        log.debug("Find user profile by email {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toUserProfile(user);
    }

    public UserProfileResponse getProfile(UUID id) {
        log.debug("Find user profile by id {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toUserProfile(user);
    }
}
