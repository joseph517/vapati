package com.vaPaTi.vaPaTi.controller.comment;

import com.vaPaTi.vaPaTi.dtos.comment.CommentResponseDTO;
import com.vaPaTi.vaPaTi.dtos.comment.CreateCommentDTO;
import com.vaPaTi.vaPaTi.service.comment.CommentService;
import com.vaPaTi.vaPaTi.validation.common.SortValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "Comment API")
public class CommentController {

    private final CommentService commentService;
    private final SortValidationService sortValidationService;

    @PostMapping("/publications/{publicationId}/comments")
    @Operation(summary = "Comment on a publication",
            description = "Add a comment to the publication {publicationId} as the authenticated user. Requires a token. "
                    + "Anyone who can see the publication can comment on it, including its author and an admin, "
                    + "also on a visible CLOSED campaign. The text is trimmed and has up to 280 characters")
    public ResponseEntity<CommentResponseDTO> createComment(@PathVariable Long publicationId,
                                                            @Valid @RequestBody CreateCommentDTO dto) {
        return ResponseEntity.ok(commentService.createComment(publicationId, dto));
    }

    @GetMapping("/publications/{publicationId}/comments")
    @Operation(summary = "List the comments of a publication",
            description = "Public, paginated. Get the comments of the publication {publicationId}, with a fixed order "
                    + "by creation date ascending. Comments whose authors are sanctioned are hidden, except for an admin")
    public ResponseEntity<Page<CommentResponseDTO>> getComments(@PathVariable Long publicationId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = sortValidationService.validateAndGetPageable(page, size);
        return ResponseEntity.ok(commentService.getCommentsByPublicationId(publicationId, pageable));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment",
            description = "Delete the comment {commentId}. Requires a token. It can be deleted by the author of the comment, "
                    + "the author of the publication or an admin. A hidden comment answers 404")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of(
                "message", "Comment deleted successfully",
                "commentId", commentId
        ));
    }
}
