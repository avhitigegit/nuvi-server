package com.nuvi.online_renting.users.serviceImpl;


import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.users.dto.UserProfileRequest;
import com.nuvi.online_renting.users.dto.UserProfileResponse;
import com.nuvi.online_renting.users.dto.UserRequestDTO;
import com.nuvi.online_renting.users.dto.UserResponseDTO;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import com.nuvi.online_renting.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    // Implementation of UserService methods would go here

    @Autowired
    private UserRepository userRepository;
    private AuthenticationFacade authenticationFacade;


    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // ⚠️ should be encoded if used outside /auth
        user.setRole(Role.USER);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponseDTO(user);
    }


    @Override
    @Transactional
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        User updated = userRepository.save(user);
        return mapToResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }

    @Override
    public UserProfileResponse getMyProfile() {
        User user = authenticationFacade.getCurrentUser();
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UserProfileRequest userProfileRequest) {
        User user = authenticationFacade.getCurrentUser();
        user.setName(userProfileRequest.getName());
        user.setPhone(userProfileRequest.getPhone());
        user.setAddress(userProfileRequest.getAddress());
        user.setNicNumber(userProfileRequest.getNicNumber());
        user.setProfilePictureUrl(userProfileRequest.getProfilePictureUrl());
        userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public void deactivateMyAccount() {
        User user = authenticationFacade.getCurrentUser();
        user.setEnabled(false);
        userRepository.save(user);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setName(user.getName());
        userProfileResponse.setEmail(user.getEmail());
        userProfileResponse.setPhone(user.getPhone());
        userProfileResponse.setAddress(user.getAddress());
        userProfileResponse.setNicNumber(user.getNicNumber());
        userProfileResponse.setProfilePictureUrl(user.getProfilePictureUrl());
        userProfileResponse.setKycVerified(user.isKycVerified());
        return userProfileResponse;
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(user.getId());
        userResponseDTO.setName(user.getName());
        userResponseDTO.setEmail(user.getEmail());
        userResponseDTO.setRole(user.getRole());
        userResponseDTO.setEnabled(user.isEnabled());
        userResponseDTO.setCreatedAt(user.getCreatedAt());
        userResponseDTO.setUpdatedAt(user.getUpdatedAt());
        userResponseDTO.setCreatedBy(user.getCreatedBy());
        userResponseDTO.setUpdatedBy(user.getUpdatedBy());
        return userResponseDTO;
    }
}
