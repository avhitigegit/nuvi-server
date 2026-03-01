package com.nuvi.online_renting.users.controller;

import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.users.dto.UserProfileRequest;
import com.nuvi.online_renting.users.dto.UserProfileResponse;
import com.nuvi.online_renting.users.dto.UserRequestDTO;
import com.nuvi.online_renting.users.dto.UserResponseDTO;
import com.nuvi.online_renting.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User created", userService.createUser(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_PROFILE', 'VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched", userService.getUserById(id)));
    }

    // GET /api/users?name=alice&role=USER&enabled=true&page=0&size=10&sort=name,asc
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDTO>>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Users fetched",
                userService.getAllUsers(name, role, enabled, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_PROFILE', 'VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable Long id,
                                                                   @Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", userService.updateUser(id, dto)));
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
    public ResponseEntity<UserProfileResponse> updateMyProfile(@Valid @RequestBody UserProfileRequest req) {
        return ResponseEntity.ok(userService.updateMyProfile(req));
    }

    @PatchMapping("/me/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVATE_OWN_ACCOUNT')")
    public ResponseEntity<Void> deactivateMyAccount() {
        userService.deactivateMyAccount();
        return ResponseEntity.noContent().build();
    }
}
