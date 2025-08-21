package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.UpdateUserDTO;
import com.vaPaTi.vaPaTi.dtos.UserDTO;
import com.vaPaTi.vaPaTi.dtos.UserUserInfoRequestDTO;
import com.vaPaTi.vaPaTi.service.UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User API")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    @Operation(summary = "Get all users", description = "Get all users")
    public ResponseEntity<List<UserDTO>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @PostMapping("/create")
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserUserInfoRequestDTO dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @PutMapping("/update")
    @Operation(summary = "Update user", description = "Update user by ID")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UpdateUserDTO dto) {
        return ResponseEntity.ok(userService.updateUser(dto));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete user", description = "Delete user by token user")
    public ResponseEntity<Map<String, String>> deleteUser() {
        userService.deleteUser();
        return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully",
                "success", "true"
        ));
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get user by ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getUserById(id);
        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list/paginated")
    @Operation(summary = "Get users with pagination", description = "Get users with pagination")
    public ResponseEntity<Page<UserDTO>> listUsersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") @NotNull String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.listUsersWithPagination(pageable));
    }

}
