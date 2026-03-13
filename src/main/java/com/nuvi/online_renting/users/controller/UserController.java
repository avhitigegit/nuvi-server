package com.nuvi.online_renting.users.controller;

import com.nuvi.online_renting.common.audit.Auditable;
import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.users.dto.UserProfileRequest;
import com.nuvi.online_renting.users.dto.UserProfileResponse;
import com.nuvi.online_renting.users.dto.UserRequestDTO;
import com.nuvi.online_renting.users.dto.UserResponseDTO;
import com.nuvi.online_renting.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Manage user accounts and profiles. ADMINs can view and manage all users. Regular users can view and update their own profile or deactivate their own account.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a user account (Admin)", description = "Admin creates a new user account with a specified role. For self-registration, use POST /api/v1/auth/register instead.")
    @PostMapping
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User created", userService.createUser(dto)));
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a user's account details by their ID. Users can only view their own profile; ADMINs can view any user.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_PROFILE', 'VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched", userService.getUserById(id)));
    }

    @Operation(
            summary = "List and filter all users (Admin)",
            description = "Returns a paginated list of all users. Filter by name (partial match), role (USER / SELLER / ADMIN), and account status (enabled). " +
                          "Example: GET /api/v1/users?name=alice&role=USER&enabled=true&page=0&size=10&sort=name,asc"
    )
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

    @Operation(summary = "Update a user account (Admin)", description = "Admin updates any user's account details including their role. To update your own profile as a regular user, use PUT /api/v1/users/me instead.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UPDATE_OWN_PROFILE', 'VIEW_ALL_USERS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable Long id,
                                                                   @Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated", userService.updateUser(id, dto)));
    }

    @Operation(summary = "Delete a user account (Admin)", description = "Permanently deletes a user account. This is a hard delete and cannot be undone. Admin only.")
    @Auditable(action = "USER_DELETED", resourceType = "User")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted", null));
    }

    @Operation(summary = "Get my profile", description = "Returns the full profile of the currently logged-in user including their name, email, phone, and role.")
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('VIEW_OWN_PROFILE')")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @Operation(summary = "Update my profile", description = "Allows the currently logged-in user to update their own name and phone number. Email and role cannot be changed here.")
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('UPDATE_OWN_PROFILE')")
    public ResponseEntity<UserProfileResponse> updateMyProfile(@Valid @RequestBody UserProfileRequest req) {
        return ResponseEntity.ok(userService.updateMyProfile(req));
    }

    @Operation(summary = "Deactivate my account", description = "The currently logged-in user can deactivate their own account. A deactivated account cannot log in. Contact an admin to reactivate.")
    @PatchMapping("/me/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVATE_OWN_ACCOUNT')")
    public ResponseEntity<Void> deactivateMyAccount() {
        userService.deactivateMyAccount();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete my account (Right to erasure)",
            description = "Permanently erases all personally identifiable information from this account " +
                          "(name, email, phone, address, NIC) as required by PDPA/GDPR. " +
                          "The account row is retained so linked bookings and reviews remain consistent, " +
                          "but no personal data will be readable. All active sessions are also revoked. " +
                          "This action cannot be undone."
    )
    @DeleteMapping("/me")
    @PreAuthorize("hasAuthority('DEACTIVATE_OWN_ACCOUNT')")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount() {
        userService.deleteMyAccount();
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Your account and all personal data have been permanently erased.", null));
    }

    @Operation(
            summary = "Update KYC verification status (Admin)",
            description = "Admin manually sets or revokes KYC verification for a user. " +
                          "KYC is automatically set to true when a seller application is approved, " +
                          "and false when rejected. Use this endpoint for manual overrides only. " +
                          "Example: PATCH /api/v1/users/5/kyc?verified=true"
    )
    @Auditable(action = "USER_KYC_UPDATED", resourceType = "User")
    @PatchMapping("/{id}/kyc")
    @PreAuthorize("hasAuthority('FULL_ACCESS')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateKycStatus(@PathVariable Long id,
                                                                         @RequestParam boolean verified) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                "KYC status updated to " + verified, userService.updateKycStatus(id, verified)));
    }
}
