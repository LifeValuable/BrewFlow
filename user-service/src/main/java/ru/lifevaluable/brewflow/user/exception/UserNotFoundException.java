package ru.lifevaluable.brewflow.user.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super(String.format("User was not found with email %s", email));
    }

    public UserNotFoundException(UUID id) {
        super(String.format("User was not found with id %s", id.toString()));
    }
}
