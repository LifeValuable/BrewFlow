package ru.lifevaluable.brewflow.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.lifevaluable.brewflow.user.dto.*;
import ru.lifevaluable.brewflow.user.service.UserService;

@Tag(name = "Профиль", description = "Управление профилем пользователя")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить профиль пользователя")
    @ApiResponse(responseCode = "200", description = "Данные профиля")
    @ApiResponse(responseCode = "401", description = "Токен отсутствует или недействителен")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            Authentication authentication) {

        String email = authentication.getName();
        UserProfileResponse response = userService.getProfile(email);
        return ResponseEntity.ok(response);
    }
}
