package com.nuvi.online_renting.users.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.users.dto.UserProfileRequest;
import com.nuvi.online_renting.users.dto.UserProfileResponse;
import com.nuvi.online_renting.users.dto.UserRequestDTO;
import com.nuvi.online_renting.users.dto.UserResponseDTO;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponseDTO createUser(UserRequestDTO dto);

    UserResponseDTO getUserById(Long id);

    PagedResponse<UserResponseDTO> getAllUsers(String name, Role role, Boolean enabled, Pageable pageable);

    UserResponseDTO updateUser(Long id, UserRequestDTO dto);

    void deleteUser(Long id);

    UserProfileResponse getMyProfile();

    UserProfileResponse updateMyProfile(UserProfileRequest userProfileRequest);

    void deactivateMyAccount();
}
