package com.spendwise.mapper;

import com.spendwise.domain.entity.User;
import com.spendwise.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for User entity to UserResponse DTO.
 * No business logic; pure field mapping with enum-to-string conversion for role.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    UserResponse toUserResponse(User user);
}
