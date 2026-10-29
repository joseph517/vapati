package com.vaPaTi.vaPaTi.controller.userblock;

import com.vaPaTi.vaPaTi.dtos.userblock.BlockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UnblockResponseDTO;
import com.vaPaTi.vaPaTi.dtos.userblock.UserBlockResponseDTO;
import com.vaPaTi.vaPaTi.service.userblock.UserBlockService;
import com.vaPaTi.vaPaTi.validation.common.SortValidationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserBlockController {

    private static final List<String> BLOCK_SORT_FIELDS = List.of("createdAt");

    private final UserBlockService userBlockService;
    private final SortValidationService sortValidationService;

    public UserBlockController(
            UserBlockService userBlockService,
            SortValidationService sortValidationService
    ) {
        this.userBlockService = userBlockService;
        this.sortValidationService = sortValidationService;
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Block user",
            description = "Current user (from the token) blocks the user {userId}. Deletes the follows between the two users in both directions. "
                    + "While the block exists, neither can follow the other nor comment on the other's publications")
    public ResponseEntity<BlockResponseDTO> blockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userBlockService.blockUser(userId));
    }

    @DeleteMapping("/{userId}/block")
    @Operation(summary = "Unblock user",
            description = "Current user (from the token) removes their block on the user {userId}. Does not restore the follows that the block deleted")
    public ResponseEntity<UnblockResponseDTO> unblockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userBlockService.unblockUser(userId));
    }

    @GetMapping("/{userId}/block")
    @Operation(summary = "Check if blocking",
            description = "Returns true if the current user (from the token) blocks the user {userId}")
    public ResponseEntity<Boolean> isBlocking(@PathVariable Long userId) {
        return ResponseEntity.ok(userBlockService.isBlocking(userId));
    }

    @GetMapping("/me/blocks")
    @Operation(summary = "Get my blocks",
            description = "Paginated list of the users the current user blocks, newest block first by default (sortBy=createdAt, "
                    + "sortDirection=desc, ties broken by id descending). Leaves out blocked users who are deleted, banned or currently suspended")
    public ResponseEntity<Page<UserBlockResponseDTO>> getMyBlocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Pageable pageable = sortValidationService.validateAndGetPageable(
                page, size, sortBy, sortDirection, BLOCK_SORT_FIELDS);
        return ResponseEntity.ok(userBlockService.getMyBlocks(pageable));
    }
}
