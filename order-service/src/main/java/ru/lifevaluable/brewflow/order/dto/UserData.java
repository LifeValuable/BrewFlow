package ru.lifevaluable.brewflow.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserData(
        @NotNull
        UUID id,
        @Email
        String email,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName
) {}
