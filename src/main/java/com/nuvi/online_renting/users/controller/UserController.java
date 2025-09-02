package com.nuvi.online_renting.users.controller;

import com.nuvi.online_renting.common.dto.ApiResponse;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/com/nuvi/online_renting/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User retrieved", user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "All users retrieved", userService.getAllUsers())
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "User created", userService.createUser(user))
        );
    }
}
