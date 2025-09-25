package ru.lifevaluable.brewflow.order.exception;

public class InvalidTokenException extends UserServiceException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
