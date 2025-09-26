package ru.lifevaluable.brewflow.order.client;

import ru.lifevaluable.brewflow.order.dto.UserData;
import ru.lifevaluable.brewflow.order.exception.UserServiceException;

import java.util.UUID;

public interface UserServiceClient {
    UserData getUser(UUID userId) throws UserServiceException;
}