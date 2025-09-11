package ru.lifevaluable.brewflow.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        LocalDateTime createdAt
) {
}
