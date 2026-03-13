package com.nuvi.online_renting.users.serviceImpl;

import com.nuvi.online_renting.common.dto.PagedResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final AuthenticationFacade authenticationFacade;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           AuthenticationFacade authenticationFacade,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationFacade = authenticationFacade;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);
        return mapToResponseDTO(userRepository.save(user));
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
    public PagedResponse<UserResponseDTO> getAllUsers(String name, Role role, Boolean enabled, Pageable pageable) {
        Page<User> page = userRepository.filterUsers(name, role, enabled, pageable);
        return new PagedResponse<>(page.map(this::mapToResponseDTO));
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
        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User {} soft-deleted", id);
    }

    @Override
    public UserProfileResponse getMyProfile() {
        return mapToProfileResponse(authenticationFacade.getCurrentUser());
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UserProfileRequest req) {
        User user = authenticationFacade.getCurrentUser();
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        user.setNicNumber(req.getNicNumber());
        user.setProfilePictureUrl(req.getProfilePictureUrl());
        return mapToProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deactivateMyAccount() {
        User user = authenticationFacade.getCurrentUser();
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateKycStatus(Long userId, boolean kycVerified) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setKycVerified(kycVerified);
        log.info("KYC status for user {} manually set to {} by admin", userId, kycVerified);
        return mapToResponseDTO(userRepository.save(user));
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setSuspended(user.isSuspended());
        dto.setSuspensionReason(user.getSuspensionReason());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setUpdatedBy(user.getUpdatedBy());
        return dto;
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        UserProfileResponse res = new UserProfileResponse();
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setAddress(user.getAddress());
        res.setNicNumber(user.getNicNumber());
        res.setProfilePictureUrl(user.getProfilePictureUrl());
        res.setKycVerified(user.isKycVerified());
        return res;
    }
}
