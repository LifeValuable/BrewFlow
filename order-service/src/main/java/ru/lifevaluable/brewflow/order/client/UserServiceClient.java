package ru.lifevaluable.brewflow.order.client;

import ru.lifevaluable.brewflow.order.dto.UserData;
import ru.lifevaluable.brewflow.order.exception.UserServiceException;

public interface UserServiceClient {
    UserData getUserByToken(String jwtToken) throws UserServiceException;
}