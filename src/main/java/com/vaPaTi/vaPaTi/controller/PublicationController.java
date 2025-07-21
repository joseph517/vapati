package com.vaPaTi.vaPaTi.controller;

import com.vaPaTi.vaPaTi.dtos.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.service.PublicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publications")
@Tag(name = "Publication", description = "Publication API")
public class PublicationController {

    private final PublicationService publicationService;

    public PublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new publication", description = "Create a new publication for a user")
    public ResponseEntity<PublicationResponseDTO> createPublication(@RequestBody CreatePublicationDTO dto) {
        PublicationResponseDTO created = publicationService.createPublication(dto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List publications by user", description = "Get all publications by a specific user ID")
    public ResponseEntity<List<PublicationResponseDTO>> getPublicationsByUser(@PathVariable Long userId) {
        List<PublicationResponseDTO> publications = publicationService.getPublicationsByUserId(userId);
        return ResponseEntity.ok(publications);
    }

    @DeleteMapping("/delete/{publicationId}/user/{userId}")
    @Operation(summary = "Delete a publication", description = "Delete a publication by ID if it belongs to the user")
    public ResponseEntity<String> deletePublication(
            @PathVariable Long publicationId,
            @PathVariable Long userId
    ) {
        publicationService.deletePublication(publicationId, userId);
        return ResponseEntity.ok("Deleted");
    }
}

