package com.nuvi.online_renting.users.controller;

import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.users.dto.UserProfileRequest;
import com.nuvi.online_renting.users.dto.UserProfileResponse;
import com.nuvi.online_renting.users.dto.UserRequestDTO;
import com.nuvi.online_renting.users.dto.UserResponseDTO;
import com.nuvi.online_renting.users.service.UserService;
import com.nuvi.online_renting.users.serviceImpl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = userService.createUser(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "User created", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        UserResponseDTO response = userService.getUserById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched", users));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = userService.updateUser(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted", null));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileRequest userProfileRequest) {
        return ResponseEntity.ok(userService.updateMyProfile(userProfileRequest));
    }

    @PatchMapping("/me/deactivate")
    @PreAuthorize("hasAnyRole('USER','SELLER','ADMIN')")
    public ResponseEntity<Void> deactivateMyAccount() {
        userService.deactivateMyAccount();
        return ResponseEntity.noContent().build();
    }
}
