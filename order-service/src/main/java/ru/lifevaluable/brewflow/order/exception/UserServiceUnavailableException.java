package ru.lifevaluable.brewflow.order.exception;

public class UserServiceUnavailableException extends UserServiceException {
    public UserServiceUnavailableException(String message) {
        super(message);
    }

    public UserServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
