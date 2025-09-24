package ru.lifevaluable.brewflow.user.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new UserAlreadyRegisteredException(request.email());
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        return userMapper.toRegisterResponse(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new InvalidCredentialsException();
        return new LoginResponse(user.getId(), jwtUtil.generateToken(user.getId().toString(), user.getEmail()));
    }

    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toUserProfile(user);
    }
}
