package ru.lifevaluable.brewflow.user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super(String.format("User was not found with email %s", email));
    }
}
