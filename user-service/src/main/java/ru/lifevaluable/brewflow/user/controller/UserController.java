package ru.lifevaluable.brewflow.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.lifevaluable.brewflow.user.dto.*;
import ru.lifevaluable.brewflow.user.service.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            Authentication authentication) {

        String email = authentication.getName();
        UserProfileResponse response = userService.getProfile(email);
        return ResponseEntity.ok(response);
    }
}
