package com.vaPaTi.vaPaTi.controller.publication;

import com.vaPaTi.vaPaTi.dtos.publication.PublicationLikeResponseDTO;
import com.vaPaTi.vaPaTi.service.publication.PublicationLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publications/{publicationId}/like")
@Tag(name = "Publication", description = "Publication API")
@RequiredArgsConstructor
public class PublicationLikeController {

    private final PublicationLikeService publicationLikeService;

    @PostMapping
    @Operation(summary = "Like a publication", description = "Like the publication {publicationId} as the authenticated user. Requires a token. Idempotent: liking again answers 200 with the same state. Anyone who can see the publication can like it, including its author and an admin. A hidden publication answers 404")
    public ResponseEntity<PublicationLikeResponseDTO> likePublication(@PathVariable Long publicationId) {
        return ResponseEntity.ok(publicationLikeService.likePublication(publicationId));
    }

    @DeleteMapping
    @Operation(summary = "Unlike a publication", description = "Remove the like of the authenticated user from the publication {publicationId}. Requires a token. Idempotent: unliking without a like answers 200 with the same state. Anyone who can see the publication can unlike it, including its author and an admin. A hidden publication answers 404")
    public ResponseEntity<PublicationLikeResponseDTO> unlikePublication(@PathVariable Long publicationId) {
        return ResponseEntity.ok(publicationLikeService.unlikePublication(publicationId));
    }
}
