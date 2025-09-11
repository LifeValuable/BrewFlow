package ru.lifevaluable.brewflow.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.lifevaluable.brewflow.user.dto.LoginRequest;
import ru.lifevaluable.brewflow.user.dto.LoginResponse;
import ru.lifevaluable.brewflow.user.dto.RegisterRequest;
import ru.lifevaluable.brewflow.user.dto.RegisterResponse;
import ru.lifevaluable.brewflow.user.service.UserService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody @Valid RegisterRequest request) {

        RegisterResponse response = userService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest request) {

        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}

