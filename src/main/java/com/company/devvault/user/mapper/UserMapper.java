package com.company.devvault.user.mapper;

import com.company.devvault.auth.dto.UserDto;
import com.company.devvault.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setProfileImage(user.getProfileImage());
        dto.setDepartment(user.getDepartment());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}