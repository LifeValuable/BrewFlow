package ru.lifevaluable.brewflow.user.exception;

public class UserAlreadyRegisteredException extends RuntimeException {
    public UserAlreadyRegisteredException(String email) {
        super(String.format("User is already registered with email %s", email));
    }
}
