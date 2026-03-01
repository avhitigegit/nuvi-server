package com.nuvi.online_renting.users.controller;

import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.users.dto.UserProfileRequest;
import com.nuvi.online_renting.users.dto.UserProfileResponse;
import com.nuvi.online_renting.users.dto.UserRequestDTO;
import com.nuvi.online_renting.users.dto.UserResponseDTO;
import com.nuvi.online_renting.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Admin-only: directly creating a user record
    @PostMapping
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = userService.createUser(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "User created", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_PROFILE', 'VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        UserResponseDTO response = userService.getUserById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", users));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_PROFILE', 'VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable Long id,
                                                                   @Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = userService.updateUser(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted", null));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('VIEW_OWN_PROFILE')")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('UPDATE_OWN_PROFILE')")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileRequest userProfileRequest) {
        return ResponseEntity.ok(userService.updateMyProfile(userProfileRequest));
    }

    @PatchMapping("/me/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVATE_OWN_ACCOUNT')")
    public ResponseEntity<Void> deactivateMyAccount() {
        userService.deactivateMyAccount();
        return ResponseEntity.noContent().build();
    }
}
