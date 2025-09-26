package ru.lifevaluable.brewflow.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;
import ru.lifevaluable.brewflow.user.dto.RegisterRequest;
import ru.lifevaluable.brewflow.user.dto.RegisterResponse;
import ru.lifevaluable.brewflow.user.dto.UserProfileResponse;
import ru.lifevaluable.brewflow.user.entity.User;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toUser(RegisterRequest request);
    RegisterResponse toRegisterResponse(User user);
    UserProfileResponse toUserProfile(User user);
}
