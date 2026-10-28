package com.vaPaTi.vaPaTi.controller.user;

import com.vaPaTi.vaPaTi.dtos.user.AutoThanksResponseDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateAutoThanksDTO;
import com.vaPaTi.vaPaTi.dtos.user.UpdateUserDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserDTO;
import com.vaPaTi.vaPaTi.dtos.user.UserUserInfoRequestDTO;
import com.vaPaTi.vaPaTi.service.user.UserService;
import com.vaPaTi.vaPaTi.validation.common.SortValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User API")
public class UserController {

    // Only User columns: the paginated query uses SELECT DISTINCT, and SQL Server rejects ORDER BY on joined columns
    private static final List<String> USER_SORT_FIELDS = List.of("id", "createdAt");

    private final UserService userService;
    private final SortValidationService sortValidationService;

    public UserController(
            UserService userService,
            SortValidationService sortValidationService
    ) {
        this.userService = userService;
        this.sortValidationService = sortValidationService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    @Operation(summary = "Get all users", description = "Get all users")
    public ResponseEntity<List<UserDTO>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @PostMapping("/create")
    @Operation(summary = "Create user", description = "Create a new user. profilePicture, if present, must be an http or https URL; blank stores null")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserUserInfoRequestDTO dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @PutMapping("/update")
    @Operation(summary = "Update user",
            description = "Updates the authenticated user. Changing the password, or the email to a different one, requires currentPassword "
                    + "(400 if missing or incorrect) and invalidates every session, including the current one: the user has to log in again")
    public ResponseEntity<UserDTO> updateUser(@Valid @RequestBody UpdateUserDTO dto) {
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
    @Operation(summary = "Get user by ID",
            description = "The user themselves and any ADMIN get the full UserDTO (userInfo with email and phone, verified, bankAccounts). "
                    + "Anyone else gets a PublicUserProfileDTO: id, categories, firstName, lastName, userName, description and profilePicture")
    public ResponseEntity<Object> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/me/auto-thanks")
    @Operation(summary = "Get my auto thanks message",
            description = "Default thanks message the authenticated user's donors receive right after donating. null if not set")
    public ResponseEntity<AutoThanksResponseDTO> getAutoThanksMessage() {
        return ResponseEntity.ok(userService.getAutoThanksMessage());
    }

    @PutMapping("/me/auto-thanks")
    @Operation(summary = "Set my auto thanks message",
            description = "Stored trimmed, applies to all of the user's campaigns. Blank (400) or over 280 characters (400) is rejected")
    public ResponseEntity<AutoThanksResponseDTO> updateAutoThanksMessage(@Valid @RequestBody UpdateAutoThanksDTO dto) {
        return ResponseEntity.ok(userService.updateAutoThanksMessage(dto));
    }

    @DeleteMapping("/me/auto-thanks")
    @Operation(summary = "Delete my auto thanks message", description = "Removes the default thanks message (it becomes null)")
    public ResponseEntity<AutoThanksResponseDTO> deleteAutoThanksMessage() {
        return ResponseEntity.ok(userService.deleteAutoThanksMessage());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list/paginated")
    @Operation(summary = "Get users with pagination", description = "Get users with pagination")
    public ResponseEntity<Page<UserDTO>> listUsersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Pageable pageable = sortValidationService.validateAndGetPageable(
                page, size, sortBy, sortDirection, USER_SORT_FIELDS);
        return ResponseEntity.ok(userService.listUsersWithPagination(pageable));
    }

}
