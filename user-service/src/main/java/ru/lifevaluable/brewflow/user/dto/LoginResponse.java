package ru.lifevaluable.brewflow.user.dto;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String jwtToken
) {
}
