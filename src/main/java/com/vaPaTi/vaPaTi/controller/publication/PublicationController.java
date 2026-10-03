package com.vaPaTi.vaPaTi.controller.publication;

import com.vaPaTi.vaPaTi.dtos.publication.CreatePublicationDTO;
import com.vaPaTi.vaPaTi.dtos.publication.PublicationResponseDTO;
import com.vaPaTi.vaPaTi.service.publication.PublicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Publication", description = "Publication API")
public class PublicationController {

    private final PublicationService publicationService;

    public PublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping("/campaigns/{campaignId}/publications")
    @Operation(summary = "Publish in a campaign", description = "Create a publication in the campaign {campaignId}. Only its owner can publish, while the campaign is ACTIVE or COMPLETED")
    public ResponseEntity<PublicationResponseDTO> createPublication(@PathVariable Long campaignId,
                                                                    @Valid @RequestBody CreatePublicationDTO dto) {
        PublicationResponseDTO created = publicationService.createPublication(campaignId, dto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/campaigns/{campaignId}/publications")
    @Operation(summary = "List publications by campaign", description = "Get the publications of the campaign {campaignId}, newest first. Public; a CLOSED campaign is only visible to its owner or an admin")
    public ResponseEntity<List<PublicationResponseDTO>> getPublicationsByCampaign(@PathVariable Long campaignId) {
        List<PublicationResponseDTO> publications = publicationService.getPublicationsByCampaignId(campaignId);
        return ResponseEntity.ok(publications);
    }

    @DeleteMapping("/publications/{publicationId}")
    @Operation(summary = "Delete a publication", description = "Delete a publication by ID if it belongs to the authenticated user")
    public ResponseEntity<Map<String, Object>> deletePublication(@PathVariable Long publicationId) {
        publicationService.deletePublication(publicationId);
        return ResponseEntity.ok(Map.of(
                "message", "Publication deleted successfully",
                "publicationId", publicationId
        ));
    }
}

